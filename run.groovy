
if (Config.console) {
    g = new Game()
    g.startGame(["one", "two", "three", "four", "five", "six", "seven"])
} else {
    g = new IRCGame()
}
