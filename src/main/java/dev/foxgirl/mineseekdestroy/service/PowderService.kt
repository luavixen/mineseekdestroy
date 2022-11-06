package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class PowderService : Service() {

    fun handleUpdate() {
        for (player in players) {
            if (!player.isPlaying) continue

            val inventory = player.inventory ?: continue

            val concretePowderTeam = when (player.team) {
                GameTeam.PLAYER_BLACK -> Blocks.BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_YELLOW -> Blocks.YELLOW_CONCRETE_POWDER
                GameTeam.PLAYER_BLUE -> Blocks.BLUE_CONCRETE_POWDER
                else -> continue
            }

            for (i in 0 until inventory.size()) {
                val stack = inventory.getStack(i)
                if (stack.let { !it.isEmpty && concretePowderBlocks.contains(it.item) }) {
                    inventory.setStack(i, ItemStack(concretePowderTeam.asItem(), stack.count))
                }
            }

        }
    }

    private companion object {

        private val concretePowderBlocks = setOf<Item>(
            Items.WHITE_CONCRETE_POWDER,
            Items.ORANGE_CONCRETE_POWDER,
            Items.MAGENTA_CONCRETE_POWDER,
            Items.LIGHT_BLUE_CONCRETE_POWDER,
            Items.YELLOW_CONCRETE_POWDER,
            Items.LIME_CONCRETE_POWDER,
            Items.PINK_CONCRETE_POWDER,
            Items.GRAY_CONCRETE_POWDER,
            Items.LIGHT_GRAY_CONCRETE_POWDER,
            Items.CYAN_CONCRETE_POWDER,
            Items.PURPLE_CONCRETE_POWDER,
            Items.BLUE_CONCRETE_POWDER,
            Items.BROWN_CONCRETE_POWDER,
            Items.GREEN_CONCRETE_POWDER,
            Items.RED_CONCRETE_POWDER,
            Items.BLACK_CONCRETE_POWDER,
        )

    }

}
