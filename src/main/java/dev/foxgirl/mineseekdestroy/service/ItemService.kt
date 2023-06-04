package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import dev.foxgirl.mineseekdestroy.util.collect.immutableMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity

class ItemService : Service() {

    private inline fun Inventory.forEach(action: (ItemStack, Item, Int) -> Unit) {
        for (i in 0 until size()) {
            val stack: ItemStack = getStack(i)
            val item: Item = stack.item
            if (item !== AIR) action(stack, item, i)
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

            val toolItems = when (player.mainTeam) {
                GameTeam.PLAYER_BLUE -> toolItemsBlue
                else -> toolItemsYellow
            }

            val powderItem = when (player.team) {
                GameTeam.PLAYER_DUEL -> BROWN_CONCRETE_POWDER
                GameTeam.PLAYER_WARDEN -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_BLACK -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_YELLOW -> YELLOW_CONCRETE_POWDER
                GameTeam.PLAYER_BLUE -> BLUE_CONCRETE_POWDER
                else -> continue
            }

            inventory.forEach { stack, item, i ->
                val toolItem = toolItems[item]
                if (toolItem != null) {
                    inventory.setStack(i, ItemStack(toolItem))
                }
                if (powderItems.contains(item) && item !== powderItem) {
                    val count = stack.count
                    inventory.setStack(i, ItemStack.EMPTY)
                    inventory.insertStack(ItemStack(powderItem, count))
                }
                if (illegalItems.contains(item)) {
                    inventory.setStack(i, ItemStack.EMPTY)
                }
                val nbt: NbtCompound? = stack.nbt
                if (nbt != null && nbt.contains("MsdIllegal")) {
                    inventory.setStack(i, ItemStack.EMPTY)
                }
            }
        }
    }

    private companion object {

        private val droppedItems = Game.DROPPED_ITEMS

        private val illegalItems = Game.ILLEGAL_ITEMS

        private val powderItems = immutableSetOf(
            WHITE_CONCRETE_POWDER, WHITE_CONCRETE,
            ORANGE_CONCRETE_POWDER, ORANGE_CONCRETE,
            MAGENTA_CONCRETE_POWDER, MAGENTA_CONCRETE,
            LIGHT_BLUE_CONCRETE_POWDER, LIGHT_BLUE_CONCRETE,
            YELLOW_CONCRETE_POWDER, YELLOW_CONCRETE,
            LIME_CONCRETE_POWDER, LIME_CONCRETE,
            PINK_CONCRETE_POWDER, PINK_CONCRETE,
            GRAY_CONCRETE_POWDER, GRAY_CONCRETE,
            LIGHT_GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE,
            CYAN_CONCRETE_POWDER, CYAN_CONCRETE,
            PURPLE_CONCRETE_POWDER, PURPLE_CONCRETE,
            BLUE_CONCRETE_POWDER, BLUE_CONCRETE,
            BROWN_CONCRETE_POWDER, BROWN_CONCRETE,
            GREEN_CONCRETE_POWDER, GREEN_CONCRETE,
            RED_CONCRETE_POWDER, RED_CONCRETE,
            BLACK_CONCRETE_POWDER, BLACK_CONCRETE,
            WHITE_TERRACOTTA, LIGHT_GRAY_TERRACOTTA,
        )

        private val toolItemsYellow = immutableMapOf(
            IRON_SWORD to IRON_AXE,
            IRON_PICKAXE to IRON_SHOVEL,
            BOW to CROSSBOW,
        )
        private val toolItemsBlue = immutableMapOf(
            IRON_AXE to IRON_SWORD,
            IRON_SHOVEL to IRON_PICKAXE,
            CROSSBOW to BOW,
        )

    }

}
