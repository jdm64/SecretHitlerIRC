
class Config {
    static Map fields = [
        "console": false,
        "debug": false,
        "autoelect": false,
        "minTable": false,
        "rebalance": true,
        "showLastVoter": true,
        "server": "skynet.parasoft.com",
        "debugUser": "dan",
        "botName": "shitler",
        "channel": "#game"
    ]

    static {
        load()
    }

    static def load() {
        def props = new Properties()

        def file = new File("settings.properties")
        if (file.exists()) {
            println("Loading Config from: $file")
            file.withInputStream {
                try {
                    props.load(it)
                } catch (Throwable t) {
                    println("Failed to load config file: ${t.getMessage()}") //.getMessage()
                }
            }
        } else {
            println("Loading Config from System properties. Create $file to load from file.")
        }

        // override with system properties
        props.putAll(System.getProperties())

        fields.each {
            def val = props.containsKey(it.key) ? props.get(it.key) : it.value
            if (it.value instanceof Boolean) {
                it.value = Boolean.valueOf(val)
            } else if (it.value instanceof Integer) {
                it.value = Integer.valueOf(val)
            } else {
                it.value = val
            }
        }

        println("Config settings:")
        fields.each {
            println("\t$it.key = $it.value")
        }
        println()
    }

    static def $static_propertyMissing(name) {
        if (!fields.containsKey(name)) {
            println("Config key '$name' not found")
        }
        return fields[name]
    }
}
