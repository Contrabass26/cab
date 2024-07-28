const val DEPTH = 1

fun main() {
    val state = FullGameState(null, GameStage.ME_DOWN, toCardsSet("3S", "8C", "9D", "AD"), toCardsSet("JS", "6C", "KD"), toCards("3C", "3H"))
    println(state.getBestMove(DEPTH))
}

fun input(prompt: String): String {
    print(prompt)
    return readln()
}