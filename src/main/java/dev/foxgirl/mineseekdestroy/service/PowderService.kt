package dev.foxgirl.mineseekdestroy.service

import com.google.common.collect.ImmutableSet
import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*

class PowderService : Service() {

    private val powderItemList = ImmutableSet.copyOf(arrayOf(
        YELLOW_CONCRETE_POWDER,
        BLUE_CONCRETE_POWDER,
        BLACK_CONCRETE_POWDER,
        BROWN_CONCRETE_POWDER,
        LIME_CONCRETE_POWDER,
        WHITE_CONCRETE_POWDER,
        ORANGE_CONCRETE_POWDER,
        MAGENTA_CONCRETE_POWDER,
        LIGHT_BLUE_CONCRETE_POWDER,
        PINK_CONCRETE_POWDER,
        GRAY_CONCRETE_POWDER,
        LIGHT_GRAY_CONCRETE_POWDER,
        CYAN_CONCRETE_POWDER,
        PURPLE_CONCRETE_POWDER,
        GREEN_CONCRETE_POWDER,
        RED_CONCRETE_POWDER,
    ))

    fun handleUpdate() {
        for (player in players) {
            if (!player.isPlaying) continue

            val inventory = player.inventory ?: continue

            val powderItem = when (player.team) {
                GameTeam.PLAYER_BLACK -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_YELLOW -> YELLOW_CONCRETE_POWDER
                GameTeam.PLAYER_BLUE -> BLUE_CONCRETE_POWDER
                GameTeam.PLAYER_DUEL -> BROWN_CONCRETE_POWDER
                else -> continue
            }

            for (i in 0 until inventory.size()) {
                val stack = inventory.getStack(i)
                val item = stack.item
                if (item !== AIR && item !== powderItem && powderItemList.contains(item)) {
                    val count = stack.count
                    inventory.removeStack(i)
                    inventory.insertStack(ItemStack(powderItem, count))
                }
            }
        }
    }

}
