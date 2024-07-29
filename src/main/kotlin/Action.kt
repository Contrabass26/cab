import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

private val LOGGER = LogManager.getLogger("Action")

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

    // Returns the probability of winning if I play this action at this GameState
    fun getWinProbability(state: FullGameState, depth: Int): Double {
        LOGGER.debug("If action {} happens at state {}, what's the probability that I win?", this, state)
        val states = branch(state) // We are assuming that all action branches are equally likely
        // Base case
        if (depth <= 0 && (state.stage == GameStage.ME_UP || state.stage == GameStage.OPP_UP)) {
            LOGGER.debug("Max depth reached - let's simplify things")
            val result = states.map {
                val myHandValue = it.myCards.getHandValue()
                val oppHandValue = it.oppCards.getHandValue()
                when {
                    myHandValue > oppHandValue -> 0.7
                    myHandValue < oppHandValue -> 0.2
                    else -> 0.5
                }
            }.average()
            LOGGER.debug("Returning $result")
            return result
        }
        // Recursion
        val result = states.map { newState ->
            LOGGER.debug("Suppose the action results in {}, what actions could follow", newState - state)
            // Suppose this one happened
            // What actions could follow?
            val actions = newState.stage.actions(newState)
            LOGGER.debug("These ones: {}", actions)
            // Find the win probabilities if each of these were played
            val winProbabilities = actions.associateWith { it.getWinProbability(newState, depth - 1) }
            LOGGER.debug("So the probabilities of me winning at {} after the actions are played are:", newState)
            LOGGER.debug("{}", winProbabilities)
            LOGGER.debug("Now for the play probabilities")
            // Find the probabilities of each of these being played
            // TODO: Should this go outside of the block?
            // TODO: A better way to distribute play probabilities based on win probabilities - currently everything seems to tend to 0.5
            val playProbabilities = if (newState.stage.isOpp) {
                LOGGER.debug("It's the opponent's turn, so bad things will probably happen for me")
                // The opponent is choosing which move will be played
                // Distribute the play probabilities inversely according to the win probabilities
                val oppWinProbabilities = winProbabilities.mapValues { 1 - it.value }
                val oppWinTotal = oppWinProbabilities.values.sum()
                if (oppWinTotal == 0.0)
                    oppWinProbabilities.mapValues { 1.0 / oppWinProbabilities.size }
                else
                    oppWinProbabilities.mapValues { it.value / oppWinTotal }
            } else {
                LOGGER.debug("It's my turn, so only the best move has a chance of being played")
                // I am choosing which move will be played, so I would always choose the highest win probability
                val maxValue = winProbabilities.values.max()
                val probability = 1.0 / winProbabilities.values.count { it == maxValue }
                winProbabilities.mapValues { if (it.value == maxValue && !probability.isNaN()) probability else 0.0 }
//                val total = winProbabilities.values.sum()
//                if (total == 0.0)
//                    winProbabilities.mapValues { 1.0 / winProbabilities.size }
//                else
//                    winProbabilities.mapValues { it.value / total }
            }
            LOGGER.debug("So the probabilities of playing the actions are:")
            LOGGER.debug("{}", playProbabilities)
            // Find overall win probability if the action results in this state
            val total = playProbabilities.mapValues { it.value * winProbabilities[it.key]!! }.values.sum()
            LOGGER.debug("The total probability for this state is {}", total)
            total
        }.sumOf { it / states.size }
        LOGGER.debug("The average final probability across all states is {}", result)
        return result
    }
}