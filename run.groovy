
def gm = Config.console ? new ConsoleGameMaster() : new IRCGameMaster()
new Game(gm).run()
