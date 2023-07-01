package dev.foxgirl.mineseekdestroy.util

import com.mojang.brigadier.StringReader
import net.minecraft.block.BlockState
import net.minecraft.fluid.FluidState
import net.minecraft.nbt.*
import net.minecraft.nbt.visitor.StringNbtWriter
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*

private inline fun createNbtList(size: Int, block: (NbtList) -> Unit) =
    NbtList(ArrayList(size), NbtElement.END_TYPE).also(block)

private inline fun createNbtCompound(size: Int, block: (HashMap<String, NbtElement>) -> Unit) =
    NbtCompound(HashMap<String, NbtElement>(size.let { it + (it shr 1) }).apply(block))

fun toNbtList(source: Collection<*>): NbtList =
    createNbtList(source.size) { source.mapTo(it, ::toNbtElement) }

fun toNbtCompound(source: Map<*, *>): NbtCompound =
    createNbtCompound(source.size) { source.forEach { (key, value) -> it[(key as CharSequence).toString()] = toNbtElement(value) } }

fun toNbtElement(value: Any?): NbtElement =
    when (value) {
        null -> throw NullPointerException("Cannot convert null to NbtElement")

        is NbtElement -> value

        is Byte -> NbtByte.of(value)
        is ByteArray -> NbtByteArray(value)
        is Short -> NbtShort.of(value)
        is Int -> NbtInt.of(value)
        is IntArray -> NbtIntArray(value)
        is Long -> NbtLong.of(value)
        is LongArray -> NbtLongArray(value)
        is Float -> NbtFloat.of(value)
        is Double -> NbtDouble.of(value)
        is String -> NbtString.of(value)

        is Boolean -> NbtByte.of(value)
        is BooleanArray -> NbtByteArray(
            ByteArray(value.size).also { value.forEachIndexed { i, b -> it[i] = if (b) 1 else 0 } }
        )

        is UUID -> NbtHelper.fromUuid(value)
        is BlockPos -> NbtHelper.fromBlockPos(value)
        is BlockState -> NbtHelper.fromBlockState(value)
        is FluidState -> NbtHelper.fromFluidState(value)

        is Identifier -> NbtString.of(value.toString())
        is Text -> NbtString.of(Text.Serializer.toJson(value))

        is Collection<*> -> toNbtList(value)
        is Map<*, *> -> toNbtCompound(value)

        else -> throw IllegalArgumentException("Cannot convert class ${value::class.simpleName} to NbtElement")
    }

fun nbtListOf() = NbtList()
fun nbtListOf(vararg values: Any?) =
    createNbtList(values.size) { values.mapTo(it, ::toNbtElement) }

fun nbtCompoundOf() = NbtCompound()
fun nbtCompoundOf(vararg pairs: Pair<String, Any?>) =
    createNbtCompound(pairs.size) { pairs.forEach { (key, value) -> it[key] = toNbtElement(value) } }

fun NbtElement.asList() = this as NbtList
fun NbtElement.asCompound() = this as NbtCompound

fun NbtElement.toByte(): Byte =
    (this as NbtByte).byteValue()
fun NbtElement.toByteArray(): ByteArray =
    (this as NbtByteArray).byteArray.clone()
fun NbtElement.toShort(): Short =
    (this as NbtShort).shortValue()
fun NbtElement.toInt(): Int =
    (this as NbtInt).intValue()
fun NbtElement.toIntArray(): IntArray =
    (this as NbtIntArray).intArray.clone()
fun NbtElement.toLong(): Long =
    (this as NbtLong).longValue()
fun NbtElement.toLongArray(): LongArray =
    (this as NbtLongArray).longArray.clone()
fun NbtElement.toFloat(): Float =
    (this as NbtFloat).floatValue()
fun NbtElement.toDouble(): Double =
    (this as NbtDouble).doubleValue()

fun NbtElement.toUUID(): UUID =
    NbtHelper.toUuid(this)
fun NbtElement.toBlockPos(): BlockPos =
    NbtHelper.toBlockPos(this.asCompound())

fun nbtDecode(string: String): NbtElement =
    StringNbtReader(StringReader(string)).parseElement()
fun nbtEncode(element: NbtElement): String =
    StringNbtWriter().apply(element)

fun NbtElement.encode(): String = nbtEncode(this)

operator fun NbtCompound.set(key: String, value: NbtElement?) {
    this.put(key, value)
}

operator fun NbtCompound.set(key: String, value: Byte) {
    this.putByte(key, value)
}
operator fun NbtCompound.set(key: String, value: Short) {
    this.putShort(key, value)
}
operator fun NbtCompound.set(key: String, value: Int) {
    this.putInt(key, value)
}
operator fun NbtCompound.set(key: String, value: Long) {
    this.putLong(key, value)
}
operator fun NbtCompound.set(key: String, value: Float) {
    this.putFloat(key, value)
}
operator fun NbtCompound.set(key: String, value: Double) {
    this.putDouble(key, value)
}
operator fun NbtCompound.set(key: String, value: Boolean) {
    this.putBoolean(key, value)
}
operator fun NbtCompound.set(key: String, value: String) {
    this.putString(key, value)
}
operator fun NbtCompound.set(key: String, value: UUID) {
    this.putUuid(key, value)
}
operator fun NbtCompound.set(key: String, value: ByteArray) {
    this.putByteArray(key, value)
}
operator fun NbtCompound.set(key: String, value: IntArray) {
    this.putIntArray(key, value)
}
operator fun NbtCompound.set(key: String, value: LongArray) {
    this.putLongArray(key, value)
}

fun identifier(id: String) = Identifier(id)
fun identifier(namespace: String, path: String) = Identifier(namespace, path)
