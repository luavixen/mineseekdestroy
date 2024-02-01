package dev.foxgirl.mineseekdestroy.util

import com.mojang.brigadier.StringReader
import net.minecraft.block.BlockState
import net.minecraft.fluid.FluidState
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.*
import net.minecraft.nbt.visitor.StringNbtWriter
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*

private fun createNbtList(capacity: Int, type: Byte = NbtElement.END_TYPE) = NbtList(ArrayList(capacity.coerceAtLeast(4)), type)
private inline fun createNbtList(capacity: Int, type: Byte = NbtElement.END_TYPE, block: (NbtList) -> Unit) = createNbtList(capacity, type).also(block)
private fun createNbtCompound(capacity: Int) = NbtCompound(HashMap(capacity.coerceAtLeast(4), 1.0F))
private inline fun createNbtCompound(capacity: Int, block: (NbtCompound) -> Unit) = createNbtCompound(capacity).also(block)

fun nbtList(capacity: Int, type: Byte) = createNbtList(capacity, type)
fun nbtList(capacity: Int) = createNbtList(capacity)
fun nbtList() = createNbtList(8)

fun nbtCompound(capacity: Int) = createNbtCompound(capacity)
fun nbtCompound() = createNbtCompound(8)

fun toNbtList(source: Collection<*>): NbtList =
    createNbtList(source.size) { source.mapTo(it, ::toNbtElement) }
fun toNbtCompound(source: Map<*, *>): NbtCompound =
    createNbtCompound(source.size) { source.forEach { (key, value) -> it[key as String] = toNbtElement(value) } }

fun toNbt(value: NbtElement) = value

fun toNbt(value: Byte): NbtByte = NbtByte.of(value)
fun toNbt(value: ByteArray): NbtByteArray = NbtByteArray(value)
fun toNbt(value: Short): NbtShort= NbtShort.of(value)
fun toNbt(value: Int): NbtInt = NbtInt.of(value)
fun toNbt(value: IntArray): NbtIntArray = NbtIntArray(value)
fun toNbt(value: Long): NbtLong = NbtLong.of(value)
fun toNbt(value: LongArray): NbtLongArray = NbtLongArray(value)
fun toNbt(value: Float): NbtFloat = NbtFloat.of(value)
fun toNbt(value: Double): NbtDouble = NbtDouble.of(value)
fun toNbt(value: String): NbtString = NbtString.of(value)

fun toNbt(value: Boolean): NbtByte = NbtByte.of(value)
fun toNbt(value: BooleanArray): NbtByteArray =
    NbtByteArray(ByteArray(value.size).also { value.forEachIndexed { i, b -> it[i] = if (b) 1 else 0 } })

fun toNbt(value: UUID): NbtIntArray = NbtHelper.fromUuid(value)
fun toNbt(value: BlockPos): NbtCompound = NbtHelper.fromBlockPos(value)
fun toNbt(value: BlockState): NbtCompound = NbtHelper.fromBlockState(value)
fun toNbt(value: FluidState): NbtCompound = NbtHelper.fromFluidState(value)

fun toNbt(value: Text) = toNbt(Text.Serialization.toJsonString(value))

fun toNbt(value: Identifier) = toNbt(value.toString())
fun toNbt(value: Item) = toNbt(Registries.ITEM.getId(value))

fun toNbt(value: ItemStack): NbtCompound = nbtCompound(4).also(value::writeNbt)
fun toNbt(value: Inventory): NbtCompound = Inventories.toNbt(value)

fun toNbt(value: Enum<*>) = toNbt(value.name)

fun toNbt(value: Collection<*>) = toNbtList(value)
fun toNbt(value: Map<*, *>) = toNbtCompound(value)

