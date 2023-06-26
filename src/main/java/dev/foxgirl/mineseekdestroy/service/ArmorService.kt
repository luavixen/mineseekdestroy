package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.GameTeam.*
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.item.trim.*
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.DyeColor

class ArmorService : Service() {

    private var loadouts = mapOf<GameTeam, Array<ItemStack>>()

    override fun setup() {
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

        fun stack(item: Item) = ItemStack(item)

        fun ItemStack.color(color: Int) =
            this.also { stack -> stack.getOrCreateSubNbt("display").putInt("color", color) }
        fun ItemStack.enchant(enchantment: Enchantment, level: Int) =
            this.also { stack -> stack.addEnchantment(enchantment, level) }

        val registryManager = world.registryManager
        val registryTrimMaterials = registryManager.get(RegistryKeys.TRIM_MATERIAL)
        val registryTrimPatterns = registryManager.get(RegistryKeys.TRIM_PATTERN)

        fun ItemStack.trim(material: RegistryKey<ArmorTrimMaterial>, pattern: RegistryKey<ArmorTrimPattern>) =
            this.also { stack ->
                val trim = ArmorTrim(
                    registryTrimMaterials.getEntry(material).get(),
                    registryTrimPatterns.getEntry(pattern).get(),
                )
                ArmorTrim.apply(registryManager, stack, trim)
            }

        val loadoutEmpty: Array<ItemStack> = run {
            Array(4) { ItemStack.EMPTY }
        }
        val loadoutDuel: Array<ItemStack> = run {
            arrayOf(
                stack(LEATHER_BOOTS)
                    .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.SNOUT),
                stack(CHAINMAIL_LEGGINGS)
                    .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.HOST)
                    .enchant(Enchantments.SWIFT_SNEAK, 3),
                stack(CHAINMAIL_CHESTPLATE)
                    .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.HOST),
                stack(LEATHER_HELMET)
                    .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.SHAPER),
            )
        }
        val loadoutWarden: Array<ItemStack> = run {
            arrayOf(
                stack(NETHERITE_BOOTS)
                    .enchant(Enchantments.FEATHER_FALLING, 4)
                    .enchant(Enchantments.FROST_WALKER, 2),
                stack(NETHERITE_LEGGINGS)
                    .enchant(Enchantments.SWIFT_SNEAK, 3),
                stack(NETHERITE_CHESTPLATE)
                    .enchant(Enchantments.THORNS, 3),
                stack(NETHERITE_HELMET)
                    .enchant(Enchantments.RESPIRATION, 3)
                    .enchant(Enchantments.THORNS, 3),
            )
        }
        val loadoutBlack: Array<ItemStack> = run {
            arrayOf(
                stack(LEATHER_BOOTS)
                    .color(colorBlack).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.SNOUT),
                stack(LEATHER_LEGGINGS)
                    .color(colorBlack).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.HOST)
                    .enchant(Enchantments.THORNS, 3)
                    .enchant(Enchantments.SWIFT_SNEAK, 3),
                stack(LEATHER_CHESTPLATE)
                    .color(colorBlack).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.HOST),
                stack(LEATHER_HELMET)
                    .color(colorBlack).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.SHAPER),
            )
        }
        val loadoutYellow: Array<ItemStack> = run {
            arrayOf(
                stack(LEATHER_BOOTS)
                    .color(colorYellow).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.SNOUT),
                stack(LEATHER_LEGGINGS)
                    .color(colorYellow).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.HOST)
                    .enchant(Enchantments.SWIFT_SNEAK, 3),
                stack(LEATHER_CHESTPLATE)
                    .color(colorYellow).trim(ArmorTrimMaterials.NETHERITE, ArmorTrimPatterns.SILENCE),
                stack(LEATHER_HELMET)
                    .color(colorYellow).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.SHAPER),
            )
        }
        val loadoutBlue: Array<ItemStack> = run {
            arrayOf(
                stack(LEATHER_BOOTS)
                    .color(colorBlue).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.SNOUT),
                stack(LEATHER_LEGGINGS)
                    .color(colorBlue).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.HOST)
                    .enchant(Enchantments.SWIFT_SNEAK, 3),
                stack(ELYTRA),
                stack(LEATHER_HELMET)
                    .color(colorBlue).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.SHAPER),
            )
        }

        loadouts = enumMapOf(
            NONE to loadoutEmpty,
            SKIP to loadoutEmpty,
            GHOST to loadoutEmpty,
            OPERATOR to loadoutEmpty,
            PLAYER_DUEL to loadoutDuel,
            PLAYER_WARDEN to loadoutWarden,
            PLAYER_YELLOW to loadoutYellow,
            PLAYER_BLUE to loadoutBlue,
            PLAYER_BLACK to loadoutBlack,
        )
    }

    private fun armorSet(list: MutableList<ItemStack>, player: GamePlayer): Boolean {
        val loadout = loadouts[player.team]!!
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
            val item = stack.item
            if (item !is ArmorItem && item !== ELYTRA) continue
            list[i] = ItemStack.EMPTY
            dirty = true
        }
        return dirty
    }

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

            if (player.team.isPlayingOrGhost) {
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

}
