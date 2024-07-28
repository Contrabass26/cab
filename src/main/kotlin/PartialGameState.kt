// Captures all information about the current state that is known to me
data class PartialGameState(val turns: Int, val meKnocked: Boolean?, val stage: GameStage, val myCards: Set<Card>, val oppCards: Set<Card>, val centreCards: List<Card>) {

    // Returns all cards that are not known to be in any player's hand or in the discard pile
    fun getPickupDeck() =
        allCards() - myCards - oppCards - centreCards.toSet()

    // Returns all the FullGameStates that could be reality given the information in this PartialGameState
    fun branch() =
        oppCards.padFrom(stage.oppHandSize, getPickupDeck()).map { toFullGameState(it) }

    // Converts this PartialGameState into a FullGameState given the extra information of the opponent's cards
    fun toFullGameState(oppCards: Iterable<Card>): FullGameState {
        return FullGameState(meKnocked, stage, myCards, oppCards.toSet(), centreCards)
    }

    override fun toString(): String {
        return "PartialGameState(turns=$turns, meKnocked=$meKnocked, stage=$stage, myCards=$myCards, oppCards=$oppCards, centreCards=$centreCards)"
    }
}