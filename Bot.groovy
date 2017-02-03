// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')
// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
//@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.22')


import org.pircbotx.*
import org.pircbotx.hooks.*
import org.pircbotx.hooks.types.*


class Bot extends ListenerAdapter {
    static PircBotX bot

    public void onGenericMessage(GenericMessageEvent event) {
        event.respondPrivateMessage(event.message)
        event.respondWith(event.message)
        bot.send().message("#test", "Hello There")
    }

    def static void startBot() {
        def config = new Configuration.Builder()
            .setName("shbot")
            .addServer("skynet.parasoft.com")
            .addListener(new Bot())
            .addAutoJoinChannel("#test")
            .buildConfiguration()
        bot = new PircBotX(config)
        bot.startBot()
        println "bot started"
    }

}
