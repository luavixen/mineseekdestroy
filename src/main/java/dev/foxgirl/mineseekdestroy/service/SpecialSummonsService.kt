package dev.foxgirl.mineseekdestroy.service

import com.google.common.collect.ImmutableSet
import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Inventories
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.Scheduler
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.boss.CommandBossBar
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.Items.*
import net.minecraft.nbt.NbtByte
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtOps
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionUtil
import net.minecraft.potion.Potions
import net.minecraft.screen.AnvilScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.lang.Exception
import java.time.Duration
import java.time.Instant

class SpecialSummonsService : Service() {

    private enum class Theology {
        DEEP, OCCULT, COSMOS, BARTER, FLAME;
    }

    private class TheologyPair(theology1: Theology, theology2: Theology) {
        val theology1 = minOf(theology1, theology2)
        val theology2 = maxOf(theology1, theology2)

        val isDouble get() = theology1 === theology2
        val isOnce get() = pairsOnce.contains(this)

        override fun toString() = "TheologyPair(theology1=$theology1, theology2=$theology2)"

        override fun hashCode() = 31 * theology1.hashCode() + theology2.hashCode()
        override fun equals(other: Any?) =
            other === this || (other is TheologyPair && other.theology1 === theology1 && other.theology2 === theology2)

        companion object {
            val pairsDouble = setOf(
                TheologyPair(DEEP, DEEP),
                TheologyPair(OCCULT, OCCULT),
                TheologyPair(COSMOS, COSMOS),
                TheologyPair(BARTER, BARTER),
                TheologyPair(FLAME, FLAME),
            )
            val pairsOnce = setOf(
                TheologyPair(DEEP, OCCULT),
                TheologyPair(DEEP, COSMOS),
                TheologyPair(DEEP, BARTER),
                TheologyPair(OCCULT, BARTER),
            )
        }
    }

    private class Altar(
        val pos: BlockPos,
        val theology: Theology,
    )

    private class Options(
        val kind: TheologyPair,
        val altar: Altar,

        val player: GamePlayer,
        val team: GameTeam = player.team,
    ) {
        val pos: BlockPos get() = player.entity?.blockPos ?: altar.pos
    }

    private abstract inner class Summon(protected val options: Options) {
        val kind get() = options.kind
        val altar get() = options.altar
        val player get() = options.player
        val team get() = options.team
        val pos get() = options.pos

        open fun timeout(): Duration = Duration.ZERO

        open fun perform() {}
        open fun update() {}
    }

    private interface Stoppable {
        fun stop()
    }

    private fun ServerPlayerEntity.giveItem(stack: ItemStack) {
        if (!giveItemStack(stack)) dropItem(stack, false)?.let { it.resetPickupDelay(); it.setOwner(uuid) }
    }

    private fun ServerPlayerEntity.removeItem(predicate: (ItemStack) -> Boolean) {
        val inventory = inventory
        for (i in 0 until inventory.size()) {
            if (predicate(inventory.getStack(i))) inventory.setStack(i, ItemStack.EMPTY)
        }
    }

    private inner class DeepDeepSummon(options: Options) : Summon(options) {
        override fun perform() {
            val slices = iterator {
                val (start, end) = properties.regionFlood
                for (y in start.y..end.y) {
                    yield(Region(
                        BlockPos(start.x, y, start.z),
                        BlockPos(end.x, y, end.z),
                    ))
                }
            }
            Scheduler.interval(0.5) { schedule ->
                if (slices.hasNext()) {
                    Editor.edit(world, slices.next()) { state, _, _, _ ->
                        if (state.contains(Properties.WATERLOGGED)) {
                            state.with(Properties.WATERLOGGED, true)
                        } else if (state.isAir) {
                            Blocks.WATER.defaultState
                        } else {
                            null
                        }
                    }
                } else {
                    schedule.cancel()
                }
            }
        }
    }

