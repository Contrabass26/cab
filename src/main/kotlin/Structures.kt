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

enum class GameStage(private val index: Int) {
    ME_UP(0), ME_DOWN(1), OPP_UP(2), OPP_DOWN(3);

    operator fun next() = entries[index + 1]
}

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

    override fun toString() = cards.toString()
}

data class GameState(val meKnocked: Boolean?, val stage: GameStage, val myCards: Deck, val oppCards: Deck, val centreCards: Deck) {

    fun getPickupDeck() =
        Deck.all() - myCards - oppCards - centreCards.toSet()

    fun getWinProbability(depth: Int): Double {
        val result = if (depth == 0)
            if (myCards.getHandValue() > oppCards.getHandValue()) 1.0 else 0.0
        else {
            val branches = branch()
            branches.map { it.key.getWinProbability(depth - 1) * it.value }.sum()
        }
//        print("\t".repeat(2 - depth))
//        println("$this = $result")
        return result
    }

    fun getBestMove(): Action {
        val probabilities = branch().keys.associateWith { (this + it).getWinProbability(2) }
        return probabilities.maxBy { it.value }.key
    }

    fun branch(): Map<Action, Double> = when (stage) {
        GameStage.ME_UP -> {
            // I need to pick up a card
            val cards = getPickupDeck()
            val states = cards.map {
                this.copy(
                    stage = GameStage.ME_DOWN,
                    myCards = myCards + it,
                )
            }.plus(
                this.copy(
                    stage = GameStage.ME_DOWN,
                    myCards = myCards + centreCards.peek(),
                    centreCards = centreCards - centreCards.peek()
                )
            )
            states.associateWith { 1.0 / states.size }
        }

        GameStage.ME_DOWN -> {
            // I need to put down a card
            val states = myCards.map {
                this.copy(
                    stage = GameStage.OPP_UP,
                    myCards = myCards - it,
                    centreCards = centreCards + it
                )
            }
            states.associateWith { 1.0 / states.size }
        }

        GameStage.OPP_UP -> {
            // Opponent needs to pick up a card
            val cards = getPickupDeck()
            val states = cards.map {
                this.copy(
                    stage = GameStage.OPP_DOWN,
                    oppCards = oppCards + it,
                )
            }.plus(
                this.copy(
                    stage = GameStage.OPP_DOWN,
                    oppCards = oppCards + centreCards.peek(),
                    centreCards = centreCards - centreCards.peek()
                )
            )
            states.associateWith { 1.0 / states.size }
        }

        GameStage.OPP_DOWN -> {
            // What decks could the opponent have
            val unknownCount = 4 - oppCards.count()
            val oppDecks = if (unknownCount == 0) listOf(oppCards) else {
                val decks = (0..<unknownCount).map { getPickupDeck() }.toTypedArray()
                nestedLoop(*decks).map { oppCards + it }.toList()
            }
            val deckProbability = 1.0 / oppDecks.size
            val maps = oppDecks.map { hand ->
                // For each card that the opponent could put down
                val handValues = hand.associateWith { (hand - it).getHandValue() }
                val total = handValues.values.sum()
                return@map hand.associateBy({
                    this.copy(
                        stage = GameStage.ME_UP,
                        oppCards = oppCards - it,
                        centreCards = centreCards + it
                    )
                }) { deckProbability * handValues[it]!! / total.toDouble() }
            }
            maps.foldRight(mapOf()) { a, b -> a + b }
        }
    }

    override fun toString(): String {
        return "GameState(knocked=$meKnocked, stage=$stage, myCards=$myCards, oppCards=$oppCards, centreCards=$centreCards)"
    }

    operator fun plus(action: Action) = action(this)
}

fun <T> nestedLoop(vararg elements: Iterable<T>) = sequence {
//    println("New nested loop")
    if (elements.isEmpty()) return@sequence
    val counts = elements.map { 0 }.toTypedArray()
    val maxima = elements.map { it.count() - 1 }
    outer@ while (maxima.zip(counts, Int::minus).none { it < 0 }) {
        val args = counts.mapIndexed { i, count -> elements[i].elementAt(count) }
//        println(args)
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

abstract class Action {

    companion object {

        fun playCard() = object : Action() {
            override fun invoke(state: GameState) = with(state) {
                val card = Card(input("Which card was played? "))
                when (stage) {
                    GameStage.OPP_DOWN -> copy(stage = stage.next(), oppCards = oppCards - card, centreCards = centreCards + card)
                    GameStage.ME_DOWN -> copy(stage = stage.next(), myCards = myCards - card, centreCards = centreCards + card)
                    else -> throw IllegalStateException("Action is not allowed for game state: $this")
                }
            }

            operator fun invoke(state: GameState, card: Card) = with(state) {
                when (stage) {
                    GameStage.OPP_DOWN -> copy(stage = stage.next(), oppCards = oppCards - card, centreCards = centreCards + card)
                    GameStage.ME_DOWN -> copy(stage = stage.next(), myCards = myCards - card, centreCards = centreCards + card)
                    else -> throw IllegalStateException("Action is not allowed for game state: $this")
                }
            }

            override fun branch(state: GameState): Sequence<GameState> {
                if (state.stage == GameStage.ME_DOWN) {
                    return state.myCards.asSequence().map { this(state, it) }
                }
                if (state.stage == GameStage.OPP_DOWN) {

                }
            }
        }

        fun knock() = object : Action() {
            override fun invoke(state: GameState) = with(state) {
                assert(stage == GameStage.ME_UP || stage == GameStage.OPP_UP)
                copy(
                    stage = stage.next(),
                    meKnocked = stage == GameStage.ME_UP
                )
            }

            override fun branch(state: GameState) = sequenceOf(this(state))
        }

        fun pickBlind() = object : Action() {
            override fun invoke(state: GameState) = when (state.stage) {
                GameStage.ME_UP -> {
                    val card = Card(input("Which card did you pick up? "))
                    this(state, card)
                }

                GameStage.OPP_UP -> state.copy(stage = state.stage.next())
                else -> throw IllegalStateException("Action is not allowed for game state: $this")
            }

            private operator fun invoke(state: GameState, card: Card) = with(state) {
                when (stage) {
                    GameStage.ME_UP -> copy(stage = stage.next(), myCards = myCards + card)
                    GameStage.OPP_UP -> copy(stage = stage.next(), oppCards = oppCards + card)
                    else -> throw IllegalStateException("Action is not allowed for game state: $this")
                }
            }

            override fun branch(state: GameState) = state.getPickupDeck().asSequence().map { this(state, it) }
        }

        fun pickVisible() = object : Action() {
            override fun invoke(state: GameState) = with(state) {
                val card = centreCards.peek()
                when (stage) {
                    GameStage.ME_UP -> copy(stage = stage.next(), myCards = myCards + card, centreCards = centreCards - card)
                    GameStage.OPP_UP -> copy(stage = stage.next(), oppCards = oppCards + card, centreCards = centreCards - card)
                    else -> throw IllegalStateException("Action is not allowed for game state: $this")
                }
            }

            override fun branch(state: GameState) = sequenceOf(this(state))
        }
    }

    abstract operator fun invoke(state: GameState): GameState

    abstract fun branch(state: GameState): Sequence<GameState>
}