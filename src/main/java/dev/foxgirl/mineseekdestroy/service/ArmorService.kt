package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.GameTeam.*
import dev.foxgirl.mineseekdestroy.util.*
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

class ArmorService : Service() {

    private fun armorRemove(list: MutableList<ItemStack>) {
        for ((i, stack) in list.withIndex()) {
            val item = stack.item
            if (item is ArmorItem || item === ELYTRA) {
                list[i] = stackOf()
            }
        }
    }

    private fun armorSet(list: MutableList<ItemStack>, loadout: Array<ItemStack>) {
        for ((i, stack) in list.withIndex()) {
            if (stack contentEquals loadout[i]) continue
            list[i] = loadout[i].copy()
        }
    }

    private fun armorSetHidden(list: MutableList<ItemStack>, loadout: Array<ItemStack>) {
        if (loadout[2].item === ELYTRA) {
            armorSet(list, loadoutEmptyWithElytra)
        } else {
            armorSet(list, loadoutEmpty)
        }
    }

    private val armorModifiers = HashMap<Int, EntityAttributeModifier>()

    private fun armorModifierFor(protection: Int) =
        armorModifiers.computeIfAbsent(protection) {
            EntityAttributeModifier("msd_armor_$it", it.toDouble(), EntityAttributeModifier.Operation.ADDITION)
        }
    private fun armorModifierFor(armor: Array<ItemStack>) =
        armorModifierFor(armor.sumOf {
            val item = it.item
            if (item is ArmorItem) item.protection else 0
        })

    private val loadoutEmpty = arrayOf(stackOf(), stackOf(), stackOf(), stackOf())
    private val loadoutEmptyWithElytra = arrayOf(stackOf(), stackOf(), stackOf(ELYTRA), stackOf())

    private lateinit var loadoutFor: (GamePlayer) -> Array<ItemStack>

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

        fun ItemStack.color(color: DyeColor): ItemStack {
            dataDisplay()["color"] = convertColor(color)
            return this
        }
        fun ItemStack.enchant(enchantment: Enchantment, level: Int): ItemStack {
            addEnchantment(enchantment, level)
            return this
        }

        val registryManager = world.registryManager
        val registryTrimMaterials = registryManager.get(RegistryKeys.TRIM_MATERIAL)
        val registryTrimPatterns = registryManager.get(RegistryKeys.TRIM_PATTERN)

        fun ItemStack.trim(material: RegistryKey<ArmorTrimMaterial>, pattern: RegistryKey<ArmorTrimPattern>): ItemStack {
            ArmorTrim.apply(registryManager, this, ArmorTrim(
                registryTrimMaterials.getEntry(material).get(),
                registryTrimPatterns.getEntry(pattern).get(),
            ))
            return this
        }

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
                .color(DyeColor.YELLOW).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.SNOUT)
                .enchant(Enchantments.BLAST_PROTECTION, 1),
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
                .color(DyeColor.BLUE).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.SNOUT)
                .enchant(Enchantments.DEPTH_STRIDER, 3),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.BLUE).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.HOST)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(ELYTRA),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.BLUE).trim(ArmorTrimMaterials.DIAMOND, ArmorTrimPatterns.SHAPER),
        )

        loadoutFor = { player ->
            when (player.team) {
                NONE, SKIP, GHOST, OPERATOR -> loadoutEmpty
                DUELIST -> loadoutDuel
                WARDEN -> loadoutWarden
                YELLOW -> loadoutYellow
                BLUE -> loadoutBlue
                BLACK -> loadoutBlack
            }
        }
    }

    override fun update() {
        for ((player, playerEntity) in playerEntities) {
            if (game.isOperator(playerEntity)) continue

            val inventory = playerEntity.inventory!!

            armorRemove(inventory.main)
            armorRemove(inventory.offHand)

            val loadout = loadoutFor(player)

            val armorAttribute = playerEntity.getAttributeInstance(EntityAttributes.GENERIC_ARMOR)!!
            var armorModifier = null as EntityAttributeModifier?

            if (Rules.hiddenArmorEnabled) {
                armorSetHidden(inventory.armor, loadout)
                armorModifier = armorModifierFor(loadout)
            } else {
                armorSet(inventory.armor, loadout)
            }

            armorAttribute.modifiers
                .filter { it.name.startsWith("msd_armor_") && it != armorModifier }
                .forEach { armorAttribute.removeModifier(it) }

            if (armorModifier != null && !armorAttribute.hasModifier(armorModifier)) {
                armorAttribute.addTemporaryModifier(armorModifier)
            }
        }
    }

}
