
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
            line += event.result.center(29)
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
