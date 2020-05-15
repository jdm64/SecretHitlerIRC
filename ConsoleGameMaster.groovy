
class ConsoleGameMaster extends GameMaster {

    def run(game) {
        def players = ["one", "two", "three", "four", "five", "six", "seven"]
        game.startGame(players)
    }

    def messageGroup(message) {
        println message
    }

    def messagePlayer(name, message) {
        println "$name: $message"
    }

    def questionPlayer(name, question) {
        messagePlayer(name, question)
        return System.console().readLine(">>> ")
    }
}
