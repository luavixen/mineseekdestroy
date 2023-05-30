package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam.*
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ArmorItem
import net.minecraft.item.DyeableItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.util.DyeColor

class ArmorService : Service() {

    override fun update() {
        for ((player, entity) in playerEntities) {
            if (game.isOperator(entity)) continue

            val inventory = entity.inventory!!
            var inventoryDirty = false

            if (armorRemove(inventory.main)) {
                inventoryDirty = true
            }
            if (armorRemove(inventory.offHand)) {
                inventoryDirty = true
            }

            if (player.team.isPlaying) {
                if (armorSet(inventory.armor, player)) {
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

        private val loadoutFor: (GamePlayer) -> Array<ItemStack>

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

            fun armorColorer(color: Int): (Item) -> ItemStack =
                { item -> ItemStack(item).also { (item as DyeableItem).setColor(it, color) } }

            val loadoutEmpty: Array<ItemStack> = run {
                Array(4) { ItemStack.EMPTY }
            }
            val loadoutDuel: Array<ItemStack> = run {
                arrayOf(
                    ItemStack(LEATHER_BOOTS),
                    ItemStack(CHAINMAIL_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    ItemStack(CHAINMAIL_CHESTPLATE),
                    ItemStack(LEATHER_HELMET),
                )
            }
            val loadoutWarden: Array<ItemStack> = run {
                arrayOf(
                    ItemStack(NETHERITE_BOOTS)
                        .apply { addEnchantment(Enchantments.FEATHER_FALLING, 4) }
                        .apply { addEnchantment(Enchantments.FROST_WALKER, 2) },
                    ItemStack(NETHERITE_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    ItemStack(NETHERITE_CHESTPLATE)
                        .apply { addEnchantment(Enchantments.THORNS, 3) },
                    ItemStack(NETHERITE_HELMET)
                        .apply { addEnchantment(Enchantments.RESPIRATION, 3) }
                        .apply { addEnchantment(Enchantments.THORNS, 3) },
                )
            }
            val loadoutBlackYellow: Array<ItemStack> = run {
                val armor = armorColorer(colorBlack)
                arrayOf(
                    armor(LEATHER_BOOTS)
                        .apply { addEnchantment(Enchantments.THORNS, 3) },
                    armor(LEATHER_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    armor(LEATHER_CHESTPLATE),
                    armor(LEATHER_HELMET),
                )
            }
            val loadoutBlackBlue: Array<ItemStack> = run {
                val armor = armorColorer(colorBlack)
                arrayOf(
                    armor(LEATHER_BOOTS)
                        .apply { addEnchantment(Enchantments.THORNS, 3) },
                    armor(LEATHER_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    ItemStack(ELYTRA),
                    armor(LEATHER_HELMET),
                )
            }
            val loadoutYellow: Array<ItemStack> = run {
                val armor = armorColorer(colorYellow)
                arrayOf(
                    armor(LEATHER_BOOTS),
                    armor(LEATHER_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    armor(LEATHER_CHESTPLATE),
                    armor(LEATHER_HELMET),
                )
            }
            val loadoutBlue: Array<ItemStack> = run {
                val armor = armorColorer(colorBlue)
                arrayOf(
                    armor(LEATHER_BOOTS),
                    armor(LEATHER_LEGGINGS)
                        .apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) },
                    ItemStack(ELYTRA),
                    armor(LEATHER_HELMET),
                )
            }

            loadoutFor = { player ->
                when (player.team) {
                    NONE -> loadoutEmpty
                    SKIP -> loadoutEmpty
                    OPERATOR -> loadoutEmpty
                    PLAYER_DUEL -> loadoutDuel
                    PLAYER_WARDEN -> loadoutWarden
                    PLAYER_YELLOW -> loadoutYellow
                    PLAYER_BLUE -> loadoutBlue
                    PLAYER_BLACK ->
                        if (player.mainTeam == PLAYER_BLUE) loadoutBlackBlue else loadoutBlackYellow
                }
            }
        }

        private fun armorSet(list: MutableList<ItemStack>, player: GamePlayer): Boolean {
            val loadout = loadoutFor(player)
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
                if (stack.item !is ArmorItem) continue
                list[i] = ItemStack.EMPTY
                dirty = true
            }
            return dirty
        }

    }

}
