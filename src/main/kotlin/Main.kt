fun main() {
    val state = initGame()
    while (true) {
        oppMove(state)
        myMove(state)
    }
}

fun initGame(): GameState {
    val myCards = Deck.none()
    for (i in 1..3) {
        myCards += Card(input("Enter card $i: "))
    }
    return GameState(false, false, myCards, Deck.none(), Deck.none())
}

fun input(prompt: String): String {
    print(prompt)
    return readln()
}

fun myMove(state: GameState) {
    println("\n--- ME ---")
    println("My cards: ${state.myCards}")
    println("Top card: ${state.centreCards[0]}")
    // Get two decks to choose from
    val pickupDeck = state.getPickupDeck()
    println("Pickup deck = $pickupDeck")
    val centreCard = state.centreCards[0]
    // Compare hand values after pickup
    val pickupValues = pickupDeck.asSequence()
        .map { state.myCards + it }
        .map { it.getHandValue() }
        .toList()
    println("Pickup values = $pickupValues")
    val centreValue = (state.myCards + centreCard).getHandValue()
    println("Centre value = $centreValue")
    val centreScore = pickupValues.sumOf { centreValue - it } / pickupValues.size.toDouble()
    // Output results
    if (centreScore >= 0) {
        println("Take the face-up card (%.2f)".format(centreScore))
    } else {
        println("Take a random card (%.2f)".format(centreScore))
    }
    // Get card that was picked
    state.myCards += if (centreScore >= 0)
        centreCard
    else
        Card(input("Enter drawn card: "))
    // Choose card to put down
    val putDownCard = state.myCards.asSequence()
        .maxBy { (state.myCards - it).getHandValue() }
    println("Put down $putDownCard")
    state.myCards -= putDownCard
    state.centreCards += putDownCard
}

fun oppMove(state: GameState) {
    println("\n--- OPPONENT ---")
    println("Opponent's cards: ${state.oppCards}")
    val pickedUp = input("Where did opponent pick up from (F or R)? ")
    if (pickedUp == "F") {
        state.oppCards += state.centreCards.pop()
    }
    val putDown = Card(input("What did opponent put down? "))
    state.centreCards += putDown
    state.oppCards -= putDown
}