// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.Colors

enum ColorCode {
    RED(Config.console ? "\033[31m" : Colors.RED),
    BLUE(Config.console ? "\033[34m" : Colors.BLUE),
    NORMAL(Config.console ? "\033[0m" : Colors.NORMAL)

    String str

    ColorCode(str) {
        this.str = str
    }

    String toString() {
        return str
    }
}
