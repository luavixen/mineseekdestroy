package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.item.ArmorItem
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
                if (armorSet(inventory.armor, team)) {
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

        private val itemLeatherHelmet =
            Items.LEATHER_HELMET as DyeableArmorItem
        private val itemLeatherChestplate =
            Items.LEATHER_CHESTPLATE as DyeableArmorItem
        private val itemLeatherLeggings =
            Items.LEATHER_LEGGINGS as DyeableArmorItem
        private val itemLeatherBoots =
            Items.LEATHER_BOOTS as DyeableArmorItem

        private val itemChainmailHelmet =
            Items.CHAINMAIL_HELMET as ArmorItem
        private val itemChainmailChestplate =
            Items.CHAINMAIL_CHESTPLATE as ArmorItem
        private val itemChainmailLeggings =
            Items.CHAINMAIL_LEGGINGS as ArmorItem
        private val itemChainmailBoots =
            Items.CHAINMAIL_BOOTS as ArmorItem

        private fun armorSet(list: MutableList<ItemStack>, team: GameTeam): Boolean {
            var dirty = false

            val color = color(team)

            fun update(index: Int, item: ArmorItem) {
                val stack = list[index]
                if (stack.isEmpty || stack.isDamaged || stack.item != item) {
                    list[index] = ItemStack(item)
                    dirty = true
                }
            }
            fun update(index: Int, item: DyeableArmorItem) {
                val stack = list[index]
                if (stack.isEmpty || stack.isDamaged || stack.item != item || item.getColor(stack) != color) {
                    list[index] = ItemStack(item).also { item.setColor(it, color) }
                    dirty = true
                }
            }

            if (team == GameTeam.PLAYER_DUEL) {
                update(0, itemLeatherBoots)
                update(1, itemChainmailLeggings)
                update(2, itemChainmailChestplate)
                update(3, itemLeatherHelmet)
            } else {
                update(0, itemLeatherBoots)
                update(1, itemLeatherLeggings)
                update(2, itemLeatherChestplate)
                update(3, itemLeatherHelmet)
            }

            return dirty
        }

        private fun armorRemove(list: MutableList<ItemStack>): Boolean {
            var dirty = false
            for (i in list.indices) {
                val stack = list[i]
                if (stack.isEmpty) continue
                if (stack.item !is ArmorItem) continue
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
        private val colorDuel = convertColor(DyeColor.BROWN)

        private fun color(team: GameTeam): Int {
            return when (team) {
                GameTeam.OPERATOR -> colorOperator
                GameTeam.PLAYER_BLACK -> colorBlack
                GameTeam.PLAYER_YELLOW -> colorYellow
                GameTeam.PLAYER_BLUE -> colorBlue
                GameTeam.PLAYER_DUEL -> colorDuel
                else -> 0
            }
        }

    }

}
