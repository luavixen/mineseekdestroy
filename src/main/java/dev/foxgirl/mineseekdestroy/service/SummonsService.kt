package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import dev.foxgirl.mineseekdestroy.util.async.await
import dev.foxgirl.mineseekdestroy.util.async.terminate
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.state.property.Properties
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.World
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

class SummonsService : Service() {

    enum class Theology {
        DEEP { override val color get() = Formatting.DARK_AQUA },
        OCCULT { override val color get() = Formatting.LIGHT_PURPLE },
        COSMOS { override val color get() = Formatting.BLUE },
        BARTER { override val color get() = Formatting.YELLOW },
        FLAME { override val color get() = Formatting.RED },
        OPERATOR { override val color get() = Formatting.GREEN };

        abstract val color: Formatting

        val displayName: Text get() = text(name) * color
    }

    class Prayer(theology1: Theology, theology2: Theology) {
        val theology1: Theology
        val theology2: Theology

        init {
            if (theology1 <= theology2) {
                this.theology1 = theology1
                this.theology2 = theology2
            } else {
                this.theology1 = theology2
                this.theology2 = theology1
            }
        }

        val isDouble get() = theology1 === theology2

        val isOncePerGame get() = prayersOncePerGame.contains(this)
        val isOncePerRound get() = prayersOncePerRound.contains(this)

