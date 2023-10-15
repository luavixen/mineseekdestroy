package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.GameTeam.*
import dev.foxgirl.mineseekdestroy.util.Rules
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import dev.foxgirl.mineseekdestroy.util.dataDisplay
import dev.foxgirl.mineseekdestroy.util.set
import dev.foxgirl.mineseekdestroy.util.stackOf
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.item.trim.*
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.DyeColor
import java.util.*

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

        fun ItemStack.color(color: DyeColor) =
            this.also { stack -> stack.dataDisplay()["color"] = convertColor(color) }
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

        val loadoutEmpty = Array(4) { ItemStack.EMPTY }
        val loadoutDuel = arrayOf(
            stackOf(LEATHER_BOOTS)
                .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.SNOUT),
            stackOf(CHAINMAIL_LEGGINGS)
                .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.HOST)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(CHAINMAIL_CHESTPLATE)
                .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.HOST),
            stackOf(LEATHER_HELMET)
                .trim(ArmorTrimMaterials.REDSTONE, ArmorTrimPatterns.SHAPER),
        )
        val loadoutWarden = arrayOf(
            stackOf(NETHERITE_BOOTS)
                .enchant(Enchantments.FEATHER_FALLING, 4)
                .enchant(Enchantments.FROST_WALKER, 2),
            stackOf(NETHERITE_LEGGINGS)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(NETHERITE_CHESTPLATE)
                .enchant(Enchantments.THORNS, 3),
            stackOf(NETHERITE_HELMET)
                .enchant(Enchantments.RESPIRATION, 3)
                .enchant(Enchantments.THORNS, 3),
        )
        val loadoutBlack = arrayOf(
            stackOf(LEATHER_BOOTS)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.SNOUT),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.HOST)
                .enchant(Enchantments.THORNS, 3)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(LEATHER_CHESTPLATE)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.HOST),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.AMETHYST, ArmorTrimPatterns.SHAPER),
        )
        val loadoutYellow = arrayOf(
            stackOf(LEATHER_BOOTS)
                .color(DyeColor.YELLOW).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.SNOUT),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.YELLOW).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.HOST)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(LEATHER_CHESTPLATE)
                .color(DyeColor.YELLOW).trim(ArmorTrimMaterials.NETHERITE, ArmorTrimPatterns.SILENCE),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.YELLOW).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.SHAPER),
        )
        val loadoutBlue = arrayOf(
            stackOf(LEATHER_BOOTS)
                .color(DyeColor.BLUE).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.SNOUT),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.BLUE).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.HOST)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(ELYTRA),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.BLUE).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.SHAPER),
        )

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

    override fun update() {
        for ((player, playerEntity) in playerEntities) {
            if (game.isOperator(playerEntity)) continue

            val inventory = playerEntity.inventory!!

            armorRemove(inventory.main)
            armorRemove(inventory.offHand)

            val armorAttribute = playerEntity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR)!!
            var armorModifierType = ArmorModifierType.NONE

            if (Rules.hiddenArmorEnabled) {
                armorModifierType = armorSetHidden(inventory.armor, player)
            } else {
                if (player.team.isPlayingOrGhost) {
                    armorSet(inventory.armor, player)
                } else {
                    armorRemove(inventory.armor)
                }
            }

            for ((type, modifier) in armorModifiers) {
                if (armorAttribute.hasModifier(modifier)) {
                    if (armorModifierType !== type) armorAttribute.removeModifier(modifier)
                } else {
                    if (armorModifierType === type) armorAttribute.addPersistentModifier(modifier)
                }
            }
        }
    }

    private fun armorSet(list: MutableList<ItemStack>, player: GamePlayer) {
        val loadout = loadouts[player.team]!!
        for (i in list.indices) {
            if (ItemStack.areEqual(list[i], loadout[i])) continue
            list[i] = loadout[i].copy()
        }
    }

    private fun armorRemove(list: MutableList<ItemStack>, ignoreElytra: Boolean = false) {
        for (i in list.indices) {
            val stack = list[i]
            val item = stack.item
            if (item !is ArmorItem && (item !== ELYTRA || ignoreElytra)) continue
            list[i] = ItemStack.EMPTY
        }
    }

    private fun armorSetHidden(list: MutableList<ItemStack>, player: GamePlayer): ArmorModifierType {
        val stack = loadouts[player.team]!![2]
        return if (stack.item === ELYTRA) {
            if (!ItemStack.areEqual(list[2], stack)) list[2] = stack.copy()
            armorRemove(list, true)
            ArmorModifierType.ELYTRA
        } else {
            armorRemove(list, false)
            ArmorModifierType.FULL
        }
    }

    private enum class ArmorModifierType { NONE, ELYTRA, FULL }

    private companion object {

        private val armorModifierFull =
            EntityAttributeModifier(UUID.fromString("a459f091-7fc2-42b0-b44f-eb33fddee8c3"), "msd_armor_full", 7.0, EntityAttributeModifier.Operation.ADDITION)
        private val armorModifierElytra =
            EntityAttributeModifier(UUID.fromString("442563b0-7d6a-4b18-b82f-01ecc5e6bc77"), "msd_armor_elytra", 4.0, EntityAttributeModifier.Operation.ADDITION)

        private val armorModifiers = immutableListOf(
            ArmorModifierType.FULL to armorModifierFull,
            ArmorModifierType.ELYTRA to armorModifierElytra,
        )

    }

}
