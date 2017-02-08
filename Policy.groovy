// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.Colors

class Policy {
    enum Type { LIBERAL, FASCIST }

    Type type

    String toString() {
        def color = Colors.RED
        if (type == Type.LIBERAL) {
            color = Colors.BLUE
        }
        return color + type + Colors.NORMAL
    }
}
