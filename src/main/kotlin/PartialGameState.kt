// Captures all information about the current state that is known to me
data class PartialGameState(val turns: Int, val meKnocked: Boolean?, val stage: GameStage, val myCards: Set<Card>, val oppCards: Set<Card>, val centreCards: List<Card>) {

    // Returns all cards that are not known to be in any player's hand or in the discard pile
    fun getPickupDeck() =
        allCards() - myCards - oppCards - centreCards.toSet()

    // Returns all the FullGameStates that could be reality given the information in this PartialGameState
    fun branch() =
        oppCards.padFrom(stage.oppHandSize, getPickupDeck()).map { toFullGameState(it) }

    // Returns the Action that corresponds to the best move in this scenario, based on the information known by me
    fun getBestMove(depth: Int): Action {
        val actionProbabilities = stage.actions.associateWith { action ->
            // Get all the possible FullGameStates that could result from this action being played (packaged into GameStateTreeNodes)
            val roots = branch().map { action.branch(it) }.foldToList().map { GameStateTreeNode(it) }
            // Branch all of these down to the specified depth
            roots.forEach { it.branch(depth) }
            // Calculate win probabilities and play probabilities
            roots.forEach { root ->
                root.asSequence().forEach { node ->
                    if (node.isEmpty()) {
                        // This node is a leaf, so we can calculate its win probability directly
                        node.winProbability = node.value.getWinProbability()
                    } else {
                        // This node has children, and all of their win probabilities will have been calculated
                        // Calculate each child's probability of being played
                        if (node.value.stage.isOpp) {
                            // The opponent is choosing which move will be played
                            // Distribute the play probabilities inversely according to the win probabilities
                            val oldTotal = node.sumOf { it.winProbability!! }
                            val newWinProbabilities = node.associateWith { oldTotal - it.winProbability!! }
                            val newTotal = newWinProbabilities.values.sum()
                            node.forEach { it.playProbability = if (newTotal == 0.0) 0.0 else newWinProbabilities[it]!! / newTotal }
                        } else {
                            // I am choosing which move will be played
                            // Distribute the play probabilities according to the win probabilities
                            val totalWinProbability = node.sumOf { it.winProbability!! }
                            node.forEach { it.playProbability = if (totalWinProbability == 0.0) 0.0 else it.winProbability!! / totalWinProbability }
                        }
                        // Calculate this node's win probability
                        node.winProbability = node.sumOf { it.winProbability!! * it.playProbability!! }
                    }
                }
            }
            // Each root should now have its winProbability and playProbability defined
            // Get the total probability of winning from this action
            roots.sumOf { it.winProbability!! }
        }
        println(actionProbabilities)
        return actionProbabilities.maxBy { it.value }.key
    }

    // Converts this PartialGameState into a FullGameState given the extra information of the opponent's cards
    fun toFullGameState(oppCards: Iterable<Card>): FullGameState {
        return FullGameState(turns, meKnocked, stage, myCards, oppCards.toSet(), centreCards)
    }

    override fun toString(): String {
        return "PartialGameState(turns=$turns, meKnocked=$meKnocked, stage=$stage, myCards=$myCards, oppCards=$oppCards, centreCards=$centreCards)"
    }
}