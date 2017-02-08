// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.Colors

enum Policy {
    LIBERAL(Colors.BLUE),
    FASCIST(Colors.RED)

    def color
    def Policy(color) {
        this.color = color
    }

    String toString() {
        return color + super.toString() + Colors.NORMAL
    }
}
