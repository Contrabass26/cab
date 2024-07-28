fun <T> nestedLoop(vararg elements: Iterable<T>) = sequence {
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

inline fun <reified T> repeatedArray(element: T, length: Int): Array<T> {
    return sequence {
        for (i in 1..length) {
            this.yield(element)
        }
    }.toList().toTypedArray()
}

fun standardise(nums: List<Double>, allocation: Double): List<Double> {
    // Translate to zero
    val min = nums.min()
    val translated = nums.map { it - min }
    // Convert to probability
    val total = translated.sum()
    return translated.map { it / total * allocation }
}

fun <K, V> List<Pair<K, V>>.toMap(): Map<K, V> =
    fold(mutableMapOf()) { map, pair ->
        map[pair.first] = pair.second
        map
    }

fun <T> Iterable<Iterable<T>>.foldToList() =
    fold(mutableListOf<T>()) { list, additive ->
        list.addAll(additive)
        list
    }