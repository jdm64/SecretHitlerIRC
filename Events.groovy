
class Events {
    def events = []

    def addEvent(e) {
        events << e
    }

    def getSerializedEvents() {
        def eventsList = []
        eventsList << "+-------+---------------------+----------------------+-------------------------+"
        eventsList << "| Event |      President      |      Chancellor      |         Result          |"
        eventsList << "+-------+---------------------+----------------------+-------------------------+"
        events.eachWithIndex { event, index ->
            def indexString = (index + 1) as String
            def line = "|"
            line += indexString.center(7)
            line += "|"
            line += event.president.center(21)
            line += "|"
            line += event.chancellor.center(22)
            line += "|"
            if (event.result.contains("LIBERAL") || event.result.contains("FASCIST")) {
                // These have color codes, so adjust the buffer size
                line += event.result.center(25 + event.result.size() - 7)
            } else {
                line += event.result.center(25)
            }
            line += "|"
            eventsList << line
        }
        eventsList << "+-------+---------------------+----------------------+-------------------------+"
        return eventsList
    }

    def dumpEvents() {
        getSerializedEvents().each { println it }
    }
}
