
class ConsoleGameMaster extends GameMaster {

    def run(game) {
        def players = ["one", "two", "three", "four", "five", "six", "seven"]
        game.startGame(players)
    }

    def messageGroup(message) {
        println message
    }

    def messagePlayer(name, message) {
        println message
    }

    def questionPlayer(name, question) {
        messagePlayer(name, question)
        Thread.currentThread().sleep(100)
        return System.console().readLine("$name >>> ")
    }
}
