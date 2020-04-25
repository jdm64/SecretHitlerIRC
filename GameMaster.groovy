
abstract class GameMaster {

    def abstract messageGroup(message)

    def abstract messagePlayer(name, message)

    def abstract questionPlayer(name, question)

    def tellSetup(roles, drawPile) {
        if (Config.rebalance) {
            messageGroup("Rebalanced rules enabled")
        }

        def numLib = drawPile.count(Policy.LIBERAL)
        def numFas = drawPile.count(Policy.FASCIST)
        messageGroup("Draw pile: $numLib $Policy.LIBERAL and $numFas $Policy.FASCIST")

        def vals = roles.values()
        numLib = vals.count(Role.LIBERAL)
        numFas = vals.count(Role.FASCIST)
        def numHit = vals.count(Role.HITLER)
        messageGroup("Players: $numLib $Role.LIBERAL, $numFas $Role.FASCIST and $numHit $Role.HITLER")
    }

    def tellRoles(roles, hitlerKnows) {
        roles.each { player, role ->
            def msg = "You are $role"
            if (role == Role.FASCIST) {
                msg += ", the other fascists are "
                msg += roles.findAll{ it.key != player && it.value == Role.FASCIST }.collect{ it.key }
                msg += ", Hitler is " + roles.find{ it.value == Role.HITLER }.collect{ it.key }
            } else if (role == Role.HITLER && hitlerKnows) {
                msg += ", the other fascist is " + roles.find{ it.value == Role.FASCIST }.collect{ it.key }
            }
            messagePlayer(player, msg)
        }
    }
}
