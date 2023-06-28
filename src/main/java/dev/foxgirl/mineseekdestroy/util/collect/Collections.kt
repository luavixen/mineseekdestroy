package dev.foxgirl.mineseekdestroy.util.collect

import java.util.*

inline fun <reified E : Enum<E>> enumSetOf(): EnumSet<E> = EnumSet.noneOf(E::class.java)
inline fun <reified E : Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> = enumSetOf<E>().apply { elements.forEach { add(it) } }

inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> = EnumMap<K, V>(K::class.java)
inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V> = enumMapOf<K, V>().apply { putAll(pairs) }
