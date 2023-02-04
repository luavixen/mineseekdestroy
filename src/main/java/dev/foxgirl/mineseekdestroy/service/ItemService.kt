package dev.foxgirl.mineseekdestroy.service

import com.google.common.collect.ImmutableSet
import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class ItemService : Service() {

    fun handleUpdate() {
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

            for (i in 0 until inventory.size()) {
                val stack = inventory.getStack(i)
                val item = stack.item
                if (item === Items.AIR) continue
                if (powderItems.contains(item) && item !== powderItem) {
                    val count = stack.count
                    inventory.removeStack(i)
                    inventory.insertStack(ItemStack(powderItem, count))
                }
                if (illegalItems.contains(item)) {
                    inventory.removeStack(i)
                }
            }
        }
    }

    private companion object {

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
