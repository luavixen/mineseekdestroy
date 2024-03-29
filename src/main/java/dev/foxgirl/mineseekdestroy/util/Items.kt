package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.util.DynamicChestScreenHandler.ChestSlotProvider
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
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
            it.dataDisplay()["Name"] = toNbt(name.copy().mnsndItemName())
        }
        if (lore != null && !lore.isEmpty()) {
            it.dataDisplay()["Lore"] = toNbt(lore.map { lore -> toNbt(lore.copy().mnsndItemLore()) })
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

infix fun ItemStack?.contentEquals(other: ItemStack?): Boolean {
    return if (this == null || other == null) {
        this === other
    } else {
        ItemStack.areEqual(this, other)
    }
}
infix fun ItemStack?.contentNotEquals(other: ItemStack?): Boolean {
    return !(this contentEquals other)
}

fun Item.toNbt() = toNbt(this)
fun ItemStack.toNbt() = toNbt(this)
fun Inventory.toNbt() = toNbt(this)

fun Inventory.asList(): MutableList<ItemStack> = Inventories.list(this)

operator fun Inventory.get(index: Int): ItemStack = this.getStack(index)
operator fun Inventory.set(index: Int, stack: ItemStack): Unit = this.setStack(index, stack)

abstract class DynamicScreenHandlerFactory<T : ScreenHandler> : NamedScreenHandlerFactory {
    protected abstract val name: Text
    protected abstract fun construct(sync: Int, playerInventory: PlayerInventory): T

    final override fun getDisplayName(): Text = name
    final override fun createMenu(sync: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): T {
        require(playerEntity === playerInventory.player)
        return construct(sync, playerInventory)
    }
}

abstract class DynamicScreenHandler(
    type: ScreenHandlerType<*>,
    sync: Int,
    protected val playerInventory: PlayerInventory,
) : ScreenHandler(type, sync) {
    protected val playerEntity get() = playerInventory.player as ServerPlayerEntity

    protected abstract val inventory: Inventory

    protected abstract fun handleTakeResult(slot: OutputSlot, stack: ItemStack)
    protected abstract fun handleUpdateResult()
    protected abstract fun handleClosed()

    protected abstract inner class DynamicSlot(index: Int, x: Int, y: Int, target: Inventory = inventory)
        : Slot(target, index, x, y)
    {
        override fun canInsert(stack: ItemStack) = false
        override fun canTakeItems(playerEntity: PlayerEntity) = false
    }

    protected open inner class InputSlot(index: Int, x: Int, y: Int, target: Inventory = inventory)
        : DynamicSlot(index, x, y, target)
    {
        override fun canInsert(stack: ItemStack) = true
        override fun canTakeItems(playerEntity: PlayerEntity) = true
        override fun markDirty() {
            super.markDirty()
            handleUpdateResult()
        }
    }
    protected open inner class OutputSlot(index: Int, x: Int, y: Int, target: Inventory = inventory)
        : DynamicSlot(index, x, y, target)
    {
        override fun canInsert(stack: ItemStack) = false
        override fun canTakeItems(playerEntity: PlayerEntity) = true
        override fun onTakeItem(playerEntity: PlayerEntity, stack: ItemStack) {
            super.onTakeItem(playerEntity, stack)
            handleTakeResult(this, stack)
        }
    }

    protected open inner class StaticSlot(index: Int, x: Int, y: Int, target: Inventory = inventory)
        : DynamicSlot(index, x, y, target)
    {
        final override fun canInsert(stack: ItemStack) = false
        final override fun canTakeItems(playerEntity: PlayerEntity) = false
    }

    protected fun addPlayerInventorySlots() {
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addSlot(Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18))
            }
        }
        for (x in 0 until 9) {
            addSlot(Slot(playerInventory, x, 8 + x * 18, 142))
        }
    }

    final override fun onClosed(playerEntity: PlayerEntity) {
        handleClosed()
        playerEntity.give(cursorStack)
        cursorStack = stackOf()
    }

    private var ready = false

    override fun syncState() {
        if (!ready) { ready = true; handleUpdateResult() }
        super.syncState()
    }

    override fun quickMove(playerEntity: PlayerEntity, slotIndex: Int): ItemStack = stackOf()
    override fun canUse(playerEntity: PlayerEntity) = true
}

private fun screenHandlerTypeForChest(rows: Int): ScreenHandlerType<GenericContainerScreenHandler> {
    return when (rows) {
        1 -> ScreenHandlerType.GENERIC_9X1
        2 -> ScreenHandlerType.GENERIC_9X2
        3 -> ScreenHandlerType.GENERIC_9X3
        4 -> ScreenHandlerType.GENERIC_9X4
        5 -> ScreenHandlerType.GENERIC_9X5
        6 -> ScreenHandlerType.GENERIC_9X6
        else -> throw IllegalArgumentException("Unsupported GenericContainerScreenHandler row count $rows")
    }
}

abstract class DynamicChestScreenHandler(protected val rows: Int, sync: Int, playerInventory: PlayerInventory)
    : DynamicScreenHandler(screenHandlerTypeForChest(rows), sync, playerInventory)
{
    final override val inventory = Inventories.create(rows * 9)

    protected fun interface ChestSlotProvider {
        fun create(index: Int, row: Int, col: Int, x: Int, y: Int): Slot?
    }

    protected fun addChestInventorySlots(
        provider: ChestSlotProvider = ChestSlotProvider { _, _, _, _, _ -> null }
    ) {
        for (row in 0 until rows) {
            for (col in 0 until 9) {
                val index = col + row * 9
                val x = 8 + col * 18
                val y = 18 + row * 18
                addSlot(provider.create(index, row, col, x, y) ?: InputSlot(index, x, y))
            }
        }
    }
}
