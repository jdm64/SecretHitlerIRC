
abstract class GameMaster {

    def abstract messageGroup(message)

    def abstract messagePlayer(name, message)

    def abstract questionPlayer(name, question)

    def tellSetup(roles, drawPile) {
        if (Config.rebalance) {
            messageGroup("Rebalanced rules enabled")
        }

        def numLib = drawPile.count(Policy.LIBERAL)
        def numFas = drawPile.count(Policy.FASCIST)
        messageGroup("Draw pile: $numLib $Policy.LIBERAL and $numFas $Policy.FASCIST")

        def vals = roles.values()
        numLib = vals.count(Role.LIBERAL)
        numFas = vals.count(Role.FASCIST)
        def numHit = vals.count(Role.HITLER)
        messageGroup("Players: $numLib $Role.LIBERAL, $numFas $Role.FASCIST and $numHit $Role.HITLER")
    }

    def tellRoles(roles, hitlerKnows) {
        roles.each { player, role ->
            def msg = "You are $role"
            if (role == Role.FASCIST) {
                msg += ", the other fascists are "
                msg += roles.findAll{ it.key != player && it.value == Role.FASCIST }.collect{ it.key }
                msg += ", Hitler is " + roles.find{ it.value == Role.HITLER }.collect{ it.key }
            } else if (role == Role.HITLER && hitlerKnows) {
                msg += ", the other fascist is " + roles.find{ it.value == Role.FASCIST }.collect{ it.key }
            }
            messagePlayer(player, msg)
        }
    }

    def tellPlayerOrder(players, currentPresident, lastElected, cnhList) {
        messageGroup("Players [ " + players.withIndex().collect { player, index ->
            def str = ""
            if (lastElected.contains(player)) {
                str += "*"
            }

            if (cnhList.contains(player)) {
                str += "+"
            }

            if (index == currentPresident) {
                str += "$ColorCode.BOLD$player$ColorCode.NORMAL"
            } else {
                str += player
            }
            return str
        }.join(", ") + " ] (* = incumbent, + = cnh)")
    }

    def askPlayerYesNo(player, question) {
        while (true) {
            def response = questionPlayer(player, question).toLowerCase()
            if (response.startsWith("j") || response.startsWith("y")) {
                return true
            } else if (response.startsWith("n")) {
                return false
            } else {
                messagePlayer(player, "What? I didn't catch that.")
            }
        }
    }

    def askPlayerName(players, askPlayer, question, invalidName) {
        while (true) {
            def name = questionPlayer(askPlayer, question).trim()
            if (!players.contains(name)) {
                messagePlayer(askPlayer, "Invalid player name: $name")
            } else if (askPlayer == name) {
                messagePlayer(askPlayer, "Cannot select yourself.")
            } else {
                def invalidMsg = invalidName(name)
                if (invalidMsg) {
                    messagePlayer(askPlayer, invalidMsg)
                } else {
                    return name
                }
            }
        }
    }

    def nominateChancellor(president, players, lastElected) {
        messageGroup("Waiting for president $president to nominate a chancellor")

        def valid = players - president - lastElected
        def chancellor = askPlayerName(players, president, "Who is your nominee for chancellor? $valid", { nominee ->
            return lastElected.contains(nominee) ? "${nominee} is incumbent." : null
        })

        messageGroup("President $president, nominates Chancellor $chancellor. Let's vote on this government.")

        return chancellor
    }

    def aproveGovernment(player, president, chancellor, voteCounter) {
        def response = askPlayerYesNo(player, "Do you approve of a government of $president and $chancellor? [Ja, Nein]")
        if (response) {
            voteCounter.getAndIncrement()
        } else {
            voteCounter.getAndDecrement()
        }
        return [(player): response]
    }

    def isNumberInRange(value, min, max) {
        if (value.isNumber()) {
            def num = value as int
            if (num >= min && num <= max) {
                return true
            }
        }
        return false
    }

    def askPresidentDiscard(president, policies) {
        def response = questionPlayer(president, "Choose a policy to DISCARD from $policies [1,2,3]")
        while (!isNumberInRange(response, 1, 3)) {
            response = questionPlayer(president, "Please choose a number between 1 and 3")
        }
        return response as int
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
        return response as int
    }

    def electionResults(ja, nein, failedBefore) {
        def isPass = ja.size() > nein.size()
        def result = isPass ? "passes" : "fails"
        messageGroup("The election $result: ${ja.size()}/${nein.size()} Ja$ja Nein$nein")
        if (!isPass) {
            def afterFailed = failedBefore + 1
            messageGroup("The failed election marker is now at $afterFailed")
        }
    }

    def peek(president, topThree) {
        messageGroup("President $president is peeking at the next 3 policies on the draw pile.")
        messagePlayer(president, "The next three policies in the draw pile are: $topThree")
    }

    def inspect(president, players, inspected, roles) {
        messageGroup("Waiting for President $president to decide whom to inspect.")

        def validPlayers = players - president - inspected
        def inspectPlayer = askPlayerName(players, president, "Whom do you wish to inspect? $validPlayers", { player ->
            return inspected.contains(player) ? "$player has already been inspect once" : null
        })

        def role = roles.get(inspectPlayer) == Role.LIBERAL ? Role.LIBERAL : Role.FASCIST
        messageGroup("President $president to inspect the party membership of $inspectPlayer")
        messagePlayer(president, "$inspectPlayer is a $role")
        return inspectPlayer
    }

    def askVeto(president, chancellor) {
        messageGroup("Chancellor $chancellor moves to veto this agenda.")
        def result = askPlayerYesNo(president, "Do you agree to a veto of the current agenda? [Ja, Nein]")
        if (result) {
            messageGroup("President $president agrees to the veto, the policies are discarded and the failed government marker advances")
        } else {
            messageGroup("President $president disagrees to the veto, the chancellor must enact a policy.")
        }
        return result
    }
}
