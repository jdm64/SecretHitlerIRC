
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.security.SecureRandom

class Game {

    Map<String,Role> roles
    List<String> players
    int numPlayers
    List<Policy> drawPile
    List<Policy> discardPile
    List<String> lastElected
    List<String> inspected
    List<String> cnhList
    String currentPresident
    int libEnacted
    int facEnacted
    int failedElection
    Events events
    GameMaster gm
    SecureRandom rand = new SecureRandom()
    ConcurrentSkipListSet toVote = new ConcurrentSkipListSet<String>()

    // to handle special elections
    boolean isSpecialElectStart
    Tuple specialElectState

    def Game(GameMaster gameMaster) {
        gm = gameMaster
    }

    def run() {
        gm.run(this)
    }

    def shuffle(List list) {
        Collections.shuffle(list, rand)
        Collections.shuffle(list, rand)
    }

    def startGame(Collection<String> names) {
        if (names.size() < 5) {
            gm.messageGroup("Not enough players to start, need at least 5.")
            return false
        } else if (names.size() > 10) {
            gm.messageGroup("Too many players to start, maximum is 10.")
            return false
        }


        clearState(names.size())
        setupDeck()
        assignRoles(names)

        gm.tellSetup(roles, drawPile)
        gm.tellRoles(roles, names.size() < 7)

        gm.tellVictory(gameLoop())
        gm.tellEndgameRoles(roles)

        return true
    }

    def clearState(int playerCount) {
        numPlayers = playerCount
        currentPresident = null
        libEnacted = 0
        facEnacted = 0
        failedElection = 0
        drawPile = []
        discardPile = []
        lastElected = []
        inspected = []
        cnhList = []
        isSpecialElectStart = false
        specialElectState = null
        events = new Events()
    }

    def setupDeck() {
        def numRed = 11
        if (Config.rebalance) {
            if (numPlayers == 7) {
                numRed = 10
            } else if (numPlayers == 9) {
                numRed = 9
            }
        }
        1.upto(numRed, {
            drawPile << Policy.FASCIST
        })
        if (Config.rebalance && numPlayers == 6) {
            drawPile.remove(0)
            facEnacted = 1

            def event = new Event()
            events.addEvent(event)
            event.president = " "
            event.chancellor = " "
            event.result = Policy.FASCIST
            event.votes = "Rebalance"
        }

        1.upto(6, {
            drawPile << Policy.LIBERAL
        })

        shuffle(drawPile)
    }

    def assignRoles(Collection<String> names) {
        def namesCopy = new ArrayList<String>(names)
        shuffle(namesCopy)
        players = new ArrayList<String>(namesCopy)
        shuffle(players)

        roles = [:]
        roles << [(namesCopy.pop()): Role.HITLER]
        switch (numPlayers) {
        case 10:
        case 9:
            roles << [(namesCopy.pop()): Role.FASCIST]
            /* fall through */
        case 8:
        case 7:
            roles << [(namesCopy.pop()): Role.FASCIST]
            /* fall through */
        case 6:
        case 5:
            roles << [(namesCopy.pop()): Role.FASCIST]
            break;
        }
        namesCopy.each {
            roles << [(it): Role.LIBERAL]
        }
    }

    GameResult gameLoop() {
        currentPresident = players[0]

        if (Config.rebalance && numPlayers == 6) {
            gm.messageGroup("Start Round 1")
            gm.tellPolicyResult(drawPile.size(), discardPile.size(), libEnacted, facEnacted)
            gm.printResultTable(events)
        }

        while (true) {
            gm.messageGroup("Start Round " + (events.events.size() + 1))
            gm.tellPlayerOrder(players, currentPresident, lastElected, cnhList)

            def result = playRound(currentPresident)
            if (result != GameResult.NONE) {
                return result
            }

            if (failedElection == 3) {
                topCard()
            }

            if (libEnacted == 5) {
                return GameResult.LIBERAL_WIN_COUNT
            } else if (facEnacted == 6) {
                return GameResult.FASCIST_WIN_COUNT
            }

            gm.printResultTable(events)
            setNextPresident()
        }
    }

    def setNextPresident() {
        if (isSpecialElectStart) {
            isSpecialElectStart = false
            return
        } else if (specialElectState) {
            currentPresident = specialElectState[0]
            if (lastElected.isEmpty()) {
                lastElected = specialElectState[1]
            }
            specialElectState = null
        }
        currentPresident = players[(players.indexOf(currentPresident) + 1) % players.size()]
    }

