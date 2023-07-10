package dev.foxgirl.mineseekdestroy.service

import com.viaversion.viaversion.api.Via
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.GameTeam.*
import dev.foxgirl.mineseekdestroy.util.Fuck
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
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

    fun handlePacket(packet: Packet<*>, playerEntity: ServerPlayerEntity): Packet<*>? {
        if (!hasArmorTrimRenderingBug(playerEntity)) return null
        return when (packet) {
            is ScreenHandlerSlotUpdateS2CPacket -> handleSlotUpdatePacket(packet)
            is EntityEquipmentUpdateS2CPacket -> handleEquipmentUpdatePacket(packet)
            is InventoryS2CPacket -> handleInventoryPacket(packet)
            else -> null
        }
    }

    private companion object {

        private fun hasArmorTrimRenderingBug(playerEntity: ServerPlayerEntity): Boolean {
            return try {
                Via.getAPI().getPlayerVersion(playerEntity.uuid) <= 762
            } catch (cause: IllegalArgumentException) {
                false
            }
        }

        private fun handleSlotUpdatePacket(packet1: ScreenHandlerSlotUpdateS2CPacket): ScreenHandlerSlotUpdateS2CPacket? {
            if (removeArmorTrimsCheck(packet1.stack)) {
                return Fuck.create(ScreenHandlerSlotUpdateS2CPacket::class.java).apply {
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
                    Fuck.create(EntityEquipmentUpdateS2CPacket::class.java).apply {
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
                { packet2 -> packet2.contents },
                { packet2, list ->
                    Fuck.create(InventoryS2CPacket::class.java).apply {
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
            for ((i1, value) in list.withIndex()) {
                val stack1 = getStack(value)
                if (removeArmorTrimsCheck(stack1)) {
                    val array = (list as java.util.Collection<T>).toArray() as Array<T>
                    array[i1] = updateStack(array[i1], removeArmorTrimsApply(stack1))
                    for (i2 in (i1 + 1) until array.size) {
                        val stack2 = getStack(array[i2])
                        if (removeArmorTrimsCheck(stack2)) {
                            array[i2] = updateStack(array[i2], removeArmorTrimsApply(stack2))
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

}
