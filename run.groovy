
if (Config.console) {
    g = new Game()
    def players = ["one", "two", "three", "four", "five", "six", "seven"]
    g.startGame(new ConsoleGameMaster(), players)
} else {
    g = new IRCGame()
}
