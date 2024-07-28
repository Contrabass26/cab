fun main() {
    val state = PartialGameState(0, null, GameStage.ME_DOWN, toCardsSet("3H", "3S", "8C", "9D"), toCardsSet("JS", "6C", "3C"), toCards())
    println(state.getBestMove(2))
}

fun input(prompt: String): String {
    print(prompt)
    return readln()
}