        val displayName: Text get() {
            val text = Text.empty()
                .append(theology1.displayName)
                .append(text(" X ").green())
                .append(theology2.displayName)
            val stack = summonIconsReference?.get(this)
            if (stack != null) text.style { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(stack))) }
            return text
        }

        override fun toString() = "Theologies(theology1=${theology1}, theology2=${theology2})"

        override fun hashCode() = 31 * theology1.hashCode() + theology2.hashCode()
        override fun equals(other: Any?) =
            other === this || (other is Prayer && other.theology1 === theology1 && other.theology2 === theology2)
    }

    private class Altar(
        val pos: BlockPos,
        val theology: Theology,
    )

    private class Options(
        val kind: Prayer,
        val altar: Altar,
        val player: GamePlayer,
    ) {
        val team = player.team
        val pos = player.entity?.blockPos ?: altar.pos
    }

    private enum class State { WAITING, READY, DEAD }

    private abstract inner class Summon(val options: Options) {
        val kind get() = options.kind
        val altar get() = options.altar
        val player get() = options.player
        val team get() = options.team
        val pos get() = options.pos

        val displayName: Text get() = kind.displayName

        open val timeout: Duration get() = Duration.ZERO
        open val isRoundOnly: Boolean get() = false

        protected open fun perform() {}
        protected open fun update() {}
        protected open fun stop() {}

        private var state = State.WAITING

        val isWaiting get() = state === State.WAITING
        val isReady get() = state === State.READY

        private inline fun tryAction(verb: String, action: () -> Unit): Boolean {
            return try {
                action()
                true
            } catch (cause : Exception) {
                Game.CONSOLE_OPERATORS.sendError("Summon encountered exception while $verb:", this.displayName)
                Game.LOGGER.error("SummonsService exception while $verb ${javaClass.simpleName}", cause)
                false
            }
        }

        fun tryPerform(): Boolean {
            if (isWaiting) {
                val success = tryAction("performing", ::perform)
                if (success) {
                    state = State.READY
                    Game.CONSOLE_OPERATORS.sendInfo("Summon performed:", this.displayName)
                }
                return success
            }
            return false
        }
        fun tryStop(): Boolean {
            if (isReady) {
                val success = tryAction("stopping", ::stop)
                if (success) {
                    state = State.DEAD
                    Game.CONSOLE_OPERATORS.sendInfo("Summon stopped:", this.displayName)
                }
                return success
            }
            return false
        }
        fun tryUpdate(): Boolean {
            if (isReady) {
                return tryAction("updating", ::update)
            }
            return false
        }

        fun destroy(): Boolean {
            summonListGame.remove(this)
            summonListRound.remove(this)
            return tryStop()
        }
    }

    private inner class DeepDeepSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(30)
        override fun perform() {
            val blocks = immutableSetOf<Block>(
                Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
                Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING,
                Blocks.MANGROVE_PROPAGULE, Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
                Blocks.FERN, Blocks.LARGE_FERN, Blocks.DEAD_BUSH,
                Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
                Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP,
                Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY,
                Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.WITHER_ROSE,
                Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.CRIMSON_FUNGUS,
                Blocks.WARPED_FUNGUS, Blocks.CRIMSON_ROOTS, Blocks.WARPED_ROOTS,
                Blocks.NETHER_SPROUTS, Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT,
                Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.SUGAR_CANE,
                Blocks.TORCH, Blocks.WALL_TORCH, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
                Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.VINE,
                Blocks.GLOW_LICHEN, Blocks.LILY_PAD, Blocks.SCULK_VEIN,
                Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY,
                Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET,
                Blocks.LIGHT_BLUE_CARPET, Blocks.YELLOW_CARPET, Blocks.LIME_CARPET,
                Blocks.PINK_CARPET, Blocks.GRAY_CARPET, Blocks.LIGHT_GRAY_CARPET,
                Blocks.CYAN_CARPET, Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET,
                Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET, Blocks.BLACK_CARPET,
                Blocks.RAIL, Blocks.ACTIVATOR_RAIL, Blocks.DETECTOR_RAIL, Blocks.POWERED_RAIL,
                Blocks.WHEAT, Blocks.CARROTS, Blocks.PUMPKIN_STEM, Blocks.MELON_STEM,
                Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM,
                Blocks.BEETROOTS, Blocks.POTATOES, Blocks.SUGAR_CANE, Blocks.COCOA,
                Blocks.TRIPWIRE,
            )

            Async.go {
                val (start, end) = properties.regionFlood
                var y = start.y; while (y < end.y) {
                    delay(3.0); if (!isReady) break

                    val yMin = (y)
                    val yMax = (y + 3).coerceAtMost(end.y)
                    y = yMax

                    val region = Region(
                        BlockPos(start.x, yMin, start.z),
                        BlockPos(end.x, yMax, end.z),
                    )

                    logger.info("DeepDeepSummon performing edit for ${region}")

                    Editor
                        .queue(world, region)
                        .edit { state, _, _, _ ->
                            if (state.isAir || blocks.contains(state.block)) {
                                Blocks.WATER.defaultState
                            } else if (state.contains(Properties.WATERLOGGED)) {
                                state.with(Properties.WATERLOGGED, true)
                            } else {
                                null
                            }
                        }
                        .await()
                }
                logger.info("DeepDeepSummon completed")
            }

            world.setWeatherRain()
        }
        override fun stop() {
            Editor
                .queue(world, properties.regionFlood)
                .edit { state, _, _, _ ->
                    if (state.block === Blocks.WATER) {
                        return@edit Blocks.AIR.defaultState
                    }
                    if (state.contains(Properties.WATERLOGGED)) {
                        return@edit state.with(Properties.WATERLOGGED, false)
                    }
                    return@edit null
                }
                .terminate()

            world.setWeatherClear()
        }
    }

    private inner class DeepOccultSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(60)
        override val isRoundOnly get() = true
        override fun perform() {
            val targets = playersIn.filter { it.team !== team }
            val targetsPool = targets.toMutableList().apply { shuffle() }

            logger.info("DeepOccultSummon found ${targets.size} targets")

            if (targets.isEmpty()) return

            fun target() = targetsPool.removeLastOrNull() ?: targets.random()

            val nbtSpawn = NbtHelper.fromBlockPos(properties.positionSpawn)
            val nbtDimension = World.CODEC.encodeStart(NbtOps.INSTANCE, world.registryKey).result().get()

            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== team) continue

                val target = target()

                entity.give(GameItems.summonCompass.copy().apply {
                    dataDisplay()["Name"] = toNbt((text("Sound of ") + target).mnsndItemName())
                    data()["LodestonePos"] = nbtSpawn
                    data()["LodestoneDimension"] = nbtDimension
                    data()["LodestoneTracked"] = true
                    data()["MsdTargetPlayer"] = target.uuid
                })

                logger.info("DeepOccultSummon gave tracker for ${target.nameQuoted} to ${player.nameQuoted}")
            }
        }
        override fun update() {
            for ((_, entity) in playerEntitiesNormal) {
                for (stack in entity.inventory.asList()) {
                    if (stack.item !== COMPASS) continue

                    val nbt = stack.nbt
                    if (nbt == null || !nbt.containsUuid("MsdTargetPlayer")) continue

                    val target = context.getPlayer(nbt.getUuid("MsdTargetPlayer")) ?: continue
                    val targetEntity = target.entity ?: continue

                    nbt["LodestonePos"] = NbtHelper.fromBlockPos(targetEntity.blockPos)
                }
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.inventory.removeAll { it.item === COMPASS }
            }
        }
    }

    private inner class DeepCosmosSummon(options: Options) : Summon(options) {
        override val isRoundOnly get() = true
        override fun perform() {
            world.setWeatherRain()
        }
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isBeingRainedOn && !entity.hasEffect(StatusEffects.POISON)) {
                    entity.addEffect(StatusEffects.POISON, 2.0)
                }
            }
        }
        override fun stop() {
            world.setWeatherClear()
        }
    }

    private inner class DeepBarterSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(30)
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isTouchingWater && !entity.hasEffect(StatusEffects.POISON)) {
                    entity.addEffect(StatusEffects.POISON, 3.0)
                }
            }
        }
    }

    private inner class DeepFlameSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) {
                    entity.give(GameItems.summonWaterBucket.copy())
                    entity.give(GameItems.summonChippedAnvil.copy())
                }
            }
        }
    }

    private inner class OccultOccultSummon(options: Options) : Summon(options) {
        override fun perform() {
            val playersAscended = mutableSetOf<GamePlayer>()

            for (player in players) {
                if (player.team === GameTeam.BLACK) {
                    player.team = GameTeam.SKIP
                    playersAscended.add(player)
                    context.damageService.addRecord(player, 100.0F)
                }
            }

            for ((player, entity) in playerEntitiesNormal) {
                if (!player.isAlive || player in playersAscended) continue

                val source: DamageSource
                val amount: Float

                if (player.isGhost) {
                    source = world.damageSources.create(Game.DAMAGE_TYPE_ABYSS)
                    amount = 999999.0F
                } else if (player.team !== GameTeam.BLACK) {
                    source = world.damageSources.create(Game.DAMAGE_TYPE_ABYSS)
                    amount = Math.max(entity.health - 0.5F, 0.0F)
                } else {
                    continue
                }

                Scheduler.now { entity.damage(source, amount) }
            }
        }
    }

    private inner class OccultCosmosSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(90)
        override fun perform() {
            summonListGame.find { it.kind == Prayer(COSMOS, FLAME) }?.destroy()
            Broadcast.sendSound(SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.0F, 1.0F, world, properties.borderCenter)
            for (pos in BlockPos.ofFloored(properties.borderCenter).around(7.0)) {
                world.setBlockState(pos, (if (Random.nextBoolean()) Blocks.SNOW_BLOCK else Blocks.BONE_BLOCK).defaultState)
            }
        }
    }

    private inner class OccultBarterSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(90)
        override val isRoundOnly get() = true
        override fun perform() {
            for ((player, entity) in playerEntitiesIn) {
                if (player.team === team) entity.give(GameItems.summonGoldenSword.copy())
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.inventory.removeAll { it.item === GOLDEN_SWORD }
            }
        }
    }

    private inner class OccultFlameSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(30)
        override fun perform() {
            val region = properties.regionBlimp
            val position = BlockPos(region.center.x.toInt(), region.start.y - 7, region.center.z.toInt())
            for (i in 1..3) EntityType.GHAST.spawn(world, position, SpawnReason.COMMAND)
        }
    }

    private inner class CosmosCosmosSummon(options: Options) : Summon(options) {
        override val isRoundOnly get() = true
        override fun update() {
            for ((_, entity) in playerEntitiesNormal) {
                if (!entity.hasEffect(StatusEffects.SLOW_FALLING)) {
                    entity.addEffect(StatusEffects.SLOW_FALLING, 4.5)
                }
                if (!entity.hasEffect(StatusEffects.JUMP_BOOST)) {
                    entity.addEffect(StatusEffects.JUMP_BOOST, 4.5, 5)
                }
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeEffect(StatusEffects.SLOW_FALLING)
                entity.removeEffect(StatusEffects.JUMP_BOOST)
            }
        }
    }

    private inner class CosmosBarterSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.give(GameItems.summonSteak.copyWithCount(8))
            }
        }
    }

    private inner class CosmosFlameSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(60)
        override fun perform() {
            summonListGame.find { it.kind == Prayer(COSMOS, OCCULT) }?.destroy()
            Broadcast.sendSound(SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 2.0F, 1.0F, world, properties.borderCenter)
            for (pos in BlockPos.ofFloored(properties.borderCenter).around(7.0)) {
                world.setBlockState(pos, Blocks.LAVA.defaultState)
            }
        }
        /*
        override fun update() {
            val center = BlockPos.ofFloored(properties.borderCenter)
            val region = center.let {
                Region(
                    it.add(+7, +7, +7),
                    it.add(-7, -7, -7),
                )
            }
            val positions = center.around(7.0).toHashSet()
            Editor
                .queue(world, region)
                .edit { _, x, y, z -> if (BlockPos(x, y, z) in positions) Blocks.FIRE.defaultState else null }
                .terminate()
        }
        */
    }

    private inner class BarterBarterSummon(options: Options) : Summon(options) {
        override fun perform() {
            val items = immutableSetOf<Item>(
                SHIELD, CARROT_ON_A_STICK, FISHING_ROD, TIPPED_ARROW,
                BOOK, ENCHANTED_BOOK, KNOWLEDGE_BOOK, WRITABLE_BOOK, WRITTEN_BOOK,
                WHITE_BANNER, PAPER,
                COOKED_BEEF, GOLDEN_SWORD, FLINT_AND_STEEL, WATER_BUCKET,
                COMPASS, TARGET, FIREWORK_ROCKET, ENDER_PEARL, BLUE_ICE,
                RECOVERY_COMPASS, HONEY_BOTTLE,
                ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
            )

            for ((_, entity) in playerEntitiesNormal) {
                entity.inventory.removeAll { items.contains(it.item) }
            }

            for ((player, entity) in playerEntitiesNormal) {
                if (player.isGhost) {
                    Scheduler.now { entity.damage(world.damageSources.create(Game.DAMAGE_TYPE_ABYSS), 5000.0F) }
                }
            }

            val kinds = immutableListOf(Prayer(FLAME, FLAME), Prayer(DEEP, DEEP), Prayer(COSMOS, COSMOS))
            val summons = summonListGame.filter { kinds.contains(it.kind) }

            summons.forEach { it.destroy() }
        }
    }

    private inner class BarterFlameSummon(options: Options) : Summon(options) {
        override val timeout get() = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.give(GameItems.summonBlueIce.copyWithCount(64))
            }
        }
    }

    private inner class FlameFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            val blocks = immutableSetOf<Block>(
                Blocks.WATER,
                Blocks.SEAGRASS, Blocks.TALL_SEAGRASS,
                Blocks.KELP, Blocks.KELP_PLANT,
            )

            Editor
                .queue(world, properties.regionPlayable)
                .edit { state, _, _, _ ->
                    if (blocks.contains(state.block)) {
                        return@edit Blocks.AIR.defaultState
                    }
                    if (state.contains(Properties.WATERLOGGED)) {
                        return@edit state.with(Properties.WATERLOGGED, false)
                    }
                    return@edit null
                }
                .terminate()

            game.setRuleBoolean(GameRules.DO_FIRE_TICK, true)
            world.setWeatherClear()
        }
        override fun stop() {
            game.setRuleBoolean(GameRules.DO_FIRE_TICK, false)
        }
    }

    private val summons = immutableMapOf<Prayer, (Options) -> Summon>(
        Prayer(DEEP, DEEP) to ::DeepDeepSummon,
        Prayer(DEEP, OCCULT) to ::DeepOccultSummon,
        Prayer(DEEP, COSMOS) to ::DeepCosmosSummon,
        Prayer(DEEP, BARTER) to ::DeepBarterSummon,
        Prayer(DEEP, FLAME) to ::DeepFlameSummon,
        Prayer(OCCULT, OCCULT) to ::OccultOccultSummon,
        Prayer(OCCULT, COSMOS) to ::OccultCosmosSummon,
        Prayer(OCCULT, BARTER) to ::OccultBarterSummon,
        Prayer(OCCULT, FLAME) to ::OccultFlameSummon,
        Prayer(COSMOS, COSMOS) to ::CosmosCosmosSummon,
        Prayer(COSMOS, BARTER) to ::CosmosBarterSummon,
        Prayer(COSMOS, FLAME) to ::CosmosFlameSummon,
        Prayer(BARTER, BARTER) to ::BarterBarterSummon,
        Prayer(BARTER, FLAME) to :: BarterFlameSummon,
        Prayer(FLAME, FLAME) to ::FlameFlameSummon,
    )

    private val summonListGame = mutableListOf<Summon>()
    private val summonListRound = mutableListOf<Summon>()

    private var altars = mapOf<BlockPos, Altar>()

    private var timeout = Instant.now()
    private var timeoutDuration = Duration.ZERO

    private var textProvider = null as TextProvider?
    private var textLastUpdate = Instant.now()

    var isScaldingEarth = false; private set
    var isFlashFlood = false; private set
    var isPollutedWater = false; private set
    var isAcidRain = false; private set
    var isTracking = false; private set

    override fun setup() {
        fun findTheologyAt(pos: BlockPos): Theology? {
            return when (world.getBlockState(pos).block) {
                Blocks.WARPED_PLANKS -> DEEP
                Blocks.DARK_OAK_TRAPDOOR -> OCCULT
                Blocks.BLACKSTONE -> COSMOS
                Blocks.GOLD_BLOCK -> BARTER
                Blocks.NETHER_BRICKS -> FLAME
                Blocks.COMMAND_BLOCK -> OPERATOR
                else -> null
            }
        }
        fun findTheology(pos: BlockPos): Theology? =
            findTheologyAt(pos.up()) ?: findTheologyAt(pos.down())

        Async.go {
            altars = Editor
                .queue(world, properties.regionAll)
                .search { it.block === Blocks.FLETCHING_TABLE }
                .await()
                .mapNotNull { Altar(it.pos, findTheology(it.pos) ?: return@mapNotNull null) }
                .associateBy { it.pos }

            logger.info("SummonService search for altars returned ${altars.size} result(s)")
        }
    }

    private fun timeoutReset() {
        timeout = Instant.now()
        timeoutDuration = Duration.ZERO
    }
    private fun timeoutSet(duration: Duration) {
        timeout = Instant.now().plus(duration)
        timeoutDuration = duration
    }
    private fun timeoutRemaining(): Duration {
        return Duration.between(Instant.now(), timeout)
    }
    private fun timeoutCheck(): Boolean {
        return timeout.isAfter(Instant.now())
    }

    private fun textUpdateFull() {
        val textProvider = textProvider
        if (textProvider != null) {
            Broadcast.send(TitleFadeS2CPacket(20, 80, 20))
            Broadcast.send(TitleS2CPacket(textProvider.title))
            Broadcast.send(SubtitleS2CPacket(textProvider.subtitle))
            if (Rules.summonsShowTooltipEnabled) {
                Broadcast.send(OverlayMessageS2CPacket(textProvider.tooltip))
            }
        }
        textTimeUpdate()
    }
    private fun textUpdateTooltip() {
        val textProvider = textProvider
        if (textProvider != null) {
            if (Rules.summonsShowTooltipEnabled) {
                Broadcast.send(OverlayMessageS2CPacket(textProvider.tooltip))
            }
        }
        textTimeUpdate()
    }
    private fun textClear() {
        textProvider = null
    }
    private fun textTimeUpdate() {
        textLastUpdate = Instant.now()
    }
    private fun textTimeCheck(): Boolean {
        return Duration.between(textLastUpdate, Instant.now()).toMillis() >= 500
    }

    private enum class FailureReason {
        TIMEOUT,
        MISMATCH,
        REPEATED_DOUBLE,
        REPEATED_ONCE,
        REPEATED_SEQUENCE,
        SPRINGTRAP,
    }

    private fun failCheck(options: Options): FailureReason? {
        if (Rules.chaosEnabled) {
            return null
        }

        if (options.altar.theology == OPERATOR) {
            return null
        }

        if (timeoutCheck()) {
            return FailureReason.TIMEOUT
        }

        val kind = options.kind

        if (kind.theology1 !== options.altar.theology && kind.theology2 !== options.altar.theology) {
            return FailureReason.MISMATCH
        }
        if (kind.isOncePerGame && summonListGame.any { it.kind == kind }) {
            return FailureReason.REPEATED_DOUBLE
        }
        if (kind.isOncePerRound && summonListRound.any { it.kind == kind }) {
            return FailureReason.REPEATED_ONCE
        }
        if (kind == summonListRound.lastOrNull()?.kind) {
            return FailureReason.REPEATED_SEQUENCE
        }
        if (kind == Prayer(DEEP, DEEP) && !Rules.summonsDeepdeepEnabled) {
            return FailureReason.SPRINGTRAP
        }

        return null
    }

    private fun failPerform(options: Options, reason: FailureReason) {
        for (player in players) {
            if (player.team === options.team) {
                player.entity?.addEffect(StatusEffects.GLOWING, 4.0)
            }
        }

        when (options.altar.theology) {
            DEEP -> {
                if (reason == FailureReason.SPRINGTRAP) {
                    val springtrap = EntityType.ZOMBIE.create(world)!!.also {
                        it.equipStack(EquipmentSlot.FEET, ItemStack.fromNbt(nbtDecode("""{id: "minecraft:leather_boots", tag: {Damage: 0, Trim: {material: "minecraft:redstone", pattern: "minecraft:rib"}, display: {color: 7301926}}, Count: 1b}""").asCompound()))
                        it.equipStack(EquipmentSlot.LEGS, ItemStack.fromNbt(nbtDecode("""{id: "minecraft:leather_leggings", tag: {Damage: 0, Trim: {material: "minecraft:redstone", pattern: "minecraft:rib"}, display: {color: 7301926}}, Count: 1b}""").asCompound()))
                        it.equipStack(EquipmentSlot.CHEST, ItemStack.fromNbt(nbtDecode("""{id: "minecraft:leather_chestplate", tag: {Damage: 0, Trim: {material: "minecraft:redstone", pattern: "minecraft:rib"}, display: {color: 7301926}}, Count: 1b}""").asCompound()))
                        it.equipStack(EquipmentSlot.HEAD, ItemStack.fromNbt(nbtDecode("""{id: "minecraft:player_head", tag: {display: {Name: '{"text":"Springtrap"}'}, SkullOwner: {Id: [I; -304140315, 173950771, -1692193734, 1546731912], Properties: {textures: [{Value: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjJmNjU2ZGVjYWQ1ZTNjNGZkMDllMzIyZGY3NjhlN2JkMTdhZWIzNTM3YjU4ZWNlYTI5OTBiZGU2ZWNmOSJ9fX0="}]}}}, Count: 1b}""").asCompound()))
                        it.equipStack(EquipmentSlot.MAINHAND, ItemStack.fromNbt(nbtDecode("""{id: "minecraft:mangrove_button", tag: {display: {Name: '[{"text":"Springlocks","italic":false}]'}, Enchantments: [{id: "sharpness", lvl: 255}]}, Count: 1b}""").asCompound()))
                        it.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!.baseValue = 50000.0
                        it.health = it.maxHealth
                        it.customName = text("Springtrap")
                        it.refreshPositionAndAngles(options.pos, 0.0F, 0.0F)
                    }
                    world.spawnEntityAndPassengers(springtrap)
                } else {
                    EntityType.GUARDIAN.spawn(world, options.pos, SpawnReason.COMMAND)
                }
            }
            OCCULT -> {
                for (i in 1..5) EntityType.ZOMBIE.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            COSMOS -> {
                for (i in 1..2) EntityType.PHANTOM.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            BARTER -> {
                EntityType.ILLUSIONER.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            FLAME -> {
                for (i in 1..2) EntityType.BLAZE.spawn(world, options.pos, SpawnReason.COMMAND)
            }
            OPERATOR -> {
            }
        }

        Broadcast.sendSound(
            SoundEvents.ENTITY_HORSE_DEATH,
            SoundCategory.PLAYERS,
            3.0F, 0.5F,
            world, options.pos.toCenterPos(),
        )
    }

    private fun summonPerform(options: Options) {
        val summon = summons[options.kind]!!.invoke(options)
        if (summon.tryPerform()) {
            summonListGame.add(summon)
            summonListRound.add(summon)
            timeoutSet(summon.timeout)
        }
    }

    private fun summonEffects(options: Options) {
        val lightning = EntityType.LIGHTNING_BOLT.create(world)!!
        lightning.setPosition(options.altar.pos.toCenterPos())
        lightning.setCosmetic(true)
        world.spawnEntity(lightning)

        Async.go {
            val shulker = EntityType.SHULKER.create(world)!!
            shulker.setPosition(options.altar.pos.let { Vec3d(it.x.toDouble() + 0.5, it.y.toDouble(), it.z.toDouble() + 0.5) })
            shulker.isAiDisabled = true
            shulker.isInvulnerable = true

            world.spawnEntity(shulker)
            context.scoreboard.addScoreHolderToTeam(shulker.scoreboardName, context.getTeam(GameTeam.OPERATOR))

            lifetime()
                .withCondition { shulker.isAlive }
                .go {
                    while (true) {
                        shulker.isInvisible = true
                        shulker.isGlowing = true
                        delay()
                    }
                }

            delay(Rules.summonsAltarGlowDuration)

            try {
                context.scoreboard.removeScoreHolderFromTeam(shulker.scoreboardName, context.getTeam(GameTeam.OPERATOR))
                shulker.remove(Entity.RemovalReason.DISCARDED)
            } catch (ignored: IllegalStateException) {}
        }
    }

    private fun summon(options: Options): FailureReason? {
        Game.CONSOLE_OPERATORS.sendInfo(
            "Player", options.player,
            "attempting summon", options.kind,
        )

        val failure = failCheck(options)
        if (failure != null) {
            consolePlayers.sendInfoRaw(text(options.team, "failed a summon"))
            consoleOperators.sendInfo("Summon failed:", failure, options.kind)

            textProvider = textProvidersFailure[failure]!!(options)
            failPerform(options, failure)
        } else {
            consolePlayers.sendInfoRaw(text(options.team, "summoned", options.kind))

            textProvider = textProvidersSuccess[options.kind]!!(options)
            summonPerform(options)
        }

        if (Rules.chaosEnabled) {
            timeoutReset()
        }

        textUpdateFull()
        summonEffects(options)

        return failure
    }

    private interface TextProvider {
        val title: Text
        val subtitle: Text
        val tooltip: Text
    }

    private abstract class DefaultTextProvider(val options: Options) : TextProvider
    private abstract class FailureTextProvider(val options: Options) : TextProvider {
        override val title: Text = text("Summon failed!")
        override val tooltip: Text = text(options.team, "failed to summon.")
    }

    private fun updateActive() {
        val active = summonListGame.map { it.kind }
        isScaldingEarth = active.contains(Prayer(FLAME, FLAME))
        isFlashFlood = active.contains(Prayer(DEEP, DEEP))
        isPollutedWater = active.contains(Prayer(DEEP, BARTER))
        isAcidRain = active.contains(Prayer(DEEP, COSMOS))
        isTracking = active.contains(Prayer(DEEP, OCCULT))
    }

    private fun updateBar() {
        val manager = server.bossBarManager

        val bar =
            manager.get(Identifier("msd_timeout")) ?:
            manager.add(Identifier("msd_timeout"), text("Summon Cooldown").red()).apply { color = BossBar.Color.RED }

        context.playerManager.playerList.forEach { bar.addPlayer(it) }

        val timeTotal = timeoutDuration
        val timeRemaining = timeoutRemaining()

        val isVisible = timeRemaining.seconds > 0
        if (isVisible != bar.isVisible) bar.isVisible = isVisible

        val value = timeRemaining.seconds.toInt()
        if (value != bar.value) bar.value = value

        val maxValue = timeTotal.seconds.toInt()
        if (maxValue != bar.maxValue) bar.maxValue = maxValue
    }

    override fun update() {
        summonListGame.toTypedArray().forEach { if (!it.tryUpdate()) it.destroy() }

        updateActive()
        updateBar()

        if (textTimeCheck()) {
            textUpdateTooltip()
        }
    }

    fun handleRoundEnd() {
        summonListRound.toTypedArray().forEach { if (it.isRoundOnly) it.destroy() }
        summonListRound.clear()
        timeoutReset()
        textClear()
    }

    private inner class AltarScreenHandlerFactory(private val altar: Altar)
        : DynamicScreenHandlerFactory<AltarScreenHandler>()
    {
        override val name get() = text("Altar of the ") + altar.theology.displayName
        override fun construct(sync: Int, playerInventory: PlayerInventory) = AltarScreenHandler(altar, sync, playerInventory)
    }

    private inner class AltarScreenHandler(private val altar: Altar, sync: Int, playerInventory: PlayerInventory)
        : DynamicScreenHandler(ScreenHandlerType.SMITHING, sync, playerInventory)
    {
        override val inventory = Inventories.create(4)

        init {
            addSlot(object : InputSlot(0, 8, 48) {
                override fun canInsert(stack: ItemStack) = theologyFor(stack) != null
                override fun getMaxItemCount() = 1
            })
            addSlot(object : InputSlot(1, 26, 48) {
                override fun canInsert(stack: ItemStack) = theologyFor(stack) != null
                override fun getMaxItemCount() = 1
            })
            addSlot(object : InputSlot(2, 44, 48) {
                override fun canInsert(stack: ItemStack) = hasCobbledBook(stack)
                override fun getMaxItemCount() = 1
            })
            addSlot(object : OutputSlot(3, 98, 48) {
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean {
                    val nbt = inventory.getStack(3).nbt
                    return nbt != null && nbt.contains("MsdSummonItem")
                }
            })
            addPlayerInventorySlots()
        }

        private fun theologyFor(stack: ItemStack): Theology? {
            val type = PagesService.pageTypeFor(stack)
            return if (type != null && type.action === PagesService.Action.SUMMON) type.theology else null
        }
        private fun theologies(): Prayer? {
            return Prayer(
                theologyFor(inventory.getStack(0)) ?: return null,
                theologyFor(inventory.getStack(1)) ?: return null,
            )
        }

        private fun hasCobbledBook(stack: ItemStack = inventory.getStack(2)): Boolean {
            return stack.hasNbt() && "MsdBookCobbled" in stack.nbt!!
        }

        override fun handleTakeResult(slot: OutputSlot, stack: ItemStack) {
            stack.count = 0

            val pair = theologies() ?: return
            if (pair.isDouble && !hasCobbledBook()) return

            val player = context.getPlayer(playerEntity)
            val failure = summon(Options(pair, altar, player))

            if (failure == null) {
                inventory.removeStack(0, 1)
                inventory.removeStack(1, 1)
                inventory.removeStack(2, 1)
            }

            inventory.setStack(3, stackOf())
        }

        override fun handleUpdateResult() {
            val pair = theologies()
            if (pair == null) {
                inventory.setStack(3, stackOf(
                    BARRIER, nbtCompoundOf(
                        "display" to nbtCompoundOf(
                            "Name" to text("summon pages required").red(),
                            "Lore" to nbtListOf(
                                text("put two summon pages into the first two slots"),
                                text("if the summon pages are the same, you need a cobbled book"),
                            ),
                        ),
                        "MsdIllegal" to true,
                    ),
                ))
                return
            }
            if (pair.isDouble && !hasCobbledBook()) {
                inventory.setStack(3, stackOf(
                    BARRIER, nbtCompoundOf(
                        "display" to nbtCompoundOf(
                            "Name" to text("cobbled book required").red(),
                            "Lore" to nbtListOf(
                                text("you are trying to perform a pure summon (same pages)"),
                                text("this requires a cobbled book in the third slot"),
                            ),
                        ),
                        "MsdIllegal" to true,
                    ),
                ))
                return
            }
            inventory.setStack(3, summonIcons[pair]!!.copy())
        }

        override fun handleClosed() {
            playerEntity.give(inventory.removeStack(0))
            playerEntity.give(inventory.removeStack(1))
            playerEntity.give(inventory.removeStack(2))
        }
    }

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        if (!player.isOperator) {
            if (state.isWaiting) return ActionResult.PASS
            if (!player.isPlaying || !player.isAlive) return ActionResult.PASS
            if (!Rules.summonsEnabled) return ActionResult.PASS
        }

        val altar: Altar? = altars[pos]
        if (altar != null) {
            player.entity?.openHandledScreen(AltarScreenHandlerFactory(altar))
            return ActionResult.SUCCESS
        } else {
            return ActionResult.PASS
        }
    }

    fun executeDebugPrint(console: Console) {
        console.sendInfo("--- SUMMON SYSTEM STATE")

        fun sendSummons(summons: List<Summon>) {
            for (summon in summons) {
                console.sendInfo(
                    "  -", summon.kind,
                    "team", summon.team,
                    "player", summon.player,
                )
            }
        }

        console.sendInfo("summonListGame:")
        sendSummons(summonListGame)
        console.sendInfo("summonListRound:")
        sendSummons(summonListRound)

        console.sendInfo("timeout:", timeoutRemaining().seconds, "seconds")
        console.sendInfo("timeoutDuration:", timeoutDuration.seconds, "seconds")

        console.sendInfo("textProvider:", textProvider)
        console.sendInfo("textLastUpdate:", Duration.between(textLastUpdate, Instant.now()).seconds, "seconds")

        console.sendInfo("isScaldingEarth:", isScaldingEarth)
        console.sendInfo("isPollutedWater:", isPollutedWater)
        console.sendInfo("isAcidRain:", isAcidRain)
        console.sendInfo("isTracking:", isTracking)

        console.sendInfo("---")
    }

    fun executeDebugReset(console: Console) {
        console.sendInfo("Resetting summon system state")

        summonListGame.forEach { it.tryStop() }
        summonListGame.clear()
        summonListRound.clear()

        timeoutReset()
        textClear()
    }

    fun executeDebugShowText(console: Console) {
        console.sendInfo("Displaying all summon TextProviders")

        val iterator = iterator {
            fun options() = Options(Prayer(DEEP, FLAME), altars.values.random(), players.random())

            for ((key, provider) in textProvidersSuccess) {
                yield(text(key) to provider(options()))
            }

            for ((key, provider) in textProvidersFailure) {
                yield(text(key) to provider(options()))
            }
        }

        Scheduler.interval(3.0) { schedule ->
            if (iterator.hasNext()) {
                val (name, provider) = iterator.next()

                console.sendInfo("Displaying TextProvider for", name)

                textProvider = provider
                textUpdateFull()
            } else {
                console.sendInfo("Finished displaying TextProviders")

                textClear()
                schedule.cancel()
            }
        }
    }

    fun executeClearTimeout(console: Console) {
        console.sendInfo("Clearing summon timeout")
        timeoutReset()
    }

    fun executeSummon(console: Console, kind: Prayer, player: GamePlayer) {
        console.sendInfo("Performing summon", kind, "manually")

        val altar: Altar = try {
            altars.values.random()
        } catch (cause : NoSuchElementException) {
            console.sendError("Failed, no altars available")
            return
        }

        val options = Options(kind, altar, player)
        summonPerform(options)
        summonEffects(options)
    }

    private companion object {

        private fun Inventory.removeAll(predicate: (ItemStack) -> Boolean) {
            for (i in 0 until size()) {
                val stack = getStack(i)
                if (predicate(stack)) setStack(i, stackOf())
            }
        }

        private fun ServerWorld.setWeatherRain() {
            setWeather(0, 24000 * 10, true, false)
        }
        private fun ServerWorld.setWeatherClear() {
            setWeather(24000 * 10, 0, false, false)
        }

        private val prayersOncePerGame = immutableListOf<Prayer>(
            Prayer(DEEP, DEEP),
            Prayer(DEEP, BARTER),
            Prayer(OCCULT, OCCULT),
            Prayer(OCCULT, BARTER),
            Prayer(COSMOS, FLAME),
            Prayer(FLAME,  FLAME),
        )
        private val prayersOncePerRound = immutableListOf<Prayer>(
            Prayer(DEEP, OCCULT),
            Prayer(DEEP, COSMOS),
            Prayer(OCCULT, COSMOS),
            Prayer(COSMOS, COSMOS),
            Prayer(BARTER, BARTER),
        )

        private fun summonIconFor(kind: Prayer, item: Item, vararg lore: Text): Pair<Prayer, ItemStack> =
            kind to stackOf(item, nbtCompoundOf(
                "display" to nbtCompoundOf(
                    "Name" to kind.displayName,
                    "Lore" to lore.asList(),
                ),
                "MsdIllegal" to true,
                "MsdSummonItem" to true,
            ))

        private var summonIconsReference: Map<Prayer, ItemStack>? = null

        private val summonIcons = immutableMapOf<Prayer, ItemStack>(
            summonIconFor(
                Prayer(DEEP, DEEP), WATER_BUCKET,
                text("Flood the entire map?"),
            ),
            summonIconFor(
                Prayer(BARTER, BARTER), STRUCTURE_VOID,
                text("Destroy all special items?"),
            ),
            summonIconFor(
                Prayer(FLAME, FLAME), FIRE_CHARGE,
                text("All blocks become flammable?"),
            ),
            summonIconFor(
                Prayer(COSMOS, COSMOS), RABBIT_FOOT,
                text("Gravity greatly reduced?"),
            ),
            summonIconFor(
                Prayer(OCCULT, OCCULT), WITHER_SKELETON_SKULL,
                text("Black team wins?"),
            ),
            summonIconFor(
                Prayer(DEEP, BARTER), POTION,
                text("Water blocks deal poison damage"),
            ),
            summonIconFor(
                Prayer(BARTER, FLAME), MAGMA_BLOCK,
                text("Receive some really hot blocks"),
            ),
            summonIconFor(
                Prayer(FLAME, COSMOS), SUNFLOWER,
                text("Call upon the unstoppable power of the sun"),
            ),
            summonIconFor(
                Prayer(COSMOS, OCCULT), END_STONE,
                text("Call upon the beautiful power of the moon"),
            ),
            summonIconFor(
                Prayer(DEEP, FLAME), ANVIL,
                text("Receive water buckets and anvils"),
            ),
            summonIconFor(
                Prayer(BARTER, COSMOS), COOKED_BEEF,
                text("Receive some steak"),
            ),
            summonIconFor(
                Prayer(FLAME, OCCULT), GHAST_SPAWN_EGG,
                text("Spawn ghasts in the arena"),
            ),
            summonIconFor(
                Prayer(DEEP, COSMOS), PRISMARINE_SHARD,
                text("Acid rain starts pouring down"),
            ),
            summonIconFor(
                Prayer(BARTER, OCCULT), GOLDEN_SWORD,
                text("Receive very powerful swords"),
            ),
            summonIconFor(
                Prayer(DEEP, OCCULT), COMPASS,
                text("Reveal the locations of your enemies"),
            )
        )

        init { summonIconsReference = summonIcons }

        private val textProvidersSuccess = immutableMapOf<Prayer, (Options) -> TextProvider>(
            Prayer(DEEP, OCCULT) to { object : DefaultTextProvider(it) {
                override val title =
                    text("Star & compass, map & sextant;")
                override val subtitle =
                    text(options.team, "received trackers to hunt their enemies.")
                override val tooltip =
                    text(options.team, "received trackers for enemy players.")
            } },
            Prayer(DEEP, COSMOS) to { object : DefaultTextProvider(it) {
                override val title =
                    text("Whirling clouds cast a stinging spittle!")
                override val subtitle =
                    text("Acid rain! Stay indoors.")
                override val tooltip =
                    text(options.team, "summoned stinging rain.")
            } },
            Prayer(DEEP, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    text("Flowing waters boil and bubble...")
                override val subtitle =
                    text("Bodies of water are now harmful!")
                override val tooltip =
                    text(options.team, "poisoned the water.")
            } },
            Prayer(DEEP, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    text("From the heart of the forge,")
                override val subtitle =
                    text(options.team, "has summoned water buckets and anvils.")
                override val tooltip =
                    text(options.team, "summoned water buckets and anvils.")
            } },
            Prayer(OCCULT, COSMOS) to { object : DefaultTextProvider(it) {
                override val title =
                    text("The full moon is upon us!")
                override val subtitle =
                    text(options.team, "has pulled a moon away the stars.")
                override val tooltip =
                    text(options.team, "summoned a moon to the arena's center.")
            } },
            Prayer(OCCULT, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    text("You only have one shot.")
                override val subtitle =
                    text(options.team, "has gained powerful single-use swords.")
                override val tooltip =
                    text(options.team, "was granted powerful swords.")
            } },
            Prayer(OCCULT, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    text("Horrifying screams come from below!")
                override val subtitle =
                    text(options.team, "has summoned some ghasts!")
                override val tooltip =
                    text(options.team, "summoned some ghasts!")
            } },
            Prayer(COSMOS, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    text("A feast fit for royals!")
                override val subtitle =
                    text(options.team, "has received a bounty of steaks.")
                override val tooltip =
                    text(options.team, "summoned a steak feast.")
            } },
            Prayer(COSMOS, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    text("The unmatched power of the sun!")
                override val subtitle =
                    text(options.team, "has pulled a star out of the sky.")
                override val tooltip =
                    text(options.team, "summoned a flaming star to the arena's center.")
            } },
            Prayer(BARTER, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    text("The weather outside is frightful...")
                override val subtitle =
                    text(options.team, "gained a full stack of ice blocks.")
                override val tooltip =
                    text(options.team, "gained blocks of ice.")
            } },
            Prayer(DEEP, DEEP) to { object : DefaultTextProvider(it) {
                override val title =
                    text("FLASH FLOOD") * DEEP.color
                override val subtitle =
                    text("The map is filling with water!")
                override val tooltip =
                    text("The map is flooding.")
            } },
            Prayer(OCCULT, OCCULT) to { object : DefaultTextProvider(it) {
                override val title =
                    text("DESPERATION") * OCCULT.color
                override val subtitle =
                    text(GameTeam.BLACK, "ascends, almost killing everyone else.")
                override val tooltip =
                    text(GameTeam.BLACK, "players have ascended.")
            } },
            Prayer(COSMOS, COSMOS) to { object : DefaultTextProvider(it) {
                override val title =
                    text("FINAL FRONTIER") * COSMOS.color
                override val subtitle =
                    text("Gravity has been significantly reduced!")
                override val tooltip =
                    text("Gravity is significantly reduced.")
            } },
            Prayer(BARTER, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    text("THE I.R.S.") * BARTER.color
                override val subtitle =
                    text("All special items have been lost!")
                override val tooltip =
                    text("All special items have been lost.")
            } },
            Prayer(FLAME, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    text("INFERNO") * FLAME.color
                override val subtitle =
                    text("Most blocks are now flammable.")
                override val tooltip =
                    text("Most blocks are now flammable.")
            } },
        )

        private val textProvidersFailure = immutableMapOf<FailureReason, (Options) -> TextProvider>(
            FailureReason.TIMEOUT to { object : FailureTextProvider(it) {
                override val subtitle =
                    text("Let the cooldown complete first!")
            } },
            FailureReason.MISMATCH to { object : FailureTextProvider(it) {
                override val subtitle: Text =
                    if (options.kind.isDouble) {
                        text(options.kind, "can only be performed at a", options.kind.theology1, "altar!")
                    } else {
                        text(
                            options.kind.displayName,
                            "can only be performed at a",
                            options.kind.theology1,
                            "or",
                            options.kind.theology2,
                            "altar!",
                        )
                    }
            } },
            FailureReason.REPEATED_DOUBLE to { object : FailureTextProvider(it) {
                override val subtitle =
                    text(options.kind, "can only be performed once per game!")
            } },
            FailureReason.REPEATED_ONCE to { object : FailureTextProvider(it) {
                override val subtitle =
                    text(options.kind, "can only be performed once per round!")
            } },
            FailureReason.REPEATED_SEQUENCE to { object : FailureTextProvider(it) {
                override val subtitle =
                    text(options.kind, "can't be performed twice in a row!")
            } },
            FailureReason.SPRINGTRAP to { object : FailureTextProvider(it) {
                override val subtitle =
                    text("He always comes back...")
            } },
        )

    }

}
