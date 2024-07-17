import java.util.function.Predicate

const val SUITS = "HCDS"
const val VALUES = "A23456789TJQK"

data class Card(val suit: Int, val value: Int) {

    init {
        println("Creating $suit-$value")
    }

    constructor(string: String) : this(
        SUITS.indexOf(string[1]),
        VALUES.indexOf(string[0]) + 1
    )

    val index = suit * 13 + value // 1-52

    constructor(index: Int): this(
        (index - 1).floorDiv(13),
        (index - 1).mod(13)
    )

    override fun toString(): String {
        println("$suit-$value")
        return "${SUITS[suit]}${VALUES[value - 1]}"
    }
}

class Deck(private val predicate: Predicate<Card>) : Iterable<Card> {

    companion object {
        fun all() = Deck { true }

        fun none() = Deck { false }
    }

    constructor(vararg cards: Card) : this({ cards.contains(it) })

    constructor(vararg cards: String) : this({ cards.contains(it.toString()) })

    operator fun not() = Deck(predicate.negate())

    operator fun plus(other: Card) = Deck(predicate.or { it == other })

    operator fun plus(other: Iterable<Card>) = Deck(predicate.or { other.contains(it) })

    operator fun minus(other: Card) = Deck(predicate.and { it != other })

    operator fun minus(other: Iterable<Card>) = Deck(predicate.and { !other.contains(it) })

    override fun iterator() = (1..52)
        .asSequence()
        .map { Card(it) }
        .filter { predicate.test(it) }
        .iterator()

    fun draw() = toList().random()
}

class OrderedDeck(vararg cards: Card): Iterable<Card> {

    constructor(vararg cards: String) : this(*cards.map { Card(it) }.toTypedArray())

    private val cards: MutableList<Card> = cards.toMutableList()

    operator fun plus(other: Card) {
        cards.add(0, other)
    }

    override fun iterator() = cards.iterator()

    operator fun get(index: Int) = cards[index]
}

data class GameState(val knocked: Boolean, val myCards: Deck, val oppCards: Deck, val centreCards: OrderedDeck) {

    val pickupDeck by lazy {
        Deck.all() - myCards - oppCards - centreCards.toSet()
    }
}

class GameStateNode private constructor(private val getState: () -> GameState) {

    private val children = mutableSetOf<GameStateNode>()

    constructor(value: GameState) : this({ value })

    constructor(parent: GameStateNode, changes: ((GameState) -> Unit)) : this({ parent.getState().apply(changes) })

    fun addChild(changes: (GameState) -> Unit) {
        children.add(GameStateNode(this, changes))
    }
}