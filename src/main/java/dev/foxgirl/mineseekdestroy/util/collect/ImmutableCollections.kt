package dev.foxgirl.mineseekdestroy.util.collect

fun <T> immutableListOf(): List<T> = ImmutableList.of()
fun <T> immutableListOf(vararg elements: T): List<T> = ImmutableList.copyOf(elements)

fun <T> immutableSetOf(): Set<T> = ImmutableSet.of()
fun <T> immutableSetOf(vararg elements: T): Set<T> = ImmutableSet.copyOf(elements)

fun <K, V> immutableMapOf(): Map<K, V> = ImmutableMap.of()
fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): Map<K, V> {
    val builder = ImmutableMap.builder<K, V>(pairs.size)
    for ((key, value) in pairs) {
        builder.put(key, value)
    }
    return builder.build()
}

fun <T> Iterable<T>.toImmutableList(): List<T> {
    val collection = if (this is Collection<T>) this else toCollection(mutableListOf())
    return ImmutableList.of(collection)
}
fun <T> Iterable<T>.toImmutableSet(): Set<T> {
    val collection = if (this is Collection<T>) this else toCollection(mutableListOf())
    return ImmutableSet.of(collection)
}

fun <K, V> Map<K, V>.toImmutableList(): List<Pair<K, V>> {
    if (isEmpty()) {
        return immutableListOf()
    }
    val builder = ImmutableList.builder<Pair<K, V>>(size)
    for (entry in this) {
        builder.add(entry.toPair())
    }
    return builder.build()
}
fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> {
    return ImmutableMap.of(this)
}