package dev.foxgirl.mineseekdestroy.util

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

abstract class AbstractInventory : Inventory {

    abstract override fun size(): Int

    abstract operator fun get(slot: Int): ItemStack
    abstract operator fun set(slot: Int, stack: ItemStack): ItemStack

    open fun isMutable(slot: Int) = true

    final override fun getStack(slot: Int): ItemStack {
        return if (isMutable(slot)) get(slot) else get(slot).copy()
    }
    final override fun setStack(slot: Int, stack: ItemStack) {
        if (isMutable(slot)) set(slot, stack)
    }
    final override fun removeStack(slot: Int): ItemStack {
        if (isMutable(slot)) {
            return set(slot, stackOf())
        }
        return stackOf()
    }
    final override fun removeStack(slot: Int, amount: Int): ItemStack {
        if (isMutable(slot)) {
            return get(slot).split(amount)
        }
        return stackOf()
    }

    override fun isEmpty(): Boolean {
        for (i in 0 until size()) {
            if (!getStack(i).isEmpty) {
                return false
            }
        }
        return true
    }

    override fun clear() {
        for (i in 0 until size()) {
            setStack(i, stackOf())
        }
    }

    override fun isValid(slot: Int, stack: ItemStack): Boolean {
        return slot >= 0 && slot < size()
    }

    override fun canPlayerUse(player: PlayerEntity): Boolean {
        return true
    }

    override fun onOpen(player: PlayerEntity) {
    }
    override fun onClose(player: PlayerEntity) {
    }

    override fun markDirty() {
    }

}
