// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')
// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
//@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.22')

import org.pircbotx.*
import org.pircbotx.hooks.*
import org.pircbotx.hooks.events.*
import org.pircbotx.hooks.types.*


class IRCGame extends Game {
    def bot
    def botName = "shbot"
    def channelName = "#test"
    def listener = new IRCListener()

    class IRCListener extends ListenerAdapter {
        public void onMessage(MessageEvent event) {
            if (event.message.startsWith(botName + ":")) {
                def command = event.message - "$botName:"
                switch (command.trim().toLowerCase()) {
                    case "start":
                        def users = event.channel.usersNicks
                        //startGame(users - botName)
                        startGame(["one", "two", "three", "four", "five"])
                        break;
                }
            }
        }

        public void onPrivateMessage(PrivateMessageEvent event) {
            println "Private message $event.message"
        }
    
    }

    def IRCGame() {
        def config = new Configuration.Builder()
            .setName(botName)
            .addServer("skynet.parasoft.com")
            .addListener(listener)
            .addAutoJoinChannel(channelName)
            .buildConfiguration()
        bot = new PircBotX(config)
        Thread.start {
            bot.startBot()
        }
    }

    def messageGroup(message) {
        bot.send().message(channelName, message)
    }

    def messagePlayer(name, message) {
        //bot.send().message(name, message)
        bot.send().message("daniel", "$name: $message")
    }

    def questionPlayer(name, question) {
        //messagePlayer(name, question)
        bot.send().notice("daniel", "$name: $message")
        return System.console().readLine("What is your response? ")
    }
}
