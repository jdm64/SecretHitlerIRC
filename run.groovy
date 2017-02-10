
if (Boolean.getBoolean("console")) {
    g = new Game()
    g.createGame()
    g.startGame(["one", "two", "three", "four", "five"])
} else {
    g = new IRCGame()
}
