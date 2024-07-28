// Captures all the information about the current state that is known by either player
data class FullGameState(val turns: Int, val meKnocked: Boolean?, val stage: GameStage, val myCards: Set<Card>, val oppCards: Set<Card>, val centreCards: List<Card>) {

    fun getPickupDeck() =
        allCards() - myCards - oppCards - centreCards.toSet()

    fun getWinProbability(): Double {
        return when {
            myCards.getHandValue() > oppCards.getHandValue() -> 1.0
            myCards.getHandValue() < oppCards.getHandValue() -> 0.0
            else -> 0.5
        }
    }

    private fun meWins(): Boolean? {
        if (myCards.getHandValue() == 31) return true
        if (oppCards.getHandValue() == 31) return false
        if (stage == GameStage.OPP_UP && meKnocked == false)
            return myCards.getHandValue() >= oppCards.getHandValue()
        if (stage == GameStage.ME_UP && meKnocked == true)
            return myCards.getHandValue() > oppCards.getHandValue()
        return null
    }

    override fun toString(): String {
        return "GameState(turns=$turns, meKnocked=$meKnocked, stage=$stage, myCards=$myCards, oppCards=$oppCards, centreCards=$centreCards)"
    }
}