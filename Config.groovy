
class Config {
    static boolean console = Boolean.getBoolean("console")
    static boolean debug = Boolean.getBoolean("debug")
    static boolean autoelect = Boolean.getBoolean("autoelect")
    static boolean voicing = Boolean.getBoolean("voicing")

    static String server = System.getProperty("server", "skynet.parasoft.com")
    static String debugUser = System.getProperty("debugUser", "dan")
    static String botName = System.getProperty("botName", "shitler")
    static String channelName = System.getProperty("channel", "#game")
}
