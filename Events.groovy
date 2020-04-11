
class Events {
    def events = []

    def addEvent(e) {
        events << e
    }

    def getSerializedEvents() {
        def eventsList = []

        def presSize = "President".size()
        def chanSize = "Chancellor".size()
        def resSize = "Result".size()
        def votSize = "Votes".size()
        events.each { event ->
            presSize = Math.max(presSize, event.president.size())
            chanSize = Math.max(chanSize, event.chancellor.size())

            def res = event.result.size()
            if (event.result.contains("LIBERAL") || event.result.contains("FASCIST")) {
                res = 7
            }
            resSize = Math.max(resSize, res)

            votSize = Math.max(votSize, event.votes.size())
        }
        def pad = 2
        presSize += pad
        chanSize += pad
        resSize += pad
        votSize += pad

        eventsList << "+" + ("-" * 7) + "+" + ("-" * presSize) + "+" + ("-" * chanSize) + "+" + ("-" * resSize) + "+" + ("-" * votSize) + "+"
        eventsList << "| Round |" + "President".center(presSize) + "|" + "Chancellor".center(chanSize) + "|" + "Result".center(resSize) + "|" + "Votes".center(votSize) + "|"
        eventsList << "+" + ("-" * 7) + "+" + ("-" * presSize) + "+" + ("-" * chanSize) + "+" + ("-" * resSize) + "+" + ("-" * votSize) + "+"
        events.eachWithIndex { event, index ->
            def indexString = (index + 1) as String
            def line = "|"
            line += indexString.center(7)
            line += "|"
            line += event.president.center(presSize)
            line += "|"
            line += event.chancellor.center(chanSize)
            line += "|"
            if (event.result.contains("LIBERAL") || event.result.contains("FASCIST")) {
                // These have color codes, so adjust the buffer size
                line += event.result.center(resSize + event.result.size() - 7)
            } else {
                line += event.result.center(resSize)
            }
            line += "|"
            line += event.votes.center(votSize)
            line += "|"
            eventsList << line
        }
        eventsList << "+" + ("-" * 7) + "+" + ("-" * presSize) + "+" + ("-" * chanSize) + "+" + ("-" * resSize) + "+" + ("-" * votSize) + "+"
        return eventsList
    }

    def dumpEvents() {
        getSerializedEvents().each { println it }
    }
}
