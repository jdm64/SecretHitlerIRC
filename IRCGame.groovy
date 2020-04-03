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
    def debug = Boolean.getBoolean("debug")
    def voicing = Boolean.getBoolean("voicing")
    def debugUser = "dan"
    def bot
    def botName = "shitler"
    def channelName = "#game"
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
                        createGame()
                        def users = channel.usersNicks
                        if (debug) {
                            startGame(["one", "two", "three", "four", "five"])
                        } else {
                            startGame(new ArrayList(users - botName))
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
        if (voicing) {
            channel.users.each { user ->
                if (!user.getNick().equals(botName)) {
                    if (names == null || names.contains(user.getNick())) {
                        channel.send().voice(user)
                    }
                }
            }
        }
    }

    def takeVoice(names) {
        if (voicing) {
            if (debug) {
                names << debugUser
            }
            channel.users.each { user ->
                if (names?.contains(user.getNick())) {
                    channel.send().deVoice(user)
                }
            }
        devoiced = names
        }
    }

    def startGame(names) {
        if (voicing) {
            channel.send().setMode("+m")
        }
        giveVoice()
        super.startGame(names)
    }
    
    def roundEnd() {
        if (devoiced) {
            giveVoice(devoiced)
        }
    }

    def electGovernment(president, chancellor) {
        if (Boolean.getBoolean("autoelect")) {
            return true
        } else {
            def elected = super.electGovernment(president, chancellor)
            if (elected) {
                takeVoice([president, chancellor])
            }
            return elected
        }
    }

    def kill(user) {
        //takeVoice([user])
        super.kill(user)
    }

    def endGame() {
        roles.each {
            messageGroup "${it.key} was ${it.value}"
        }
        giveVoice(null)
    }

    def messageGroup(message) {
        channel.send().message(message)
    }

    def messagePlayer(name, message) {
        if (debug) {
            bot.send().message(debugUser, "$name: $message")
        } else {
            bot.send().message(name, message)
        }
    }

    def questionPlayer(name, question) {
        listener.clearLastMessage(name)
        messagePlayer(name, question)
        if (debug) {
            return listener.nextMessageFrom(debugUser)
        } else {
            return listener.nextMessageFrom(name)
        }
    }

    def printEvents() {
        events.getSerializedEvents().each { event ->
            messageGroup(event)
        }
    }
}
