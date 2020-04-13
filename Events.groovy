
class Events {
    def events = []

    def addEvent(e) {
        events << e
    }

    def dash(count) {
        return "-" * count
    }

    def divider(columns) {
        return "+" + columns.collect{ it -> dash(it.value) }.join("+") + "+"
    }

    def header(columns) {
        return "|" + columns.collect{ it -> it.key.center(it.value) }.join("|") + "|"
    }

    def minimalTable() {
        def eventsList = []

        eventsList << "President > Chancellor => Result ++ Ja -- Nein"

        events.each { event ->
            def line = event.president + " > "
            line += event.chancellor + " => "
            line += event.result + " ++ "
            line += event.ja + " -- "
            line += event.nein
            eventsList << line
        }

        return eventsList
    }

    def fullTable() {
        def eventsList = []

        def columns = [:]
        ["Round", "President", "Chancellor", "Result", "Ja", "Nein"].each{ i -> columns << [(i): i.size()] }

        events.each { event ->
            columns.President = Math.max(columns.President, event.president.size())
            columns.Chancellor = Math.max(columns.Chancellor, event.chancellor.size())

            // these have color codes
            def result = event.result
            def resSize = (result.contains("LIBERAL") || result.contains("FASCIST")) ? 7 : result.size()
            columns.Result = Math.max(columns.Result, resSize)

            // brackets will be removed so minus 2
            columns.Ja = Math.max(columns.Ja, event.ja.size() - 2)
            columns.Nein = Math.max(columns.Nein, event.nein.size() - 2)
        }

        // add size for space on either side
        columns.each { it.value += 2 }

        eventsList << divider(columns)
        eventsList << header(columns)
        eventsList << divider(columns)
        events.eachWithIndex { event, index ->
            def indexString = (index + 1) as String
            def line = "|"
            line += indexString.center(columns.Round)
            line += "|"
            line += event.president.center(columns.President)
            line += "|"
            line += event.chancellor.center(columns.Chancellor)
            line += "|"
            if (event.result.contains("LIBERAL") || event.result.contains("FASCIST")) {
                // These have color codes, so adjust the buffer size
                line += event.result.center(columns.Result + event.result.size() - 7)
            } else {
                line += event.result.center(columns.Result)
            }
            line += "|"
            line += (event.ja.size() > 2 ? event.ja[1..-2] : " ").center(columns.Ja)
            line += "|"
            line += (event.nein.size() > 2 ? event.nein[1..-2] : " ").center(columns.Nein)
            line += "|"
            eventsList << line
        }
        eventsList << divider(columns)
        return eventsList
    }

    def toLines() {
        return Config.minTable ? minimalTable() : fullTable()
    }
}
