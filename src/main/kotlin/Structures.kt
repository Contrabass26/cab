import java.util.function.Predicate

data class Card(val suit: Int, val value: Int) {
    val index = suit * 13 + value // 1-52

    constructor(index: Int): this(
        (index - 1).floorDiv(13),
        (index - 1).mod(13)
    )
}

fun <T> Iterator<T>.size(): Int {
    var count = 0
    while (hasNext()) {
        next()
        count++
    }
    return count
}

data class Deck(private val predicate: Predicate<Card>) : Iterable<Card> {

    val size by lazy { iterator().size() }

    constructor(vararg cards: Card) : this({ cards.contains(it) })

    operator fun not() = Deck(predicate.negate())

    operator fun plus(other: Card) = Deck(predicate.or { it == other })

    operator fun plus(other: Deck) = Deck(predicate.or { other.contains(it) })

    override fun iterator() = (1..52)
        .asSequence()
        .map { Card(it) }
        .filter { predicate.test(it) }
        .iterator()

    fun draw(remove: Boolean = true): Card {

    }
}

data class GameState(val knocked: Boolean, val cards: Deck, val oppCards: Deck, val cardStack: Deck)

class GameStateNode private constructor(value: GameState?, parent: GameStateNode?, changes: ((GameState) -> Unit)?) {

    val getState: () -> GameState
    private val children = mutableSetOf<GameStateNode>()

    init {
        // Validation
        if ((parent == null) == (value == null)) throw IllegalArgumentException("Expected one or the other, got (${parent == null}, ${value == null})")
        if ((parent == null) != (changes == null)) throw IllegalArgumentException("Expected none or both, got (${parent == null}, ${changes == null})")
        // Set state getter
        getState = if (parent != null) {
            { parent.getState().apply(changes!!) }
        } else {
            { value!! }
        }
    }

    constructor(value: GameState) : this(value, null, null)

    constructor(parent: GameStateNode, changes: ((GameState) -> Unit)) : this(null, parent, changes)

    fun addChild(changes: (GameState) -> Unit) {
        children.add(GameStateNode(this, changes))
    }
}