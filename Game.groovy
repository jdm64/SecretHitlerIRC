
class Game {
    enum Role { LIBERAL, FASCIST, HITLER }

    String channel
    Map roles
    List players
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
        def chancellor = questionUser(president, "Who is your nominee fo chancellor?")
        while (!players.contains(chancellor) || president == chancellor) {
            messagePlayer(president,  "Chancellor $chancellor doesn't exist (or you picked yourself), try again")
            chancellor = questionUser(president, "Who is your nominee fo chancellor?")
        }
        if (electGovernment(president, chancellor)) {
            messageGroup("The election passes")
            def policies = drawPolicies()
            def discard = questionUser(president, "Choose a policy to discard from $policies [1,2,3]") as int
            discardPolicy(policies.removeAt(discard - 1))
            discard = questionUser(chancellor, "Choose a policy to discard from $policies [1,2]") as int
            discardPolicy(policies.removeAt(discard - 1))
            enactPolicy(policies[0])
        } else {
            messageGroup("The election fails")
        }
        currentPresident++
    }

    def electGovernment(president, chancellor) {
        return 1
        //def elected = 0
        //messageGroup("Let's vote on the government of President $president, and Chancellor $chancellor")
        //def votingRecord = [:]
        //players.each { player ->
        //    def response = questionUser(player, "How do you feel about the proposed government? [Ja, Nein]? ")
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
    def enactPolicy(policy) {
        messageGroup("A $policy policy was enacted")
        if (policy.type == Policy.Type.LIBERAL) {
            libEnacted++
        } else if (policy.type == Policy.Type.FASCIST) {
            facEnacted++
            specialAction()
        }
    }

    def reshuffle() {
        drawPile.addAll(discardPile)
        discardPile.clear()
        Collections.shuffle(drawPile)
    }

    def specialAction() {
        switch (players.size()) {
            case 5:
            case 6:
                break
            case 7:
            case 8:
                break
            case 9:
            case 10:
                break
        }

    }

    def messageGroup(message) {
        println message
    }

    def messagePlayer(name, message) {
        println "$name: $message"
    }

    def questionUser(name, question) {
        messagePlayer(name, question)
        return System.console().readLine("What is your response? ")
    }
}
