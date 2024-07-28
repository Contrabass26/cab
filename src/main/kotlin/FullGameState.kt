// Captures all the information about the current state that is known by either player
data class FullGameState(val meKnocked: Boolean?, val stage: GameStage, val myCards: Set<Card>, val oppCards: Set<Card>, val centreCards: List<Card>) {

    fun getPickupDeck() =
        allCards() - myCards - oppCards - centreCards.toSet()

    private fun meWins(): Boolean? {
        if (myCards.getHandValue() == 31) return true
        if (oppCards.getHandValue() == 31) return false
        if (stage == GameStage.OPP_UP && meKnocked == false)
            return myCards.getHandValue() >= oppCards.getHandValue()
        if (stage == GameStage.ME_UP && meKnocked == true)
            return myCards.getHandValue() > oppCards.getHandValue()
        return null
    }

    fun getBestMove(depth: Int): Action {
        val winProbabilities = stage.actions(this).associateWith { it.getWinProbability(this, depth) }
        println(winProbabilities)
        return winProbabilities.maxBy { it.value }.key
    }

    override fun toString(): String {
        return "${if (meKnocked != null) "$meKnocked " else ""}$stage $myCards $oppCards ${centreCards[0]}"
    }

    operator fun minus(other: FullGameState): String {
        val knockDiff = if (meKnocked != other.meKnocked) {
            if (meKnocked!!) "ME_KNOCK" else "OPP_KNOCK"
        } else ""
        val myCardsDiff = when {
            myCards.size > other.myCards.size -> "me+${myCards - other.myCards}"
            myCards.size < other.myCards.size -> "me-${other.myCards - myCards}"
            else -> ""
        }
        val oppCardsDiff = when {
            oppCards.size > other.oppCards.size -> "opp+${oppCards - other.oppCards}"
            oppCards.size < other.oppCards.size -> "opp-${other.oppCards - oppCards}"
            else -> ""
        }
        val centreCardDiff = if (centreCards[0] != other.centreCards[0]) "mid+${centreCards[0]}" else ""
        return listOf(knockDiff, myCardsDiff, oppCardsDiff, centreCardDiff).filterNot { it == "" }.joinToString(" ")
    }
}