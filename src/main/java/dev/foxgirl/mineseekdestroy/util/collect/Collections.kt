package dev.foxgirl.mineseekdestroy.util.collect

import java.util.*

inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> = EnumMap<K, V>(K::class.java)
inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V> = enumMapOf<K, V>().apply { putAll(pairs) }
