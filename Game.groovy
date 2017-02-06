
class Game {
    enum Role { LIBERAL, FASCIST, HITLER }

    String channel
    Map roles
    List players
    int numPlayers
    List drawPile
    List discardPile
    int currentPresident
    int libEnacted
    int facEnacted


    def createGame() {
        drawPile = []
        discardPile = []
        // Fill draw pile
        1.upto(6, {
            drawPile << new Policy(type: Policy.Type.LIBERAL)
        })
        1.upto(11, {
            drawPile << new Policy(type: Policy.Type.FASCIST)
        })
        Collections.shuffle(drawPile)

    }

    def startGame(names) {
        if (names.size() < 5) {
            groupMessage("Not enough players")
            return false
        }
        if (names.size() > 10) {
            groupMessage("Too many players")
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
                messagePlayer(player, "You are a ${roles.get(player)}")
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
                messagePlayer(player, "You are a ${roles[player]}, the other fascist(s) is/are $others, Hitler is $hitler")
            } else if (roles[player] == Role.HITLER) {
                if (players.size() < 7) {
                    def otherFac
                    players.each { other ->
                        if (other != player && roles[other] == Role.FASCIST) {
                            otherFac = other
                        }
                    }
                    messagePlayer(player, "You are HITLER, the other fascist is $otherFac")
                } else {
                    messagePlayer(player, "You are HITLER")
                }
            }
        }

        beginPlay()

        return true
    }

    def beginPlay() {
        messageGroup("About to begin, here is the turn order: $players")
        while (true) {
            playRound()
            messageGroup("Score: Liberals $libEnacted, Fascists $facEnacted")
            if (libEnacted == 5) {
                messageGroup "Liberals win by enacting 5 liberal policies"
                return
            } else if (facEnacted == 6) {
                messageGroup "Fascists win by enacting 6 fascist policies"
                return
            }
        }
    }

    def playRound() {
        def president = players[currentPresident]
        messageGroup("Waiting for president $president to nominate a chancellor")
        def chancellor = questionPlayer(president, "Who is your nominee for chancellor?")
        while (!players.contains(chancellor) || president == chancellor) {
            messagePlayer(president,  "Chancellor $chancellor doesn't exist (or you picked yourself), try again")
            chancellor = questionPlayer(president, "Who is your nominee for chancellor?")
        }
        messageGroup("President $president, nominates Chancellor $chancellor.")
        if (electGovernment(president, chancellor)) {
            messageGroup("The election passes")
            def policies = drawPolicies()
            def discard = questionPlayer(president, "Choose a policy to discard from $policies [1,2,3]") as int
            discardPolicy(policies.removeAt(discard - 1))
            discard = questionPlayer(chancellor, "Choose a policy to discard from $policies [1,2]") as int
            discardPolicy(policies.removeAt(discard - 1))
            enactPolicy(president, chancellor, policies[0])
        } else {
            messageGroup("The election fails")
            // Need to advance failed election marker
        }
        currentPresident++
        if (currentPresident >= players.size()) {
            currentPresident = 0
        }
    }

    def electGovernment(president, chancellor) {
        return 1
        //def elected = 0
        //messageGroup("Let's vote on the government of President $president, and Chancellor $chancellor")
        //def votingRecord = [:]
        //players.each { player ->
        //    def response = questionPlayer(player, "How do you feel about the proposed government? [Ja, Nein]? ")
        //    response = response.toLowerCase()
        //    if (response == "j" || response == "ja" || response == "y" || response == "yes") {
        //        elected++
        //        votingRecord << [(player): "Ja"]
        //    } else {
        //        elected--
        //        votingRecord << [(player): "Nein"]
        //    }
        //}
        //messageGroup("The results are: $votingRecord")
        //return elected > 0
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
        if (policy.type == Policy.Type.LIBERAL) {
            libEnacted++
        } else if (policy.type == Policy.Type.FASCIST) {
            facEnacted++
            specialAction(president)
        }
    }

    def reshuffle() {
        drawPile.addAll(discardPile)
        discardPile.clear()
        Collections.shuffle(drawPile)
    }

    def specialAction(president) {
        if (facEnacted == 4 || facEnacted == 5) {
            execute(president)
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
                    // Special election
                }
                break
            case 9:
            case 10:
                if (facEnacted == 1 || facEnacted == 2) {
                    inspect(president)
                } else if (facEnacted == 3) {
                    // Special election
                }
                break
                break
        }
    }

    def peek(president) {
        // President to peek
        def size = drawPile.size()
        // Pop, pops from the back so...
        def next = [drawPile[size - 1], drawPile[size - 2], drawPile[size -3]]
        messagePlayer(president, "The next three policies in the draw pile are: $next")
    }

    def inspect(president) {
        def response = questionPlayer(president, "Whom do you wish to inspect?")
        while (!players.contains(response)) {
            response = questionPlayer(president, "$response is not a recognized user. Choose a player to inspect. ")
        }
        def role = roles.get(response)
        if (role == Role.LIBERAL) {
            messagePlayer(president, "$response is a liberal")
        } else {
            messagePlayer(president, "$response is a fascist")
        }
    }
    
    def execute(president) {
        // President to choose a player to execute
        def response = questionPlayer(president, "Choose a player to execute. ")
        while (!players.contains(response)) {
            response = questionPlayer(president, "$response is not a recognized user. Choose a player to execute. ")
        }
        players.remove(response)
        messageGroup("$response is dead")
        currentPresident = players.indexOf(president)
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
}
