fun main() {
    val state = GameState(false, GameStage.ME_UP, Deck("4D", "6D", "5D"), Deck.none(), Deck("2S", "5S"))
    val newState = state.getBestMove()
    println(newState)
}

fun initGame(stage: GameStage): GameState {
    val myCards = Deck.none()
    val handSize = if (stage == GameStage.ME_DOWN) 4 else 3
    for (i in 1..handSize) {
        myCards += Card(input("Enter card $i: "))
    }
    return GameState(false, stage, myCards, Deck.none(), Deck.none())
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