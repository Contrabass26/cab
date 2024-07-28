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