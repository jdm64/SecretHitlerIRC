
enum Role {
    LIBERAL(ColorCode.BLUE),
    FASCIST(ColorCode.RED),
    HITLER(ColorCode.RED)

    ColorCode color

    Role(color) {
        this.color = color
    }

    String toString() {
        return "${color}${super.toString()}${ColorCode.NORMAL}"
    }
}
