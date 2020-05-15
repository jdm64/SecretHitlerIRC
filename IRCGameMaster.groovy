
class IRCGameMaster extends GameMaster {

    def game

    IRCGameMaster(IRCgame) {
        game = IRCgame
    }

    def messageGroup(message) {
        game.channel.send().message(message)
    }

    def messagePlayer(name, message) {
        game.bot.send().message(Config.debug ? Config.debugUser : name, "$name: $message")
    }

    def questionPlayer(name, question) {
        game.listener.clearLastMessage(name)
        messagePlayer(name, question)
        return listener.nextMessageFrom(Config.debug ? Config.debugUser : name)
    }
}
