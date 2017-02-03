//Bot.startBot()

g = new Game()
g.createGame()
if(g.startGame(["one", "two", "three", "four", "five", "six", "seven"])) {
    1.upto(5, {
        def draw = g.drawPolicies()
        //println draw
        g.discardPolicy(draw.pop())
        g.discardPolicy(draw.pop())
        if (g.enactPolicy(draw.pop())) {
            System.exit(0)
        }
    })
    
    1.upto(5, {
        def draw = g.drawPolicies()
        //println draw
        g.discardPolicy(draw.pop())
        g.discardPolicy(draw.pop())
        if (g.enactPolicy(draw.pop())) {
            System.exit(0)
        }
    })
    println g.drawPile
}
