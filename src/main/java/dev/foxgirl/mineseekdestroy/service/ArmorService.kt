package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.item.DyeableArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.DyeColor

class ArmorService : Service() {

    fun handleUpdate() {
        for (player in players) {
            val entity = player.entity
            if (entity == null || game.isOperator(entity)) continue

            val inventory = entity.inventory!!
            var inventoryDirty = false

            if (armorRemove(inventory.main)) {
                inventoryDirty = true
            }
            if (armorRemove(inventory.offHand)) {
                inventoryDirty = true
            }

            val team = player.team
            if (team.isPlaying) {
                if (armorSet(inventory.armor, color(team))) {
                    inventoryDirty = true
                }
            } else {
                if (armorRemove(inventory.armor)) {
                    inventoryDirty = true
                }
            }

            if (inventoryDirty) {
                inventory.markDirty()
            }
        }
    }

    private companion object {

        private val itemHelmet =
            Items.LEATHER_HELMET as DyeableArmorItem
        private val itemChestplate =
            Items.LEATHER_CHESTPLATE as DyeableArmorItem
        private val itemLeggings =
            Items.LEATHER_LEGGINGS as DyeableArmorItem
        private val itemBoots =
            Items.LEATHER_BOOTS as DyeableArmorItem

        private fun armorSet(list: MutableList<ItemStack>, color: Int): Boolean {
            var dirty = false

            fun update(index: Int, item: DyeableArmorItem) {
                val stack = list[index]
                if (
                // Must not be empty
                    stack.isEmpty ||
                    // Must be the correct armor item
                    stack.item != item ||
                    // Must not be damaged
                    stack.isDamaged ||
                    // Must be the correct color
                    item.getColor(stack) != color
                ) {
                    list[index] = ItemStack(item).also { item.setColor(it, color) }
                    dirty = true
                }
            }

            update(0, itemBoots)
            update(1, itemLeggings)
            update(2, itemChestplate)
            update(3, itemHelmet)

            return dirty
        }

        private fun armorRemove(list: MutableList<ItemStack>): Boolean {
            var dirty = false
            for (i in list.indices) {
                val stack = list[i]
                if (stack.isEmpty) continue
                if (stack.item !is DyeableArmorItem) continue
                list[i] = ItemStack.EMPTY
                dirty = true
            }
            return dirty
        }

        private fun convertComponent(component: Float): Int {
            return (component * 255.0F).toInt().coerceIn(0, 0xFF);
        }

        private fun convertColor(color: DyeColor): Int {
            val components = color.colorComponents
            val r = convertComponent(components[0])
            val g = convertComponent(components[1])
            val b = convertComponent(components[2])
            return (r shl 16) or (g shl 8) or b
        }

        private val colorOperator = convertColor(DyeColor.LIME)
        private val colorBlack = convertColor(DyeColor.BLACK)
        private val colorYellow = convertColor(DyeColor.YELLOW)
        private val colorBlue = convertColor(DyeColor.BLUE)

        private fun color(team: GameTeam): Int {
            return when (team) {
                GameTeam.OPERATOR -> colorOperator
                GameTeam.PLAYER_BLACK -> colorBlack
                GameTeam.PLAYER_YELLOW -> colorYellow
                GameTeam.PLAYER_BLUE -> colorBlue
                else -> 0
            }
        }

    }

}
