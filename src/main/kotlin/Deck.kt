fun toCards(vararg cards: String) = cards.map { Card(it) }

fun toCardsSet(vararg cards: String) = toCards(*cards).toSet()

fun allCards() = (0..51).map { Card(it) }.toList()

fun Iterable<Card>.getHandValue(): Int {
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

fun Iterable<Card>.padFrom(length: Int, source: Iterable<Card>): List<List<Card>> {
    val cardsToAdd = length - count()
    return nestedLoop(*repeatedArray(source, cardsToAdd)).map { this + it }.toList()
}