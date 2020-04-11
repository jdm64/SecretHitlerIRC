
enum Policy {
    LIBERAL(ColorCode.BLUE),
    FASCIST(ColorCode.RED)

    ColorCode color

    Policy(color) {
        this.color = color
    }

    String toString() {
        return "${color}${super.toString()}${ColorCode.NORMAL}"
    }
}
