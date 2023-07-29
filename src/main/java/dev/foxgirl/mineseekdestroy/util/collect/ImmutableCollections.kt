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

fun <T> buildImmutableList(action: ImmutableList.Builder<T>.() -> Unit): List<T> =
    ImmutableList.builder<T>().apply(action).build()
fun <T> buildImmutableList(capacity: Int, action: ImmutableList.Builder<T>.() -> Unit): List<T> =
    ImmutableList.builder<T>(capacity).apply(action).build()

fun <T> buildImmutableSet(action: ImmutableSet.Builder<T>.() -> Unit): Set<T> =
    ImmutableSet.builder<T>().apply(action).build()
fun <T> buildImmutableSet(capacity: Int, action: ImmutableSet.Builder<T>.() -> Unit): Set<T> =
    ImmutableSet.builder<T>(capacity).apply(action).build()

fun <K, V> buildImmutableMap(action: ImmutableMap.Builder<K, V>.() -> Unit): Map<K, V> =
    ImmutableMap.builder<K, V>().apply(action).build()
fun <K, V> buildImmutableMap(capacity: Int, action: ImmutableMap.Builder<K, V>.() -> Unit): Map<K, V> =
    ImmutableMap.builder<K, V>(capacity).apply(action).build()

fun <T> Iterable<T>.toImmutableList(): List<T> {
    return when (this) {
        is ImmutableList<T> -> this
        is Collection<T> -> ImmutableList.of(this)
        else -> ImmutableList.builder<T>().also { toCollection(it) }.build()
    }
}
fun <T> Iterable<T>.toImmutableSet(): Set<T> {
    return when (this) {
        is ImmutableSet<T> -> this
        is Collection<T> -> ImmutableSet.of(this)
        else -> ImmutableSet.builder<T>().also { toCollection(it) }.build()
    }
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
    return if (this is ImmutableMap<K, V>) this else ImmutableMap.of(this)
}
