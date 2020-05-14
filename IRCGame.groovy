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
    def channel
    def devoiced
    def listener = new IRCListener()

    class IRCListener extends ListenerAdapter {
        def lastMessage = new ConcurrentHashMap()

        public void onMessage(MessageEvent event) {
            if (event.message.startsWith(Config.botName + ":")) {
                def command = event.message - "${Config.botName}:"
                switch (command.trim().toLowerCase()) {
                    case "start":
                        channel = event.channel
                        def gm = new IRCGameMaster(bot, channel, listener)
                        if (Config.debug) {
                            def players = ["one", "two", "three", "four", "five"]
                            startGame(gm, players)
                        } else {
                            startGame(gm, channel.usersNicks - Config.botName)
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

    def giveVoice() {
        giveVoice(null)
    }

    def giveVoice(names) {
        if (Config.voicing) {
            channel.users.each { user ->
                if (!user.getNick().equals(Config.botName)) {
                    if (names == null || names.contains(user.getNick())) {
                        channel.send().voice(user)
                    }
                }
            }
        }
    }

    def takeVoice(names) {
        if (Config.voicing) {
            if (Config.debug) {
                names << Config.debugUser
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
        if (Config.voicing) {
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

    def electGovernment(event, president, chancellor) {
        def elected = super.electGovernment(event, president, chancellor)
        if (elected) {
            takeVoice([president, chancellor])
        }
        return elected
    }

    def kill(user) {
        takeVoice([user])
        super.kill(user)
    }

    def endGame() {
        super.endGame()
        giveVoice(null)
    }
}
