package dev.foxgirl.mineseekdestroy.util

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text

inline fun stackOf(): ItemStack = ItemStack.EMPTY

inline fun stackOf(stack: ItemStack, count: Int): ItemStack = stack.copyWithCount(count)
inline fun stackOf(stack: ItemStack): ItemStack = stack.copy()

fun stackOf(item: ItemConvertible, count: Int) = ItemStack(item, count)
fun stackOf(item: ItemConvertible) = stackOf(item, 1)

inline fun stackOf(item: ItemConvertible, block: (ItemStack) -> Unit) =
    stackOf(item).also(block)
inline fun stackOf(item: ItemConvertible, count: Int, block: (ItemStack) -> Unit) =
    stackOf(item, count).also(block)

fun stackOf(item: ItemConvertible, nbt: NbtCompound) =
    stackOf(item) { it.nbt = nbt }
fun stackOf(item: ItemConvertible, count: Int, nbt: NbtCompound?) =
    stackOf(item, count) { it.nbt = nbt }

inline fun stackOf(item: ItemConvertible, nbt: NbtCompound, block: (ItemStack) -> Unit) =
    stackOf(item, nbt).also(block)
inline fun stackOf(item: ItemConvertible, count: Int, nbt: NbtCompound, block: (ItemStack) -> Unit) =
    stackOf(item, count, nbt).also(block)

private fun stackOfDisplay(item: ItemConvertible, count: Int, nbt: NbtCompound?, name: Text?, lore: Collection<Text>?) =
    stackOf(item, count) {
        if (nbt != null) {
            it.nbt = nbt
        }
        if (name != null) {
            it.dataDisplay()["Name"] = toNbtElement(name.copy().itemName())
        }
        if (lore != null && !lore.isEmpty()) {
            it.dataDisplay()["Lore"] = toNbtList(lore.map { lore -> lore.copy().itemLore() })
        }
    }

fun stackOf(item: ItemConvertible, count: Int, nbt: NbtCompound, name: Text?, lore: Collection<Text>?) =
    stackOfDisplay(item, count, nbt, name, lore)
fun stackOf(item: ItemConvertible, count: Int, name: Text?, lore: Collection<Text>?) =
    stackOfDisplay(item, count, null, name, lore)
fun stackOf(item: ItemConvertible, nbt: NbtCompound, name: Text?, lore: Collection<Text>?) =
    stackOfDisplay(item, 1, nbt, name, lore)
fun stackOf(item: ItemConvertible, name: Text?, lore: Collection<Text>?) =
    stackOfDisplay(item, 1, null, name, lore)

fun stackOf(item: ItemConvertible, count: Int, nbt: NbtCompound, name: Text?, vararg lore: Text) =
    stackOfDisplay(item, count, nbt, name, lore.asList())
fun stackOf(item: ItemConvertible, count: Int, name: Text?, vararg lore: Text) =
    stackOf(item, count, name, lore.asList())
fun stackOf(item: ItemConvertible, nbt: NbtCompound, name: Text?, vararg lore: Text) =
    stackOf(item, nbt, name, lore.asList())
fun stackOf(item: ItemConvertible, name: Text?, vararg lore: Text) =
    stackOf(item, name, lore.asList())

fun ItemStack.data(): NbtCompound = this.getOrCreateNbt()
fun ItemStack.dataDisplay(): NbtCompound {
    val nbt = this.data()
    val nbtDisplay = nbt["display"]
    if (nbtDisplay != null) {
        return nbtDisplay.asCompound()
    } else {
        return nbtCompoundOf().also { nbt["display"] = it }
    }
}

fun Inventory.asList(): List<ItemStack> = Inventories.list(this)

fun Item.toNbt() = toNbt(this)
fun ItemStack.toNbt() = toNbt(this)
fun Inventory.toNbt() = toNbt(this)

fun PlayerEntity.give(stack: ItemStack) = this.give(stack, true)
fun PlayerEntity.give(stack: ItemStack, drop: Boolean): Boolean {
    if (stack.isEmpty) return true
    if (giveItemStack(stack)) return true
    if (drop) {
        val entity = dropItem(stack, false)
        if (entity != null) {
            entity.resetPickupDelay()
            entity.setOwner(uuid)
            return true
        }
    }
    return false
}
