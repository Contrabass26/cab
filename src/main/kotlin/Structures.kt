import java.util.function.Predicate

const val SUITS = "HCDS"
const val VALUES = "A23456789TJQK"

data class Card(val suit: Int, val value: Int) {

    constructor(string: String) : this(
        SUITS.indexOf(string[1]),
        VALUES.indexOf(string[0])
    )

    val index = suit * 13 + value // 0-51

    val handValue = when (value) {
        0 -> 11
        in 1..9 -> value + 1
        in 10..12 -> 10
        else -> throw IllegalStateException("Invalid card value $value")
    }

    constructor(index: Int): this(
        index.floorDiv(13),
        index.mod(13)
    )

    override fun toString(): String {
        return "${VALUES[value]}${SUITS[suit]}"
    }
}

class Deck(private var predicate: Predicate<Card>) : Iterable<Card> {

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

    operator fun plusAssign(other: Card) { predicate = predicate.or { it == other } }

    operator fun plusAssign(other: Iterable<Card>) { predicate = predicate.or { other.contains(it) } }

    operator fun minusAssign(other: Card) { predicate = predicate.and { it != other } }

    operator fun minusAssign(other: Iterable<Card>) { predicate = predicate.and { !other.contains(it) } }

    operator fun contains(card: Card) = predicate.test(card)

    fun getHandValue(): Int {
        // Check for 30
        if ((0..12)
                .asSequence()
                .map { value ->
                    asSequence()
                        .filter { it.value == value }
                        .count()
                }
                .firstOrNull { it >= 3 } != null
        ) return 30
        return (0..3)
            .asSequence()
            .map { suit ->
                this.asSequence()
                    .filter { it.suit == suit }
                    .map { it.handValue }
                    .sum()
            }
            .max()
    }

    override fun iterator() = (0..51)
        .asSequence()
        .map { Card(it) }
        .filter { predicate.test(it) }
        .iterator()

    override fun toString() = joinToString(" ")
}

class OrderedDeck(vararg cards: Card): Iterable<Card> {

    companion object {
        fun none() = OrderedDeck(*arrayOf<Card>())
    }

    constructor(vararg cards: String) : this(*cards.map { Card(it) }.toTypedArray())

    private val cards: MutableList<Card> = cards.toMutableList()

    operator fun plusAssign(other: Card) {
        cards.add(0, other)
    }

    operator fun minusAssign(other: Card) {
        cards.remove(other)
    }

    override fun iterator() = cards.iterator()

    operator fun get(index: Int) = cards[index]

    fun pop(): Card {
        return cards.removeFirst()
    }
}

data class GameState(val knocked: Boolean, val myCards: Deck, val oppCards: Deck, val centreCards: OrderedDeck) {

    fun getPickupDeck() =
        Deck.all() - myCards - oppCards - centreCards.toSet()
}

class GameStateNode private constructor(private val getState: () -> GameState) {

    private val children = mutableSetOf<GameStateNode>()

    constructor(value: GameState) : this({ value })

    constructor(parent: GameStateNode, changes: ((GameState) -> Unit)) : this({ parent.getState().apply(changes) })

    fun addChild(changes: (GameState) -> Unit) {
        children.add(GameStateNode(this, changes))
    }
}