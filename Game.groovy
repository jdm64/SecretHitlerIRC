
class Game {
    enum Role { LIBERAL, FASCIST, HITLER }

    String channel
    Map roles
    List players
    List drawPile
    List discardPile
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
        players.eachWithIndex { it, index ->
            println "${index + 1}: $it is ${roles.get(it)}"
        }

        return true
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
        println "Enacting $policy"
        if (policy.type == Policy.Type.LIBERAL) {
            libEnacted++
        } else if (policy.type == Policy.Type.FASCIST) {
            facEnacted++
            specialAction()
        }
        if (libEnacted == 5) {
            println "LIBS WIN"
            return true
        } else if (facEnacted == 6) {
            println "FACS WIN"
            return true
        }
        return false
    }

    def reshuffle() {
        drawPile.addAll(discardPile)
        discardPile.clear()
        Collections.shuffle(drawPile)
        println "RESHUFFLE ${drawPile}"
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

    def groupMessage(message) {
        println message
    }

    def privateMessage(name, message) {
        println "$name: $message"
    }
}
