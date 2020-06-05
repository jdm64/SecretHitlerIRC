
import java.util.concurrent.atomic.*

abstract class GameMaster {

    def abstract run(Game game)

    def abstract messageGroup(String message)

    def abstract messagePlayer(String name, String message)

    abstract String questionPlayer(String name, String question)

    def tellSetup(Map<String,Role> roles, List<Policy> drawPile) {
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
        messageGroup(" ")
    }

    def tellRoles(Map<String,Role> roles, boolean hitlerKnows) {
        messageGroup("Handing out roles....")
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
        messageGroup("Let the game begin!")
        messageGroup(" ")
    }

    def tellPlayerOrder(List<String> players, String currentPresident, List<String> lastElected, List<String> cnhList) {
        messageGroup("Players [ " + players.collect { player ->
            def str = ""
            if (lastElected.contains(player)) {
                str += "*"
            }

            if (cnhList.contains(player)) {
                str += "+"
            }

            if (player == currentPresident) {
                str += "$ColorCode.BOLD$player$ColorCode.NORMAL"
            } else {
                str += player
            }
            return str
        }.join(", ") + " ] (* = incumbent, + = cnh)")
    }

    Boolean askPlayerYesNo(String player, String question) {
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

    def askPlayerName(List<String> players, String askPlayer, String question, Closure<String> invalidName) {
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

    String nominateChancellor(String president, List<String> players, List<String> lastElected) {
        messageGroup("Waiting for president $president to nominate a chancellor")

        def valid = players - president - lastElected
        def chancellor = askPlayerName(players, president, "Who is your nominee for chancellor? $valid", { nominee ->
            return lastElected.contains(nominee) ? "${nominee} is incumbent." : null
        })

        messageGroup("President $president, nominates Chancellor $chancellor. Let's vote on this government.")

        return chancellor
    }

    Map<String,Boolean> aproveGovernment(String player, String president, String chancellor, AtomicInteger voteCounter) {
        def response = askPlayerYesNo(player, "Do you approve of a government of $president and $chancellor? [Ja, Nein]")
        if (response) {
            voteCounter.getAndIncrement()
        } else {
            voteCounter.getAndDecrement()
        }
        return [(player): response]
    }

    def isNumberInRange(String value, int min, int max) {
        if (value.isNumber()) {
            def num = value as int
            if (num >= min && num <= max) {
                return true
            }
        }
        return false
    }

    int askPresidentDiscard(String president, List<Policy> policies) {
        def response = questionPlayer(president, "Choose a policy to DISCARD from $policies [1,2,3]")
        while (!isNumberInRange(response, 1, 3)) {
            response = questionPlayer(president, "Please choose a number between 1 and 3")
        }
        return response as int
    }

    int askChancellorDiscard(String chancellor, List<Policy> policies, boolean vetoEnabled) {
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

    def electionResults(List<String> ja, List<String> nein, int failedBefore, lastVoter) {
        if (Config.showLastVoter) {
            messageGroup("The last person to vote was: $lastVoter")
        }
        def isPass = ja.size() > nein.size()
        def result = isPass ? "passes" : "fails"
        messageGroup("The election $result: ${ja.size()}/${nein.size()} Ja$ja Nein$nein")
        if (!isPass) {
            def afterFailed = failedBefore + 1
            messageGroup("The failed election marker is now at $afterFailed")
        }
    }

    def tellEnactPolicy(String president, String chancellor, Policy policy) {
        messageGroup("President $president and Chancellor $chancellor enacted a $policy policy.")
    }

    def tellTopCard(Policy policy) {
        messageGroup("The next policy ($policy) will be automatically enacted.")
    }

    def tellPolicyResult(int drawSize, int discardSize, int libEnacted, int facEnacted) {
        def totalSize = drawSize + discardSize
        def summary = ["Draw": drawSize, "Discard": discardSize, "Total": totalSize]
        messageGroup("Policy piles: $summary.")
        messageGroup("Score: ${Policy.LIBERAL} $libEnacted/5; ${Policy.FASCIST} $facEnacted/6")
    }

    def peek(String president, List<Policy> topThree) {
        messageGroup("President $president is peeking at the next 3 policies on the draw pile.")
        messagePlayer(president, "The next three policies in the draw pile are: $topThree")
    }

    def inspect(String president, List<String> players, List<String> inspected, Map<String,Role> roles) {
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

    def askSpecialElection(String president, List<String> players) {
        messageGroup("Special Election. Waiting for President $president to nominate the next president.")
        def validPlayers = players - president
        def nextPrez = askPlayerName(players, president, "Whom do you wish to nominate as the next president? $validPlayers", { player -> null })
        messageGroup("President $president nominates $nextPrez to be the next president.")
        return nextPrez
    }

    def askExecute(String president, List<String> players) {
        messageGroup("President $president will now choose a player to execute.")
        def validPlayers = players - president
        def killPlayer = askPlayerName(players, president, "Choose a player to execute. $validPlayers", { player -> null })
        messageGroup("$killPlayer is dead")
        return killPlayer
    }

    def askVeto(String president, String chancellor) {
        messageGroup("Chancellor $chancellor moves to veto this agenda.")
        def result = askPlayerYesNo(president, "Do you agree to a veto of the current agenda? [Ja, Nein]")
        if (result) {
            messageGroup("President $president agrees to the veto, the policies are discarded and the failed government marker advances")
        } else {
            messageGroup("President $president disagrees to the veto, the chancellor must enact a policy.")
        }
        return result
    }

    def printResultTable(Events events) {
        messageGroup(" ")
        events.toLines().each { String it -> messageGroup(it) }
        messageGroup(" ")
    }

    def tellVictory(GameResult result) {
        messageGroup(" ")
        switch (result) {
        case GameResult.LIBERAL_WIN_COUNT:
            messageGroup("Liberals win by enacting 5 liberal policies")
            return
        case GameResult.FASCIST_WIN_COUNT:
            messageGroup("Fascists win by enacting 6 fascist policies")
            return
        case GameResult.HITLER_KILLED:
            messageGroup("Hitler has been executed. Liberals win!")
            return
        case GameResult.HITLER_ELECTED:
            messageGroup("The fascists win! Hitler has been elected chancellor.")
        }
    }

    def tellEndgameRoles(Map<String,Role> roles) {
        roles.each {
            messageGroup("${it.key} was ${it.value}")
        }
    }
}
