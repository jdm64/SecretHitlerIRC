// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')
// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
//@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.22')

import org.pircbotx.*
import org.pircbotx.hooks.*
import org.pircbotx.hooks.events.*
import org.pircbotx.hooks.types.*
import java.util.concurrent.*


class IRCGame extends Game {
    def bot
    def botName = "shbot"
    def channelName = "#test"
    def channel
    def devoiced
    def listener = new IRCListener()

    class IRCListener extends ListenerAdapter {
        def lastMessage = new ConcurrentHashMap()

        public void onMessage(MessageEvent event) {
            if (event.message.startsWith(botName + ":")) {
                def command = event.message - "$botName:"
                switch (command.trim().toLowerCase()) {
                    case "start":
                        channel = event.channel
                        def users = channel.usersNicks
                        //startGame(users - botName)
                        createGame()
                        startGame(["one", "two", "three", "four", "five"])
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

    def giveVoice() {
        giveVoice(null)
    }
    def giveVoice(names) {
        channel.users.each { user ->
            if (!user.getNick().equals(botName)) {
                if (names == null || names.contains(user.getNick())) {
                    channel.send().voice(user)
                }
            }
        }
    }

    def takeVoice(names) {
        names << "daniel"
        channel.users.each { user ->
            if (names?.contains(user.getNick())) {
                channel.send().deVoice(user)
            }
        }
        devoiced = names
    }

    def startGame(names) {
        channel.send().setMode("+m")
        giveVoice()
        super.startGame(names)
    }
    
    def roundEnd() {
        if (devoiced) {
            giveVoice(devoiced)
        }
    }

    def electGovernment(president, chancellor) {
        def elected = super.electGovernment(president, chancellor)
        if (elected) {
            takeVoice([president, chancellor])
        }
        return elected
    }

    def endGame() {
        super.endGame()
        channel.send().setMode("-m")
    }

    def messageGroup(message) {
        channel.send().message(message)
    }

    def messagePlayer(name, message) {
        //bot.send().message(name, message)
        bot.send().message("daniel", "$name: $message")
    }

    def questionPlayer(name, question) {
        listener.clearLastMessage(name)
        messagePlayer(name, question)
        //return listener.nextMessageFrom(name)
        return listener.nextMessageFrom("daniel")
    }
}
