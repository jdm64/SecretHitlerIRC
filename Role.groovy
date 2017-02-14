// https://mvnrepository.com/artifact/org.pircbotx/pircbotx
@Grab(group='org.pircbotx', module='pircbotx', version='2.1')

import org.pircbotx.Colors

enum Role {
    LIBERAL (Colors.BLUE),
    FASCIST (Colors.RED),
    HITLER (Colors.RED)

    def color

    Role(color) {
        this.color = color
    }

    public String toString() {
        return color + super.toString() + Colors.NORMAL
    }
}
