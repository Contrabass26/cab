import java.lang.IllegalArgumentException

fun <T> List<T>.prependCopyGet(item: T): MutableList<T> {
    val newList = this.toMutableList()
    newList.addFirst(item)
    return newList
}

// An action that the player can do. Disregards details that are beyond the player's control e.g. deck randomness.
class Action(private val toString: String, val branch: (FullGameState) -> List<FullGameState>) {

    companion object {
        fun ME_PLAY_CARD(card: Card) = Action("ME_PLAY_CARD($card)") { state ->
            with(state) {
                assert(card in myCards)
                assert(stage == GameStage.ME_DOWN)
                listOf(copy(stage = GameStage.OPP_UP, myCards = myCards - card, centreCards = centreCards.prependCopyGet(card)))
            }
        }

        fun OPP_PLAY_CARD(card: Card) = Action("OPP_PLAY_CARD($card)") { state ->
            with(state) {
                assert(card in oppCards)
                assert(stage == GameStage.OPP_DOWN)
                listOf(copy(stage = GameStage.ME_UP, oppCards = oppCards - card, centreCards = centreCards.prependCopyGet(card)))
            }
        }

        val ME_KNOCK = Action("ME_KNOCK") { state ->
            with(state) {
                assert(stage == GameStage.ME_UP)
                listOf(copy(stage = GameStage.OPP_UP, meKnocked = true))
            }
        }

        val OPP_KNOCK = Action("OPP_KNOCK") { state ->
            with(state) {
                assert(stage == GameStage.ME_UP)
                listOf(copy(stage = GameStage.ME_UP, meKnocked = false))
            }
        }

        val ME_PICK_BLIND = Action("ME_PICK_BLIND") { state ->
            with(state) {
                assert(stage == GameStage.ME_UP)
                getPickupDeck().map { copy(stage = GameStage.ME_DOWN, myCards = myCards + it) }
            }
        }

        val OPP_PICK_BLIND = Action("OPP_PICK_BLIND") { state ->
            with(state) {
                assert(stage == GameStage.OPP_UP)
                getPickupDeck().map { copy(stage = GameStage.OPP_DOWN, oppCards = oppCards + it) }
            }
        }

        val ME_PICK_VISIBLE = Action("ME_PICK_VISIBLE") { state ->
            with(state) {
                val visibleCard = centreCards.firstOrNull()
                assert(stage == GameStage.ME_UP && visibleCard != null)
                listOf(
                    copy(
                        stage = GameStage.ME_DOWN,
                        myCards = myCards + visibleCard!!,
                        centreCards = centreCards - visibleCard
                    )
                )
            }
        }

        val OPP_PICK_VISIBLE = Action("OPP_PICK_VISIBLE") { state ->
            with(state) {
                val visibleCard = centreCards.firstOrNull()
                assert(stage == GameStage.OPP_UP && visibleCard != null)
                listOf(
                    copy(
                        stage = GameStage.OPP_DOWN,
                        oppCards = oppCards + visibleCard!!,
                        centreCards = centreCards - visibleCard
                    )
                )
            }
        }
    }

    override fun toString(): String {
        return toString
    }

    private fun log(message: String, depth: Int) {
        println("\t".repeat(DEPTH - depth) + message)
    }

    // Returns the probability of winning if I play this action at this GameState
    fun getWinProbability(state: FullGameState, depth: Int): Double {
        log("If action $this happens at state $state, what's the probability that I win?", depth)
        val states = branch(state) // We are assuming that all action branches are equally likely
        // Base case
        if (depth < 0) throw IllegalArgumentException("Depth was <0")
        if (depth == 0) {
            log("Max depth reached - let's simplify things", depth)
            val result = states.map {
                val myHandValue = it.myCards.getHandValue()
                val oppHandValue = it.oppCards.getHandValue()
                when {
                    myHandValue > oppHandValue -> 1.0
                    myHandValue < oppHandValue -> 0.0
                    else -> 0.5
                }
            }.average()
            log("Returning $result", depth)
            return result
        }
        // Recursion
        val result = states.map { newState ->
            log("Suppose the action results in ${newState - state}, what actions could follow", depth)
            // Suppose this one happened
            // What actions could follow?
            val actions = newState.stage.actions(newState)
            log("These ones: $actions", depth)
            // Find the win probabilities if each of these were played
            val winProbabilities = actions.associateWith { it.getWinProbability(newState, depth - 1) }
            log("So the probabilities of me winning after playing the actions are:", depth)
            log("$winProbabilities", depth)
            log("Now for the play probabilities", depth)
            // Find the probabilities of each of these being played
            // TODO: Should this go outside of the block?
            val playProbabilities = if (newState.stage.isOpp) {
                log("It's the opponent's turn, so bad things will probably happen for me", depth)
                // The opponent is choosing which move will be played
                // Distribute the play probabilities inversely according to the win probabilities
                val oldTotal = winProbabilities.values.sum()
                val newWinProbabilities = winProbabilities.mapValues { oldTotal - it.value }
                val newTotal = newWinProbabilities.values.sum()
                if (newTotal == 0.0) winProbabilities else winProbabilities.mapValues { it.value / newTotal }
            } else {
                log("It's my turn, so only the best move has a chance of being played", depth)
                // I am choosing which move will be played, so I would always choose the highest win probability
                val maxValue = winProbabilities.values.max()
                val probability = 1.0 / winProbabilities.values.count { it == maxValue }
                winProbabilities.mapValues { if (it.value == maxValue && !probability.isNaN()) probability else 0.0 }
            }
            log("So the probabilities of playing the actions are:", depth)
            log("$playProbabilities", depth)
            // Find overall win probability if the action results in this state
            val total = playProbabilities.mapValues { it.value * playProbabilities[it.key]!! }.values.sum()
            log("The total probability for this state is $total", depth)
            total
        }.average()
        log("The average final probability across all states is $result", depth)
        return result
    }
}