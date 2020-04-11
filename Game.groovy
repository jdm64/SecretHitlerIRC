// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.Colors
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class Game {
    def debug = Boolean.getBoolean("debug")

    Map roles
    List players
    int numPlayers
    List drawPile
    List discardPile
    List lastElected
    List cnhList
    int currentPresident
    int libEnacted
    int facEnacted
    int failedElection
    Events events

    def createGame() {
        currentPresident = 0
        libEnacted = 0
        facEnacted = 0
        failedElection = 0
        drawPile = []
        discardPile = []
        lastElected = []
        cnhList = []
        events = new Events()
        // Fill draw pile
        1.upto(6, {
            drawPile << Policy.LIBERAL
        })
        1.upto(11, {
            drawPile << Policy.FASCIST
        })
        Collections.shuffle(drawPile)
    }

    def startGame(names) {
        if (names.size() < 5) {
            messageGroup("Not enough players to start, need at least 5.")
            return false
        }
        if (names.size() > 10) {
            messageGroup("Too many players to start, maximum is 10.")
            return false
        }

        // Assign roles to players
        numPlayers = names.size()
        players = new ArrayList(names)
        Collections.shuffle(names)
        roles = [:]
        roles << [(names.pop()): Role.HITLER]
        switch (names.size()) {
            case 8:
            case 9:
                roles << [(names.pop()): Role.FASCIST]
                /* fall through */
            case 6:
            case 7:
                roles << [(names.pop()): Role.FASCIST]
                /* fall through */
            case 4:
            case 5:
                roles << [(names.pop()): Role.FASCIST]
                break;
        }
        names.each {
            roles << [(it): Role.LIBERAL]
        }

        Collections.shuffle(players)
        players.eachWithIndex { player, index ->
            if (roles[player] == Role.LIBERAL) {
                messagePlayer(player, "You are a ${roles[player]}")
            } else if (roles[player] == Role.FASCIST) {
                def others = []
                def hitler
                players.each { other ->
                    if (other != player && roles[other] == Role.FASCIST) {
                        others << other
                    } else if (roles[other] == Role.HITLER) {
                        hitler = other
                    }
                }
                if (others.isEmpty()) {
                    messagePlayer(player, "You are a ${roles[player]}, Hitler is ${hitler}")
                } else {
                    messagePlayer(player, "You are a ${roles[player]}, the other fascist(s) is/are ${others}, Hitler is ${hitler}")
                }
            } else if (roles[player] == Role.HITLER) {
                if (players.size() < 7) {
                    def otherFac
                    players.each { other ->
                        if (other != player && roles[other] == Role.FASCIST) {
                            otherFac = other
                        }
                    }
                    messagePlayer(player, "You are ${roles[player]}, the other fascist is ${otherFac}")
                } else {
                    messagePlayer(player, "You are ${roles[player]}")
                }
            }
        }

        beginPlay()

        return true
    }

    def endGame() {
        messageGroup(roles)
    }

    def printPlayers() {
        def message = "Players ["
        players.eachWithIndex { player, index ->
            if (index == currentPresident) {
                message += Colors.BOLD + player + Colors.NORMAL
            } else if (lastElected.contains(player) && cnhList.contains(player)) {
                message += "*+$player+*"
            } else if (lastElected.contains(player)) {
                message += "*$player*"
            }  else if (cnhList.contains(player)) {
                message += "+$player+"
            } else {
                message += player
            }
            if (index < players.size()-1) {
                message += ", "
            }
        }
        message += "] (* = incumbent, + = cnh)"
        return message
    }

    def beginPlay() {
        messageGroup("Let's start")
        while (true) {
            if (playRound()) {
                return
            } else {
                printEvents()
            }
            if (libEnacted == 5) {
                messageGroup "Liberals win by enacting 5 liberal policies"
                endGame()
                return
            } else if (facEnacted == 6) {
                messageGroup "Fascists win by enacting 6 fascist policies"
                endGame()
                return
            }
        }
    }

    // Return true if the game is over
    def playRound() {
        messageGroup(printPlayers())
        def president = players[currentPresident]
        if (presidentStart(president)) {
            roundEnd()
            return true
        }
        currentPresident++
        if (currentPresident >= players.size()) {
            currentPresident = 0
        }
        roundEnd()
        return false
    }

    def roundEnd() {}

    // Checks if the given string value is a number, and in range.
    def isNumberInRange(value, min, max) {
        if (value.isNumber()) {
            def num = value as int
            if (num >= min && num <= max) {
                return true
            }
        }
        return false
    }

    def nominateChancellor(president) {
        messagePlayer(president, printPlayers())
        while (true) {
            def chancellor = questionPlayer(president, "Who is your nominee for chancellor?")
            if (!players.contains(chancellor)) {
                messagePlayer(president, "Who? I don't know ${chancellor}.")
            } else if (president == chancellor) {
                messagePlayer(president, "This isn't a monarchy.")
            } else if (lastElected.contains(chancellor)) {
                messagePlayer(president, "${chancellor} is incumbent.")
            } else {
                return chancellor
            }
        }
    }

    def askPresidentDiscard(president, policies) {
        def response = questionPlayer(president, "Choose a policy to DISCARD from $policies [1,2,3]")
        while (!isNumberInRange(response, 1, 3)) {
            response = questionPlayer(president, "Please choose a number between 1 and 3")
        }
        return response
    }

    def askChancellorDiscard(chancellor, policies, vetoEnabled) {
        def question = "Choose a policy to DISCARD (the other will be enacted) from $policies [1,2]."
        if (vetoEnabled) {
            question += " Note: choose 0 to propose a veto."
        }
        def response = questionPlayer(chancellor, question)
        def min = vetoEnabled ? 0 : 1
        while (!isNumberInRange(response, min, 2)) {
            response = questionPlayer(chancellor, "Please choose a number between $min and 2")
        }
        return response
    }

    def presidentStart(president) {
        messageGroup("Waiting for president $president to nominate a chancellor")
        def chancellor = nominateChancellor(president)
        messageGroup("President $president, nominates Chancellor $chancellor.")

        def event = new Event()
        event.president = president
        event.chancellor = chancellor
        events.addEvent(event)

        if (electGovernment(president, chancellor)) {
            if (players.size() < 6) {
                lastElected = [chancellor]
            } else {
                lastElected = [president, chancellor]
            }
            failedElection = 0
            messageGroup("The election passes")
            if (facEnacted >= 3) {
                if (roles.get(chancellor) == Role.HITLER) {
                    messageGroup("The fascists win! Hitler has been elected chancellor.")
                    endGame()
                    return true
                } else {
                    cnhList << chancellor
                }
            }

            def policies = drawPolicies()
            def discard = askPresidentDiscard(president, policies) as int
            discardPolicy(policies.removeAt(discard - 1))
            discard = askChancellorDiscard(chancellor, policies, facEnacted == 5) as int
            if (discard == 0) {
                // Move to veto
                if (veto(president, chancellor)) {
                    event.result = "Veto"
                    return false
                }
                discard = askChancellorDiscard(chancellor, policies, false) as int
            }
            discardPolicy(policies.removeAt(discard - 1))

            event.result = "${policies[0]}"
            if (enactPolicy(president, chancellor, policies[0])) {
                return true
            }
        } else {
            event.result = "Failed"
            failedElection++
            if (failedElection < 3) {
                messageGroup("The election fails, the failed election marker is now at $failedElection")
            } else {
                topCard()
            }
        }
        return false
    }

    def electGovernment(president, chancellor) {
        if (Boolean.getBoolean("autoelect")) {
            return true
        }
        def threadPool
        if (debug) {
            threadPool = Executors.newFixedThreadPool(1)
        } else {
            threadPool = Executors.newFixedThreadPool(players.size())
        }

        messageGroup("Let's vote on the government of President $president, and Chancellor $chancellor")
        def votingRecord = new ConcurrentHashMap()
        def elected = new AtomicInteger()
        def futures = []
        try {
            players.each { player ->
                futures << threadPool.submit({
                    def response = questionPlayer(player, "Do you approve of a governemnt of $president and $chancellor ? [Ja, Nein]? ")
                    response = response.toLowerCase()
                    if (response == "j" || response == "ja" || response.startsWith("y")) {
                        elected.getAndIncrement()
                        votingRecord << [(player): true]
                    } else {
                        elected.getAndDecrement()
                        votingRecord << [(player): false]
                    }
                } as Callable)
            }
        } finally {
            threadPool.shutdown()
        }
        futures.each{it.get()}

        def ja = votingRecord.findAll{it.value}.collect{it.key}
        def nein = votingRecord.findAll{!it.value}.collect{it.key}
        messageGroup("The results are: Ja $ja; Nein $nein")
        return elected.get() > 0
    }

    def drawPolicies() {
        if (drawPile.size() < 3) {
            reshuffle()
        }
        return [drawPile.pop(), drawPile.pop(), drawPile.pop()]
    }

    def discardPolicy(policy) {
        discardPile << policy
    }

    // Returns true if the game is over
    def enactPolicy(president, chancellor, policy) {
        messageGroup("President $president and Chancellor $chancellor enacted a $policy policy.")
        messageGroup("Draw pile size: ${drawPile.size()}, Discard pile size: ${discardPile.size()}.")
        if (policy == Policy.LIBERAL) {
            libEnacted++
            messageGroup("Score: ${Policy.LIBERAL} $libEnacted/5; ${Policy.FASCIST} $facEnacted/6")
        } else if (policy == Policy.FASCIST) {
            facEnacted++
            messageGroup("Score: ${Policy.LIBERAL} $libEnacted/5; ${Policy.FASCIST} $facEnacted/6")
            if (specialAction(president)) {
                return true
            }
        }
        return false
    }

    def reshuffle() {
        messageGroup("Reshuffling the deck")
        drawPile.addAll(discardPile)
        discardPile.clear()
        Collections.shuffle(drawPile)
    }

    def veto(president, chancellor) {
        messageGroup("Chancellor $chancellor moves to veto this agenda.")
        def result = questionPlayer(president, "Do you agree to a veto of the current agenda? [Y/N]").toLowerCase()
        if (result == "y" || result == "yes") {
            messageGroup("President $president agrees to the veto, the policies are discarded and the failed government marker advances")
            failedElection++
            messageGroup("The failed election marker is now at $failedElection")
            if (failedElection == 3) {
                topCard()
            }
            return true
        } else {
            messageGroup("President $president disagrees to the veto, the chancellor must enact a policy.")
            return false
        }
    }

    def topCard() {
        def policy = drawPile.pop()
        messageGroup("The election fails, the next policy ($policy) will be automatically enacted.")
        messageGroup("Draw pile size: ${drawPile.size()}, Discard pile size: ${discardPile.size()}.")
        if (policy == Policy.LIBERAL) {
            libEnacted++
        } else if (policy == Policy.FASCIST) {
            facEnacted++
        }
        messageGroup("Score: Liberals $libEnacted, Fascists $facEnacted")
        failedElection = 0
        lastElected.clear()

        def event = new Event()
        events.addEvent(event)
        event.president = ""
        event.chancellor = ""
        event.result = "Top Card: $policy"
    }

    def specialAction(president) {
        if (facEnacted == 4 || facEnacted == 5) {
            if (execute(president)) {
                return true
            }
        }
        switch (numPlayers) {
            case 5:
            case 6:
                if (facEnacted == 3) {
                    peek(president)
                }
                break
            case 7:
            case 8:
                if (facEnacted == 2) {
                    inspect(president)
                } else if (facEnacted == 3) {
                    return specialElection(president)
                }
                break
            case 9:
            case 10:
                if (facEnacted == 1 || facEnacted == 2) {
                    inspect(president)
                } else if (facEnacted == 3) {
                    return specialElection(president)
                }
                break
        }
        return false
    }

    def peek(president) {
        if (drawPile.size() < 3) {
            reshuffle()
        }
        // President to peek at the top 3
        messageGroup("President $president is peeking at the next 3 policies on the draw pile.")
        def next = [drawPile[0], drawPile[1], drawPile[2]]
        messagePlayer(president, "The next three policies in the draw pile are: $next")
    }

    def inspect(president) {
        messageGroup("Waiting for President $president to decide whom to inspect.")
        def response = questionPlayer(president, "Whom do you wish to inspect?")
        while (!players.contains(response)) {
            response = questionPlayer(president, "$response is not a recognized user. Choose a player to inspect. ")
        }
        messageGroup("President $president to inspect the party membership of $response")
        def role = roles.get(response)
        if (role == Role.LIBERAL) {
            messagePlayer(president, "$response is a liberal")
        } else {
            messagePlayer(president, "$response is a fascist")
        }
    }

    def specialElection(president) {
        printEvents()
        messageGroup("Special Election. Waiting for President $president to nominate the next president.")
        def response = questionPlayer(president, "Whom do you wish to nominate as the next president?")
        while (!players.contains(response)) {
            response = questionPlayer(president, "$response is not a recognized user. Choose a player to inspect. ")
        }
        messageGroup("President $president nominates $response to be the next president.")
        def backupLastElected = lastElected
        lastElected = []
        def result = presidentStart(response)
        if (lastElected.isEmpty()) {
            lastElected = backupLastElected
        }
        return result
    }

    def execute(president) {
        messageGroup("President $president will now choose a player to execute.")
        // President to choose a player to execute
        def response = questionPlayer(president, "Choose a player to execute. ")
        while (response == president || !players.contains(response)) {
            response = questionPlayer(president, "$response is not a recognized user, or you tried to kill yourself. Choose a player to execute. ")
        }
        if (kill(response)) {
            return true
        }
        currentPresident = players.indexOf(president)
        return false
    }

    def kill(user) {
        players.remove(user)
        messageGroup("$user is dead")
        if (roles.get(user) == Role.HITLER) {
            messageGroup("Hitler has been executed. Liberals win!")
            endGame()
            return true
        }
        return false
    }

    def messageGroup(message) {
        println message
    }

    def messagePlayer(name, message) {
        println "$name: $message"
    }

    def questionPlayer(name, question) {
        messagePlayer(name, question)
        return System.console().readLine("What is your response? ")
    }

    def printEvents() {
        events.dumpEvents()
    }
}
