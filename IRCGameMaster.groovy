@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.*
import org.pircbotx.hooks.*
import org.pircbotx.hooks.events.*
import org.pircbotx.hooks.types.*
import java.util.concurrent.*

class IRCGameMaster extends GameMaster {

    def game
    def bot
    def channel
    def listener = new IRCListener()

    class IRCListener extends ListenerAdapter {
        def lastMessage = new ConcurrentHashMap()

        public void onMessage(MessageEvent event) {
            if (event.message.startsWith(Config.botName + ":")) {
                def command = event.message - "${Config.botName}:"
                switch (command.trim().toLowerCase()) {
                    case "start":
                        channel = event.channel
                        if (Config.debug) {
                            def players = ["one", "two", "three", "four", "five"]
                            game.startGame(players)
                        } else {
                            game.startGame(channel.usersNicks - Config.botName)
                        }
                        break;
                }
            }
        }

        public void onPrivateMessage(PrivateMessageEvent event) {
            lastMessage << [(event.user.getNick()): event.message]
        }

        def clearLastMessage(name) {
            lastMessage.remove(name)
        }

        def nextMessageFrom(name) {
            while (!lastMessage.containsKey(name)) {
                Thread.currentThread().sleep(500)
            }
            return lastMessage.remove(name)
        }
    }

    def run(gameInstance) {
        game = gameInstance
        def config = new Configuration.Builder()
            .setName(Config.botName)
            .addServer(Config.server)
            .addListener(listener)
            .addAutoJoinChannel(Config.channel)
            .setMessageDelay(10)
            .buildConfiguration()
        bot = new PircBotX(config)
        Thread.start {
            bot.startBot()
        }
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