    private inner class DeepOccultSummon(options: Options) : Summon(options), Stoppable {
        override fun perform() {
            val targets = playersIn.filter { it.team !== team }
            val targetsPool = targets.toMutableList().apply { shuffle() }

            fun target() = targetsPool.removeLastOrNull() ?: targets.random()

            val nbtSpawn = NbtHelper.fromBlockPos(properties.positionSpawn)
            val nbtDimension = World.CODEC.encodeStart(NbtOps.INSTANCE, world.registryKey).result().get()

            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== team) continue

                val target = target()

                val nameText = Text.literal("Sound of ").formatted(Formatting.GREEN).append(target.displayName)
                val nameJSON = Text.Serializer.toJson(nameText)

                val stack = ItemStack(COMPASS)

                stack.getOrCreateNbt().also {
                    it.put("LodestonePos", nbtSpawn)
                    it.put("LodestoneDimension", nbtDimension)
                    it.put("LodestoneTracked", NbtByte.ONE)
                    it.putUuid("MsdTargetPlayer", target.uuid)
                }
                stack.getOrCreateSubNbt("display").putString("Name", nameJSON)

                entity.giveItem(stack)
            }
        }
        override fun update() {
            for ((_, entity) in playerEntitiesNormal) {
                for (stack in Inventories.list(entity.inventory)) {
                    if (stack.item !== COMPASS) continue

                    val nbt = stack.nbt
                    if (nbt == null || !nbt.containsUuid("MsdTargetPlayer")) continue

                    val target = context.getPlayer(nbt.getUuid("MsdTargetPlayer")) ?: continue
                    val targetEntity = target.entity ?: continue

                    nbt.put("LodestonePos", NbtHelper.fromBlockPos(targetEntity.blockPos))
                }
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeItem { it.item === COMPASS }
            }
        }
    }

    private inner class DeepCosmosSummon(options: Options) : Summon(options) {
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isBeingRainedOn && !entity.hasStatusEffect(StatusEffects.POISON)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.POISON, 40))
                }
            }
        }
    }

    private inner class DeepBarterSummon(options: Options) : Summon(options) {
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isTouchingWater && !entity.hasStatusEffect(StatusEffects.POISON)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.POISON, 60))
                }
            }
        }
    }

    private inner class DeepFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) {
                    entity.giveItem(ItemStack(FLINT_AND_STEEL).apply { addEnchantment(Enchantments.UNBREAKING, 3) })
                    entity.giveItem(ItemStack(ANVIL))
                }
            }
        }
    }

    private inner class OccultOccultSummon(options: Options) : Summon(options) {
        override fun perform() {
            for (player in players) {
                if (player.team === GameTeam.PLAYER_BLACK) player.kills += 2
            }
            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== GameTeam.PLAYER_BLACK) entity.health /= 2
            }
        }
    }

    private inner class OccultCosmosSummon(options: Options) : Summon(options), Stoppable {
        override fun perform() {
            for ((player, entity) in playerEntitiesIn) {
                if (player.team === team) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.NIGHT_VISION, 20000000))
                } else {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.BLINDNESS, 30 * 20))
                }
            }
        }
        override fun update() {
            for ((_, entity) in playerEntitiesOut) {
                entity.removeStatusEffect(StatusEffects.NIGHT_VISION)
                entity.removeStatusEffect(StatusEffects.BLINDNESS)
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeStatusEffect(StatusEffects.NIGHT_VISION)
                entity.removeStatusEffect(StatusEffects.BLINDNESS)
            }
        }
    }

    private inner class OccultBarterSummon(options: Options) : Summon(options), Stoppable {
        override fun perform() {
            val item = ItemStack(GOLDEN_SWORD).apply {
                addEnchantment(Enchantments.SHARPNESS, 50)
                setDamage(32)
            }
            for ((player, entity) in playerEntitiesIn) {
                if (player.team === team) entity.giveItem(item.copy())
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeItem { it.item === GOLDEN_SWORD }
            }
        }
    }

    private inner class OccultFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            val region = properties.regionBlimp
            val position = BlockPos(region.center.x.toInt(), region.start.y - 7, region.center.z.toInt())
            EntityType.GHAST.spawn(world, position, SpawnReason.COMMAND)
        }
    }

    private inner class CosmosCosmosSummon(options: Options) : Summon(options) {
        override fun update() {
            for ((_, entity) in playerEntitiesNormal) {
                if (!entity.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOW_FALLING, 20000000))
                }
                if (!entity.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.JUMP_BOOST, 20000000))
                }
            }
        }
    }

    private inner class CosmosBarterSummon(options: Options) : Summon(options) {
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.giveItem(ItemStack(COOKED_BEEF, 8))
            }
        }
    }

    private inner class CosmosFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.absorptionAmount += 2.0F
            }
        }
    }

    private inner class BarterBarterSummon(options: Options) : Summon(options) {
        override fun perform() {
            val items = setOf<Item>(
                SHIELD, CARROT_ON_A_STICK, FISHING_ROD, TIPPED_ARROW,
                COOKED_BEEF, GOLDEN_SWORD, FLINT_AND_STEEL,
                ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
            )
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeItem { items.contains(it.item) }
            }
        }
    }

    private inner class BarterFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.giveItem(ItemStack(SNOW_BLOCK, 64))
            }
        }
    }

    private inner class FlameFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            Editor
                .edit(world, properties.regionPlayable) { state, _, _, _ ->
                    if (state.contains(Properties.WATERLOGGED)) {
                        return@edit state.with(Properties.WATERLOGGED, false)
                    }
                    if (state.block === Blocks.WATER) {
                        return@edit Blocks.AIR.defaultState
                    }
                    return@edit null
                }
        }
    }

    private val summons = mapOf<TheologyPair, (Options) -> Summon>(
        TheologyPair(DEEP, DEEP) to ::DeepDeepSummon,
        TheologyPair(DEEP, OCCULT) to ::DeepOccultSummon,
        TheologyPair(DEEP, COSMOS) to ::DeepCosmosSummon,
        TheologyPair(DEEP, BARTER) to ::DeepBarterSummon,
        TheologyPair(DEEP, FLAME) to ::DeepFlameSummon,
        TheologyPair(OCCULT, OCCULT) to ::OccultOccultSummon,
        TheologyPair(OCCULT, COSMOS) to ::OccultCosmosSummon,
        TheologyPair(OCCULT, BARTER) to ::OccultBarterSummon,
        TheologyPair(OCCULT, FLAME) to ::OccultFlameSummon,
        TheologyPair(COSMOS, COSMOS) to ::CosmosCosmosSummon,
        TheologyPair(COSMOS, BARTER) to ::CosmosBarterSummon,
        TheologyPair(COSMOS, FLAME) to ::CosmosFlameSummon,
        TheologyPair(BARTER, BARTER) to ::BarterBarterSummon,
        TheologyPair(BARTER, FLAME) to :: BarterFlameSummon,
        TheologyPair(FLAME, FLAME) to ::FlameFlameSummon,
    )

    private val summonListGame = mutableListOf<Summon>()
    private val summonListRound = mutableListOf<Summon>()

    private var altars = mapOf<BlockPos, Altar>()

    private var timeout = Instant.now()

    var isScaldingEarth = false; private set
    var isPollutedWater = false; private set
    var isAcidRain = false; private set
    var isTracking = false; private set

    override fun setup() {
        fun findTheologyAt(pos: BlockPos): Theology? {
            return when (world.getBlockState(pos).block) {
                Blocks.WARPED_PLANKS -> DEEP
                Blocks.DARK_OAK_TRAPDOOR -> OCCULT
                Blocks.OBSIDIAN -> COSMOS
                Blocks.WAXED_CUT_COPPER -> BARTER
                Blocks.NETHER_BRICKS -> FLAME
                else -> null
            }
        }
        fun findTheology(pos: BlockPos): Theology? =
            findTheologyAt(pos.up()) ?: findTheologyAt(pos.down())

        Editor
            .search(world, properties.regionAll) {
                it.block === Blocks.FLETCHING_TABLE
            }
            .thenAccept { results ->
                logger.info("SpecialSummonService search for altars returned ${results.size} result(s)")
                altars = results
                    .mapNotNull { Altar(it.pos, findTheology(it.pos) ?: return@mapNotNull null) }
                    .associateBy { it.pos }
            }
    }

    private fun timeoutReset() {
        timeout = Instant.now()
    }

    private fun timeoutSet(duration: Duration) {
        timeout = Instant.now().plus(duration)
    }

    private fun timeoutCheck(): Boolean {
        return timeout.isAfter(Instant.now())
    }

    private fun failPerform(options: Options) {
        for (player in players) {
            if (player.team === options.team) {
                player.entity?.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 80))
            }
        }
        when (options.altar.theology) {
            DEEP -> {
                EntityType.GUARDIAN.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            OCCULT -> {
                for (i in 0..5) EntityType.ZOMBIE.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            COSMOS -> {
                for (i in 0..2) EntityType.PHANTOM.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            BARTER -> {
                EntityType.ILLUSIONER.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            FLAME -> {
                for (i in 0..2) EntityType.BLAZE.spawn(world, options.pos, SpawnReason.COMMAND)
            }
        }
        world.playSound(
            null,
            options.pos,
            SoundEvents.ENTITY_HORSE_DEATH,
            SoundCategory.PLAYERS,
            3.0F, 0.5F,
        )
    }

    private fun failCheck(options: Options): Boolean {
        if (timeoutCheck()) {
            Game.CONSOLE_PLAYERS.sendError("Summon failed due to timeout")
            return true
        }

        val kind = options.kind
        if (kind.theology1 !== options.altar.theology && kind.theology2 !== options.altar.theology) {
            Game.CONSOLE_PLAYERS.sendError("Summon failed due to unmatched theology")
            return true
        }
        if (kind.isDouble && summonListGame.any { it.kind == kind }) {
            Game.CONSOLE_PLAYERS.sendError("Summon failed due to repeated double")
            return true
        }
        if (kind.isOnce && summonListRound.any { it.kind == kind }) {
            Game.CONSOLE_PLAYERS.sendError("Summon failed due to repeated once")
            return true
        }
        if (kind == summonListRound.lastOrNull()?.kind) {
            Game.CONSOLE_PLAYERS.sendError("Summon failed due to repeated back-to-back")
            return true
        }

        return false
    }

    private fun summonPerform(options: Options) {
        val summon = summons[options.kind]!!.invoke(options)
        summonListGame.add(summon)
        summonListRound.add(summon)
        summon.perform()
        timeoutSet(summon.timeout())
    }

    private fun summonEffects(options: Options) {
        EntityType.LIGHTNING_BOLT.spawn(world, options.pos, SpawnReason.COMMAND)
    }

    private fun summon(options: Options) {
        Game.CONSOLE_PLAYERS.sendInfo("Player", options.player.displayName, "attempting summon of type", options.kind, "with altar at", options.altar.theology, "with theology", options.altar.theology)
        if (failCheck(options)) {
            Game.CONSOLE_PLAYERS.sendError("Summon failed!")
            failPerform(options)
        } else {
            Game.CONSOLE_PLAYERS.sendError("Summon success!")
            summonPerform(options)
        }
        summonEffects(options)
    }

    override fun update() {
        summonListGame.forEach { it.update() }

        val active = summonListGame.map { it.kind }.toSet()
        isScaldingEarth = active.contains(TheologyPair(FLAME, FLAME))
        isPollutedWater = active.contains(TheologyPair(DEEP, BARTER))
        isAcidRain = active.contains(TheologyPair(DEEP, COSMOS))
        isTracking = active.contains(TheologyPair(DEEP, OCCULT))
    }

    fun handleRoundEnd() {
        summonListRound.forEach {
            if (it is Stoppable) {
                Game.CONSOLE_PLAYERS.sendInfo("Stopping summon", it)
                it.stop()
                summonListGame.remove(it)
            }
        }
        summonListRound.clear()
        timeoutReset()
    }

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        // TODO: these are all horrible hacks

        val altar: Altar? = altars[pos]
        if (altar == null) {
            Game.CONSOLE_PLAYERS.sendInfo("Player", player.displayName, "failed to open invalid altar at", pos)
            return ActionResult.PASS
        }

        Game.CONSOLE_PLAYERS.sendInfo("Player", player.displayName, "opened altar at", pos, "with theology", altar.theology)

        player.entity!!.openHandledScreen(object : NamedScreenHandlerFactory {

            override fun getDisplayName() = Text.of("Altar of ${altar.theology}")

            override fun createMenu(syncId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): ScreenHandler {
                return object : AnvilScreenHandler(syncId, playerInventory) {

                    private fun pair(): TheologyPair {
                        val a1 = input.getStack(0)
                        val a2 = input.getStack(1)
                        val p1 = PotionUtil.getPotion(a1)
                        val p2 = PotionUtil.getPotion(a2)
                        val e1 = p1.effects
                        val e2 = p2.effects
                        val ee1 = e1.first().effectType
                        val ee2 = e2.first().effectType
                        val t1 = effectToTheologyMap[ee1]!!
                        val t2 = effectToTheologyMap[ee2]!!
                        return TheologyPair(t1, t2)
                    }

                    override fun updateResult() {
                        output.setStack(0,
                            try {
                                ItemStack(STONE).setCustomName(Text.of(pair().toString()))
                            } catch (ignored : Exception) {
                                ItemStack.EMPTY
                            }
                        )
                    }

                    override fun onTakeOutput(playerEntity: PlayerEntity, stack: ItemStack) {
                        summon(Options(pair(), altar, player))
                        input.setStack(0, ItemStack.EMPTY)
                        input.setStack(1, ItemStack.EMPTY)
                    }

                    override fun canTakeOutput(playerEntity: PlayerEntity, present: Boolean): Boolean {
                        val a1 = input.getStack(0)
                        val a2 = input.getStack(1)
                        if (a1.item !== TIPPED_ARROW || a2.item !== TIPPED_ARROW) return false
                        val p1 = PotionUtil.getPotionEffects(a1).firstOrNull()
                        val p2 = PotionUtil.getPotionEffects(a2).firstOrNull()
                        if (!effectToTheologyMap.keys.contains(p1?.effectType)) return false
                        if (!effectToTheologyMap.keys.contains(p2?.effectType)) return false
                        return true
                    }

                    override fun canUse(player: PlayerEntity?) = true
                    override fun canUse(state: BlockState?) = true

                    override fun canInsertIntoSlot(slot: Slot?) = true
                    override fun canInsertIntoSlot(stack: ItemStack?, slot: Slot?) = true

                    override fun setNewItemName(newItemName: String?) {
                    }

                    private val effectToTheologyMap = mapOf<StatusEffect, Theology>(
                        StatusEffects.WATER_BREATHING to DEEP,
                        StatusEffects.INSTANT_HEALTH to OCCULT,
                        StatusEffects.INVISIBILITY to COSMOS,
                        StatusEffects.STRENGTH to BARTER,
                        StatusEffects.FIRE_RESISTANCE to FLAME,
                    )

                }
            }

        })

        return ActionResult.FAIL
    }

}
