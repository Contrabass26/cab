class GameStateTreeNode constructor(val value: FullGameState, var winProbability: Double? = null, var playProbability: Double? = null, val parent: GameStateTreeNode? = null, private val children: MutableList<GameStateTreeNode> = mutableListOf()) : MutableList<GameStateTreeNode> by children {

    fun print(depth: Int = 0) {
        print("\t".repeat(depth))
        println(this.value)
        children.forEach { it.print(depth + 1) }
    }

    fun add(value: FullGameState) {
        add(GameStateTreeNode(value))
    }

    fun asSequence(): Sequence<GameStateTreeNode> = sequence {
        children.forEach { yieldAll(it.asSequence()) }
        yield(this@GameStateTreeNode)
    }
}

fun GameStateTreeNode.branch(depth: Int) {
    if (depth >= 0) {
        value.stage.actions
            .map { it.branch(value) }
            .foldToList()
            .map { GameStateTreeNode(it, parent = this) }
            .forEach {
                it.branch(depth - 1)
                this.add(it)
            }
    }
}
