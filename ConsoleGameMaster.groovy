
class ConsoleGameMaster extends GameMaster {

    def run(Game game) {
        def players = ["one", "two", "three", "four", "five", "six", "seven"]
        game.startGame(players)
    }

    def messageGroup(String message) {
        println message
    }

    def messagePlayer(String name, String message) {
        println message
    }

    String questionPlayer(String name, String question) {
        messagePlayer(name, question)
        Thread.currentThread().sleep(100)
        return System.console().readLine("$name >>> ")
    }
}
