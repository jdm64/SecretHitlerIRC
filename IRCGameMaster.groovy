@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.*
import org.pircbotx.hooks.*
import org.pircbotx.hooks.events.*
import org.pircbotx.hooks.types.*
import java.util.concurrent.*

class IRCGameMaster extends GameMaster {

    Game game
    PircBotX bot
    Channel channel
    IRCListener listener = new IRCListener()

    class IRCListener extends ListenerAdapter {
        def lastMessage = new ConcurrentHashMap<String,String>()

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
                    case "tovote":
                        game.printWaitingVoters()
                }
            }
        }

        public void onPrivateMessage(PrivateMessageEvent event) {
            lastMessage << [(event.user.getNick()): event.message]
        }

        def clearLastMessage(String name) {
            lastMessage.remove(name)
        }

        def nextMessageFrom(String name) {
            while (!lastMessage.containsKey(name)) {
                Thread.currentThread().sleep(500)
            }
            return lastMessage.remove(name)
        }
    }

    def run(Game gameInstance) {
        game = gameInstance
        def config = new Configuration.Builder()
            .setName(Config.botName)
            .addServer(Config.server)
            .addListener(listener)
            .addAutoJoinChannel(Config.channel)
            .setMessageDelay(1)
            .buildConfiguration()
        bot = new PircBotX(config)
        Thread.start {
            bot.startBot()
        }
    }

    def messageGroup(String message) {
        channel.send().message(message)
    }

    def messagePlayer(String name, String message) {
        bot.send().message(Config.debug ? Config.debugUser : name, message)
    }

    String questionPlayer(String name, String question) {
        listener.clearLastMessage(name)
        messagePlayer(name, question)
        return listener.nextMessageFrom(Config.debug ? Config.debugUser : name)
    }
}
