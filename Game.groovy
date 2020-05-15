
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class Game {

    Map roles
    List players
    int numPlayers
    List drawPile
    List discardPile
    List lastElected
    List inspected
    List cnhList
    String currentPresident
    int libEnacted
    int facEnacted
    int failedElection
    Events events
    GameMaster gm

    // to handle special elections
    boolean isSpecialElectStart
    Tuple specialElectState

    def startGame(gameMaster, names) {
        gm = gameMaster
        if (names.size() < 5) {
            gm.messageGroup("Not enough players to start, need at least 5.")
            return false
        } else if (names.size() > 10) {
            gm.messageGroup("Too many players to start, maximum is 10.")
            return false
        }

        clearState()
        setupDeck()
        assignRoles(names)

        gm.tellSetup(roles, drawPile)
        gm.tellRoles(roles, names.size() < 7)

        gm.tellVictory(beginPlay())
        endGame()

        return true
    }

    def clearState() {
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
        }

        1.upto(6, {
            drawPile << Policy.LIBERAL
        })

        Collections.shuffle(drawPile)
        Collections.shuffle(drawPile) // double shuffle
    }

    def assignRoles(names) {
        numPlayers = names.size()
        def namesCopy = new ArrayList(names)
        Collections.shuffle(namesCopy)
        Collections.shuffle(namesCopy) // double shuffle
        players = new ArrayList(namesCopy)

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

    def beginPlay() {
        currentPresident = players[0]
        while (true) {
            gm.messageGroup("Start Round " + (events.events.size() + 1))

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

            printEvents()
            setNextPresident()
        }
    }

    def playRound(president) {
        gm.tellPlayerOrder(players, president, lastElected, cnhList)
        return presidentStart(president)
    }

    def printEvents() {
        gm.messageGroup(" ")
        events.toLines().each { gm.messageGroup(it) }
        gm.messageGroup(" ")
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

    def presidentStart(president) {
        def chancellor = gm.nominateChancellor(president, players, lastElected)

        def event = new Event()
        event.president = president
        event.chancellor = chancellor
        events.addEvent(event)

        if (electGovernment(event, president, chancellor)) {
            if (players.size() < 6) {
                lastElected = [chancellor]
            } else {
                lastElected = [president, chancellor]
            }
            failedElection = 0

            if (facEnacted >= 3) {
                if (roles.get(chancellor) == Role.HITLER) {
                    return GameResult.HITLER_ELECTED
                } else {
                    cnhList << chancellor
                }
            }

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

    def electGovernment(event, president, chancellor) {
        if (Config.autoelect) {
            event.votes = players.toString()[1..-2] + " || "
            gm.electionResults(players, [], failedElection)
            return true
        }

        def threadPool
        if (Config.debug) {
            threadPool = Executors.newFixedThreadPool(1)
        } else {
            threadPool = Executors.newFixedThreadPool(players.size())
        }

        def votingRecord = new ConcurrentHashMap()
        def elected = new AtomicInteger()
        def futures = []
        try {
            players.each { player ->
                futures << threadPool.submit({
                    votingRecord << gm.aproveGovernment(player, president, chancellor, elected)
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

        event.votes = ja.size() ? ja.toString()[1..-2] : ""
        event.votes += " || "
        event.votes += nein.size() ? nein.toString()[1..-2] : ""
        gm.electionResults(ja, nein, failedElection)

        return elected.get() > 0
    }

    def drawPolicies() {
        return [drawPile.remove(0), drawPile.remove(0), drawPile.remove(0)]
    }

    def discardPolicy(policy) {
        discardPile << policy
    }

    def enactPolicy(president, chancellor, policy) {
        if (policy == Policy.LIBERAL) {
            libEnacted++
        } else if (policy == Policy.FASCIST) {
            facEnacted++
        }

        gm.tellEnactPolicy(president, chancellor, policy)
        gm.tellPolicyResult(drawPile.size(), discardPile.size(), libEnacted, facEnacted)

        reshuffle()
        return policy == Policy.FASCIST ? specialAction(president) : GameResult.NONE
    }

    def veto(president, chancellor) {
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
        Collections.shuffle(drawPile)
        Collections.shuffle(drawPile) // double shuffle
    }

    def specialAction(president) {
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

    def specialElection(president) {
        isSpecialElectStart = true
        specialElectState = new Tuple(president, lastElected)
        lastElected = []
        currentPresident = gm.askSpecialElection(president, players)
    }

    def execute(president) {
        def killPlayer = gm.askExecute(president, players)
        players.remove(killPlayer)
        return roles.get(killPlayer) == Role.HITLER ? GameResult.HITLER_KILLED : GameResult.NONE
    }

    def endGame() {
        roles.each {
            gm.messageGroup("${it.key} was ${it.value}")
        }
    }
}
