import org.pircbotx.Colors

class Policy {
    enum Type { LIBERAL, FASCIST }

    Type type
    boolean played

    String toString() {
        def color = Colors.RED
        if (type == Type.LIBERAL) {
            color = Colors.BLUE
        }
        return color + type + Colors.NORMAL
    }
}
