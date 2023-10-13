package dev.foxgirl.mineseekdestroy.service

import com.viaversion.viaversion.api.Via
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.GameTeam.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.item.trim.*
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.DyeColor
import net.minecraft.util.collection.DefaultedList
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
        val loadoutCrab = arrayOf(
            stackOf(LEATHER_BOOTS)
                .color(DyeColor.ORANGE).trim(ArmorTrimMaterials.LAPIS, ArmorTrimPatterns.COAST),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.ORANGE).trim(ArmorTrimMaterials.LAPIS, ArmorTrimPatterns.SNOUT)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(LEATHER_CHESTPLATE)
                .color(DyeColor.ORANGE).trim(ArmorTrimMaterials.LAPIS, ArmorTrimPatterns.SILENCE),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.ORANGE).trim(ArmorTrimMaterials.LAPIS, ArmorTrimPatterns.EYE),
        )
        val loadoutArmadillo = arrayOf(
            stackOf(LEATHER_BOOTS)
                .color(DyeColor.PINK).trim(ArmorTrimMaterials.COPPER, ArmorTrimPatterns.SILENCE),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.PINK).trim(ArmorTrimMaterials.COPPER, ArmorTrimPatterns.SILENCE)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(LEATHER_CHESTPLATE)
                .color(DyeColor.PINK).trim(ArmorTrimMaterials.COPPER, ArmorTrimPatterns.SILENCE),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.PINK).trim(ArmorTrimMaterials.COPPER, ArmorTrimPatterns.SILENCE),
        )
        val loadoutPenguin = arrayOf(
            stackOf(LEATHER_BOOTS)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.QUARTZ, ArmorTrimPatterns.WILD),
            stackOf(LEATHER_LEGGINGS)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.QUARTZ, ArmorTrimPatterns.VEX)
                .enchant(Enchantments.SWIFT_SNEAK, 3),
            stackOf(ELYTRA),
            stackOf(LEATHER_HELMET)
                .color(DyeColor.BLACK).trim(ArmorTrimMaterials.GOLD, ArmorTrimPatterns.VEX),
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
            PLAYER_CRAB to loadoutCrab,
            PLAYER_ARMADILLO to loadoutArmadillo,
            PLAYER_PENGUIN to loadoutPenguin,
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

    fun handlePacket(packet: Packet<*>, playerEntity: ServerPlayerEntity): Packet<*>? {
        if (hasArmorTrimRenderingBug(playerEntity)) {
            if (packet is ScreenHandlerSlotUpdateS2CPacket)
                return handleSlotUpdatePacket(packet)
            if (packet is EntityEquipmentUpdateS2CPacket)
                return handleEquipmentUpdatePacket(packet)
            if (packet is InventoryS2CPacket)
                return handleInventoryPacket(packet)
        }
        return null
    }

    private fun hasArmorTrimRenderingBug(playerEntity: ServerPlayerEntity): Boolean {
        return try {
            Via.getAPI().getPlayerVersion(playerEntity.uuid) <= 762
        } catch (cause: IllegalArgumentException) {
            false
        }
    }

    private fun handleSlotUpdatePacket(packet1: ScreenHandlerSlotUpdateS2CPacket): ScreenHandlerSlotUpdateS2CPacket? {
        if (removeArmorTrimsCheck(packet1.stack)) {
            return Reflector.create(ScreenHandlerSlotUpdateS2CPacket::class.java).apply {
                syncId = packet1.syncId
                revision = packet1.revision
                slot = packet1.slot
                stack = removeArmorTrimsApply(packet1.stack)
            }
        }
        return null
    }
    private fun handleEquipmentUpdatePacket(packet1: EntityEquipmentUpdateS2CPacket): EntityEquipmentUpdateS2CPacket? {
        return removeArmorTrimsFromPacket(
            packet1,
            { entry -> entry.second },
            { entry, stack -> com.mojang.datafixers.util.Pair(entry.first, stack) },
            { packet2 -> packet2.equipmentList },
            { packet2, list ->
                Reflector.create(EntityEquipmentUpdateS2CPacket::class.java).apply {
                    id = packet2.id
                    equipmentList = list
                }
            },
        )
    }
    private fun handleInventoryPacket(packet1: InventoryS2CPacket): InventoryS2CPacket? {
        return removeArmorTrimsFromPacket(
            packet1,
            { stack -> stack },
            { _, stack -> stack },
            { packet2 -> (packet2.contents as DefaultedList<ItemStack>).delegate },
            { packet2, list ->
                Reflector.create(InventoryS2CPacket::class.java).apply {
                    syncId = packet2.syncId
                    revision = packet2.revision
                    contents = list
                    cursorStack = packet2.cursorStack.let { if (removeArmorTrimsCheck(it)) removeArmorTrimsApply(it) else it }
                }
            },
        )
    }

    private fun removeArmorTrimsCheck(stack: ItemStack?): Boolean {
        if (stack != null) {
            val nbt = stack.nbt
            return nbt != null && nbt.contains("Trim")
        }
        return false
    }
    private fun removeArmorTrimsApply(stack: ItemStack): ItemStack {
        return stack.copy().apply { nbt?.remove("Trim") }
    }
    @Suppress("UNCHECKED_CAST")
    private inline fun <T> removeArmorTrims(list: List<T>, getStack: (T) -> ItemStack, updateStack: (T, ItemStack) -> T): List<T>? {
        for (i1 in list.indices) {
            if (removeArmorTrimsCheck(getStack(list[i1]))) {
                val array = (list as java.util.List<T>).toArray() as Array<T>
                for (i2 in i1 until array.size) {
                    val entry = array[i2]
                    val stack = getStack(entry)
                    if (removeArmorTrimsCheck(stack)) {
                        array[i2] = updateStack(entry, removeArmorTrimsApply(stack))
                    }
                }
                return array.asList()
            }
        }
        return null
    }
    private inline fun <T, P : Packet<*>> removeArmorTrimsFromPacket(
        packet: P,
        getStack: (T) -> ItemStack,
        updateStack: (T, ItemStack) -> T,
        getPacketList: (P) -> List<T>,
        updatePacketList: (P, List<T>) -> P,
    ): P? {
        val list = removeArmorTrims(getPacketList(packet), getStack, updateStack)
        return if (list != null) updatePacketList(packet, list) else null
    }

}
