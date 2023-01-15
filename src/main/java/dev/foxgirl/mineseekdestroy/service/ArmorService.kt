package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.GameTeam.*
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ArmorItem
import net.minecraft.item.DyeableItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
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

        private val loadouts: Map<GameTeam, Array<ItemStack>>

        init {
            fun convertComponent(component: Float): Int {
                return (component * 255.0F).toInt().coerceIn(0, 0xFF)
            }
            fun convertColor(color: DyeColor): Int {
                val components = color.colorComponents
                val r = convertComponent(components[0])
                val g = convertComponent(components[1])
                val b = convertComponent(components[2])
                return (r shl 16) or (g shl 8) or b
            }

            val colorBlack = convertColor(DyeColor.BLACK)
            val colorYellow = convertColor(DyeColor.YELLOW)
            val colorBlue = convertColor(DyeColor.BLUE)

            fun loadoutEmpty(): Array<ItemStack> {
                return Array(4) { ItemStack.EMPTY }
            }
            fun loadoutDueler(): Array<ItemStack> {
                fun armor(item: Item) = ItemStack(item)
                return arrayOf(
                    armor(LEATHER_BOOTS),
                    armor(CHAINMAIL_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    armor(CHAINMAIL_CHESTPLATE),
                    armor(LEATHER_HELMET),
                )
            }
            fun loadoutNormal(color: Int): Array<ItemStack> {
                fun armorDyed(item: Item) =
                    ItemStack(item).also { (item as DyeableItem).setColor(it, color) }
                return arrayOf(
                    armorDyed(LEATHER_BOOTS),
                    armorDyed(LEATHER_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    armorDyed(LEATHER_CHESTPLATE),
                    armorDyed(LEATHER_HELMET),
                )
            }

            loadouts = buildMap(8) {
                put(NONE, loadoutEmpty())
                put(OPERATOR, loadoutEmpty())
                put(PLAYER_DUEL, loadoutDueler())
                put(PLAYER_BLACK, loadoutNormal(colorBlack))
                put(PLAYER_YELLOW, loadoutNormal(colorYellow))
                put(PLAYER_BLUE, loadoutNormal(colorBlue))
            }
        }

        private fun armorSet(list: MutableList<ItemStack>, team: GameTeam): Boolean {
            val loadout = loadouts[team]!!
            var dirty = false
            for (i in list.indices) {
                if (ItemStack.areEqual(list[i], loadout[i])) continue
                list[i] = loadout[i].copy()
                dirty = true
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

    }

}