private fun toNbtElementConversion(value: Any?): NbtElement {
    if (value == null) {
        throw NullPointerException("Cannot convert null to NbtElement")
    }
    return when (value) {
        is Byte -> toNbt(value)
        is ByteArray -> toNbt(value)
        is Short -> toNbt(value)
        is Int -> toNbt(value)
        is IntArray -> toNbt(value)
        is Long -> toNbt(value)
        is LongArray -> toNbt(value)
        is Float -> toNbt(value)
        is Double -> toNbt(value)
        is String -> toNbt(value)
        is Boolean -> toNbt(value)
        is BooleanArray -> toNbt(value)
        is UUID -> toNbt(value)
        is BlockPos -> toNbt(value)
        is BlockState -> toNbt(value)
        is FluidState -> toNbt(value)
        is Text -> toNbt(value)
        is Identifier -> toNbt(value)
        is Item -> toNbt(value)
        is ItemStack -> toNbt(value)
        is Inventory -> toNbt(value)
        is Enum<*> -> toNbt(value)
        is Collection<*> -> toNbtList(value)
        is Map<*, *> -> toNbtCompound(value)
        else -> {
            val clazz = value::class.java
            val handle = Reflector.methodHandle(clazz, "toNbt")
            if (handle == null) {
                throw IllegalArgumentException("Cannot convert class ${clazz.simpleName} to NbtElement, missing toNbt method")
            } else {
                return toNbtElement(handle.invoke(value))
            }
        }
    }
}
fun toNbtElement(value: Any?): NbtElement {
    return if (value is NbtElement) value else toNbtElementConversion(value)
}

fun nbtListOf() = nbtList()
fun nbtListOf(vararg values: Any?) =
    createNbtList(values.size) { values.mapTo(it, ::toNbtElement) }

fun nbtCompoundOf() = nbtCompound()
fun nbtCompoundOf(vararg pairs: Pair<String, Any?>) =
    createNbtCompound(pairs.size) { pairs.forEach { (key, value) -> it[key] = toNbtElement(value) } }

fun NbtElement?.asList() = this as NbtList
fun NbtElement?.asCompound() = this as NbtCompound

fun NbtElement?.toByte(): Byte =
    (this as NbtByte).byteValue()
fun NbtElement?.toByteArray(): ByteArray =
    (this as NbtByteArray).byteArray.clone()
fun NbtElement?.toShort(): Short =
    (this as NbtShort).shortValue()
fun NbtElement?.toInt(): Int =
    (this as NbtInt).intValue()
fun NbtElement?.toIntArray(): IntArray =
    (this as NbtIntArray).intArray.clone()
fun NbtElement?.toLong(): Long =
    (this as NbtLong).longValue()
fun NbtElement?.toLongArray(): LongArray =
    (this as NbtLongArray).longArray.clone()
fun NbtElement?.toFloat(): Float =
    (this as NbtFloat).floatValue()
fun NbtElement?.toDouble(): Double =
    (this as NbtDouble).doubleValue()

fun NbtElement?.toBoolean(): Boolean =
    this.toByte().toInt() != 0
fun NbtElement?.toBooleanArray(): BooleanArray =
    this.toByteArray().let { bytes -> BooleanArray(bytes.size).also { bytes.forEachIndexed { i, b -> it[i] = b.toInt() != 0 } } }

fun NbtElement?.toActualString(): String =
    (this as NbtString).asString()

fun NbtElement?.toUUID(): UUID =
    NbtHelper.toUuid(this!!)
fun NbtElement?.toBlockPos(): BlockPos =
    NbtHelper.toBlockPos(this.asCompound())

inline fun <reified E : Enum<E>> NbtElement?.toEnum(): E =
    Reflector.methodHandle(E::class.java, "valueOf", String::class.java)!!.invoke(this.toActualString()) as E

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

operator fun NbtCompound.plus(other: NbtCompound): NbtCompound {
    this.copyFrom(other)
    return this
}

fun identifier(id: String) = Identifier(id)
fun identifier(namespace: String, path: String) = Identifier(namespace, path)
