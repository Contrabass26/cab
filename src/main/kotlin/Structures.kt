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

enum class GameStage { ME_UP, ME_DOWN, OPP_UP, OPP_DOWN }

class Deck(vararg cards: Card): Iterable<Card> {

    companion object {
        fun none() = Deck(*arrayOf<Card>())

        fun all(): Deck {
            val deck = none()
            for (index in 0..51)
                deck.cards.add(Card(index))
            return deck
        }
    }

    constructor(vararg cards: String) : this(*cards.map { Card(it) }.toTypedArray())

    private val cards: MutableList<Card> = cards.toMutableList()

    operator fun plusAssign(other: Card) {
        cards.add(0, other)
    }

    operator fun plusAssign(others: Iterable<Card>) {
        cards.addAll(others)
    }

    operator fun minusAssign(other: Card) {
        cards.remove(other)
    }

    operator fun minusAssign(others: Iterable<Card>) {
        cards.removeAll(others.toSet())
    }

    operator fun plus(other: Card): Deck {
        val new = Deck.none()
        new.cards.addAll(this.cards)
        new.cards.add(0, other)
        return new
    }

    operator fun plus(others: Iterable<Card>): Deck {
        val new = Deck.none()
        new.cards.addAll(this.cards)
        new.cards.addAll(others)
        return new
    }

    operator fun minus(other: Card): Deck {
        val new = Deck.none()
        new.cards.addAll(this.cards)
        new.cards.remove(other)
        return new
    }

    operator fun minus(others: Iterable<Card>): Deck {
        val new = Deck.none()
        new.cards.addAll(this.cards)
        new.cards.removeAll(others.toSet())
        return new
    }

    override fun iterator() = cards.iterator()

    operator fun get(index: Int) = cards[index]

    fun pop(): Card {
        return cards.removeFirst()
    }

    fun peek(): Card {
        return cards.first()
    }

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

    fun isUnique(): Boolean {
        return cards.size == cards.toSet().size
    }
}

data class GameState(val knocked: Boolean, val stage: GameStage, val myCards: Deck, val oppCards: Deck, val centreCards: Deck) {

    fun getPickupDeck() =
        Deck.all() - myCards - oppCards - centreCards.toSet()

    fun getBestMove(depth: Int) {
        if (myCards.count() == 4) {
            // Need to put one down
            for (meCardDown in myCards) {
                // We're looking to assign some kind of score to each meCardDown
                var score = 0
                // Suppose we put this one down
                val state1 = this.copy( // The hypothetical future state
                    myCards = myCards - meCardDown,
                    centreCards = centreCards + meCardDown
                )
                // Now the opponent has to pick one up
                // What's the chance that they take the face-up card?
                val blindCards = state1.getPickupDeck()
                val faceUpCard = state1.centreCards.peek()
                val faceUpValue = (state1.myCards + faceUpCard).getHandValue()
                // We assume that P(opponent takes blind) = P(blind > faceUp)
                val blindChance = blindCards.asSequence()
                    .map { state1.myCards + it }
                    .map { it.getHandValue() }
                    .map { it - faceUpValue }
                    .count { it >= 0 } / state1.centreCards.count().toDouble()
                // Suppose the opponent takes the face-up card
                val state2 = this.copy(
                    oppCards = oppCards + faceUpCard,
                    centreCards = centreCards - faceUpCard
                )

            }
        }
    }

    fun branch(): Map<GameState, Double?> {
        return when (stage) {
            GameStage.ME_UP -> {
                // I need to pick up a card
                val cards = getPickupDeck()
                return cards.map {
                    this.copy(
                        myCards = myCards + it,
                    )
                }.plus(
                    this.copy(
                        myCards = myCards + centreCards.peek(),
                        centreCards = centreCards - centreCards.peek()
                    )
                ).associateWith { null }
            }
            GameStage.ME_DOWN -> {
                // I need to put down a card
                return myCards.map {
                    this.copy(
                        myCards = myCards - it,
                        centreCards = centreCards + it
                    )
                }.associateWith { null }
            }
            GameStage.OPP_UP -> {
                // Opponent needs to pick up a card
                val cards = getPickupDeck()
                return cards.map {
                    this.copy(
                        oppCards = oppCards + it,
                    )
                }.plus(
                    this.copy(
                        oppCards = oppCards + centreCards.peek(),
                        centreCards = centreCards - centreCards.peek()
                    )
                ).associateWith { null }
            }
            GameStage.OPP_DOWN -> {
                // What decks could the opponent have
                val unknownCount = 4 - oppCards.count()
                val decks = (0..<unknownCount).map { getPickupDeck() }.toTypedArray()
                val oppDecks = nestedLoop(*decks).map { oppCards + it }.toList()
                val deckProbability = 1.0 / oppDecks.size
                val maps = oppDecks.map { hand ->
                    // For each card that the opponent could put down
                    val handValues = hand.associateWith { (hand - it).getHandValue() }
                    val total = handValues.values.sum()
                    return@map hand.associateWith { deckProbability * handValues[it]!! / total.toDouble() }
                }
                return maps.foldRight(mapOf(), )
            }
        }
    }
}

fun <T> nestedLoop(vararg elements: Iterable<T>) = sequence {
    val counts = elements.map { 0 }.toTypedArray()
    val maxima = elements.map { it.count() - 1 }
    outer@ while (maxima.zip(counts, Int::minus).none { it < 0 }) {
        val args = counts.mapIndexed { i, count -> elements[i].elementAt(count) }
        yield(args)
        // Increment counter
        for (i in counts.size - 1 downTo 0) {
            counts[i]++
            if (counts[i] > maxima[i]) {
                if (i == 0) break@outer
                counts[i] = 0
            } else break
        }
    }
}