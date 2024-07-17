fun main() {
    // Test game
    val state = GameState(
        false,
        Deck("5S", "7D", "5C"),
        Deck.none(),
        OrderedDeck("2C")
    )
    makeMove(state)
}

fun makeMove(state: GameState) {
    // Get two decks to choose from
    val deck1 = state.pickupDeck
    val deck2 = Deck(state.centreCards[0])
    // Compare average card values
    val av1 = getAverageValue(deck1)
    val av2 = getAverageValue(deck2)
    // Output results
    println()
    println("Face-up card:\t$av2")
    println("Pickup deck:\t$av1")
    val choice = if (av1 > av2) "pickup deck" else "face-up card"
    println("You should choose the $choice.")
}

fun getAverageValue(deck: Deck) = deck
    .asSequence()
    .map { it.value }
    .average()