    GameResult playRound(String president) {
        def chancellor = gm.nominateChancellor(president, players, lastElected)

        def event = new Event()
        event.president = president
        event.chancellor = chancellor
        events.addEvent(event)

        if (electGovernment(event, president, chancellor)) {
            if (facEnacted >= 3) {
                if (roles.get(chancellor) == Role.HITLER) {
                    return GameResult.HITLER_ELECTED
                } else {
                    cnhList << chancellor
                }
            }

            if (players.size() < 6) {
                lastElected = [chancellor]
            } else {
                lastElected = [president, chancellor]
            }
            failedElection = 0

            def policies = drawPolicies()
            def discard = gm.askPresidentDiscard(president, policies)
            discardPolicy(policies.removeAt(discard - 1))
            discard = gm.askChancellorDiscard(chancellor, policies, facEnacted == 5)
            if (discard == 0) {
                // Move to veto
                if (veto(president, chancellor)) {
                    event.result = "Veto"
                    return GameResult.NONE
                }
                discard = gm.askChancellorDiscard(chancellor, policies, false)
            }
            discardPolicy(policies.removeAt(discard - 1))

            event.result = "${policies[0]}"
            return enactPolicy(president, chancellor, policies[0])
        } else {
            event.result = "Failed"
            failedElection++
        }
        return GameResult.NONE
    }

    def electGovernment(Event event, String president, String chancellor) {
        if (Config.autoelect) {
            event.votes = players.join(" ") + " \u2588"
            gm.electionResults(players, [], failedElection, "<NONE>")
            return true
        }

        def threadPool
        if (Config.debug) {
            threadPool = Executors.newFixedThreadPool(1)
        } else {
            threadPool = Executors.newFixedThreadPool(players.size())
        }

        def votingRecord = new ConcurrentHashMap<String,Boolean>()
        def elected = new AtomicInteger()
        def futures = []
        def lastVoter

        toVote.clear()
        toVote.addAll(players)

        try {
            players.each { player ->
                futures << threadPool.submit({
                    votingRecord << gm.aproveGovernment(player, president, chancellor, elected)
                    toVote.remove(player)
                    lastVoter = player
                } as Callable)
            }
        } finally {
            threadPool.shutdown()
        }
        futures.each{it.get()}

        def ja = []
        def nein = []
        players.each {
            if (votingRecord[it]) {
                ja << it
            } else {
                nein << it
            }
        }

        def voteLine = []
        voteLine.addAll(ja)
        voteLine.add("\u2588")
        voteLine.addAll(nein)

        event.votes = voteLine.join(" ")
        gm.electionResults(ja, nein, failedElection, lastVoter)

        return elected.get() > 0
    }

    List<Policy> drawPolicies() {
        return [drawPile.remove(0), drawPile.remove(0), drawPile.remove(0)]
    }

    def discardPolicy(Policy policy) {
        discardPile << policy
    }

    GameResult enactPolicy(String president, String chancellor, Policy policy) {
        if (policy == Policy.LIBERAL) {
            libEnacted++
        } else if (policy == Policy.FASCIST) {
            facEnacted++
        }

        reshuffle()

        gm.tellEnactPolicy(president, chancellor, policy)
        gm.tellPolicyResult(drawPile.size(), discardPile.size(), libEnacted, facEnacted)

        return policy == Policy.FASCIST ? specialAction(president) : GameResult.NONE
    }

    def veto(String president, String chancellor) {
        def result = gm.askVeto(president, chancellor)
        if (result) {
            failedElection++
            return true
        } else {
            return false
        }
    }

    def topCard() {
        reshuffle()

        def policy = drawPile.remove(0)
        if (policy == Policy.LIBERAL) {
            libEnacted++
        } else if (policy == Policy.FASCIST) {
            facEnacted++
        }

        gm.tellTopCard(policy)
        gm.tellPolicyResult(drawPile.size(), discardPile.size(), libEnacted, facEnacted)

        failedElection = 0
        lastElected.clear()

        def event = new Event()
        events.addEvent(event)
        event.president = " "
        event.chancellor = " "
        event.result = policy
        event.votes = "Top Card"
    }

    def reshuffle() {
        if (drawPile.size() >= 3) {
            return
        }
        gm.messageGroup("Reshuffling the deck")
        drawPile.addAll(discardPile)
        discardPile.clear()
        shuffle(drawPile)
    }

    GameResult specialAction(String president) {
        if (facEnacted == 4 || facEnacted == 5) {
            return execute(president)
        }

        switch (numPlayers) {
            case 5:
            case 6:
                if (facEnacted == 3) {
                    gm.peek(president, [drawPile[0], drawPile[1], drawPile[2]])
                }
                break
            case 7:
            case 8:
                if (facEnacted == 2) {
                    inspected << gm.inspect(president, players, inspected, roles)
                } else if (facEnacted == 3) {
                    specialElection(president)
                }
                break
            case 9:
            case 10:
                if (facEnacted == 1 || facEnacted == 2) {
                    inspected << gm.inspect(president, players, inspected, roles)
                } else if (facEnacted == 3) {
                    specialElection(president)
                }
                break
        }
        return GameResult.NONE
    }

    def specialElection(String president) {
        isSpecialElectStart = true
        specialElectState = new Tuple(president, lastElected)
        lastElected = []
        currentPresident = gm.askSpecialElection(president, players)
    }

    GameResult execute(String president) {
        def killPlayer = gm.askExecute(president, players)
        players.remove(killPlayer)
        return roles.get(killPlayer) == Role.HITLER ? GameResult.HITLER_KILLED : GameResult.NONE
    }
}
