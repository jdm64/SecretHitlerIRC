
class IRCGameMaster extends GameMaster {

    def channel
    def bot
    def listener

    IRCGameMaster(channel, bot, listener) {
        this.channel = channel
        this.bot = bot
        this.listener = listener
    }

    def messageGroup(message) {
        channel.send().message(message)
    }

    def messagePlayer(name, message) {
        bot.send().message(Config.debug ? Config.debugUser : name, "$name: $message")
    }

    def questionPlayer(name, question) {
        listener.clearLastMessage(name)
        messagePlayer(name, question)
        return listener.nextMessageFrom(Config.debug ? Config.debugUser : name)
    }
}
