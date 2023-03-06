package dev.foxgirl.mineseekdestroy.service

import com.google.common.collect.ImmutableSet
import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity

class ItemService : Service() {

    private inline fun Inventory.forEach(action: (ItemStack, Item, Int) -> Unit) {
        for (i in 0 until size()) {
            val stack: ItemStack = getStack(i)
            val item: Item = stack.item
            if (item !== Items.AIR) action(stack, item, i)
        }
    }

    fun handleDropInventory(entity: ServerPlayerEntity) {
        if (!context.getPlayer(entity).isPlaying) return

        val inventory = entity.inventory

        inventory.forEach { stack, item, i ->
            if (droppedItems.contains(item)) {
                inventory.setStack(i, ItemStack.EMPTY)
                entity.dropItem(stack, true, false)
            }
        }
    }

    override fun update() {
        val running = state is RunningGameState

        for (player in players) {
            if (running && player.isSpectator) continue

            val inventory = player.inventory ?: continue

            val powderItem = when (player.team) {
                GameTeam.PLAYER_BLACK -> Items.BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_YELLOW -> Items.YELLOW_CONCRETE_POWDER
                GameTeam.PLAYER_BLUE -> Items.BLUE_CONCRETE_POWDER
                GameTeam.PLAYER_DUEL -> Items.BROWN_CONCRETE_POWDER
                else -> continue
            }

            inventory.forEach { stack, item, i ->
                if (powderItems.contains(item) && item !== powderItem) {
                    val count = stack.count
                    inventory.setStack(i, ItemStack.EMPTY)
                    inventory.insertStack(ItemStack(powderItem, count))
                }
                if (illegalItems.contains(item)) {
                    inventory.setStack(i, ItemStack.EMPTY)
                }
            }
        }
    }

    private companion object {

        private val droppedItems = Game.DROPPED_ITEMS

        private val illegalItems = Game.ILLEGAL_ITEMS

        private val powderItems = ImmutableSet.copyOf(arrayOf(
            Items.YELLOW_CONCRETE_POWDER,
            Items.BLUE_CONCRETE_POWDER,
            Items.BLACK_CONCRETE_POWDER,
            Items.BROWN_CONCRETE_POWDER,
            Items.LIME_CONCRETE_POWDER,
            Items.WHITE_CONCRETE_POWDER,
            Items.ORANGE_CONCRETE_POWDER,
            Items.MAGENTA_CONCRETE_POWDER,
            Items.LIGHT_BLUE_CONCRETE_POWDER,
            Items.PINK_CONCRETE_POWDER,
            Items.GRAY_CONCRETE_POWDER,
            Items.LIGHT_GRAY_CONCRETE_POWDER,
            Items.CYAN_CONCRETE_POWDER,
            Items.PURPLE_CONCRETE_POWDER,
            Items.GREEN_CONCRETE_POWDER,
            Items.RED_CONCRETE_POWDER,
        ))

    }

}
