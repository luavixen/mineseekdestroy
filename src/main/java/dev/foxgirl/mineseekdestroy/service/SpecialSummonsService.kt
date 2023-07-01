package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.immutableMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import dev.foxgirl.mineseekdestroy.util.collect.toImmutableSet
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtOps
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.potion.PotionUtil
import net.minecraft.screen.AnvilScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameRules
import net.minecraft.world.World
import java.time.Duration
import java.time.Instant

class SpecialSummonsService : Service() {

    enum class Theology {
        DEEP { override val color = Formatting.DARK_AQUA },
        OCCULT { override val color = Formatting.LIGHT_PURPLE },
        COSMOS { override val color = Formatting.BLUE },
        BARTER { override val color = Formatting.GOLD },
        FLAME { override val color = Formatting.RED },
        OPERATOR { override val color = Formatting.GREEN };

        abstract val color: Formatting

        val displayName: Text get() = Text.literal(name).formatted(color)
    }

    class TheologyPair(theology1: Theology, theology2: Theology) {
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
        val isOnce get() = theologyPairsOnce.contains(this)

        val displayName: Text get() =
            Text.empty()
                .append(theology1.displayName)
                .append(Text.literal(" X ").formatted(Formatting.GREEN))
                .append(theology2.displayName)
                .styled { it.withItalic(false) }

        override fun toString() = "TheologyPair(theology1=${theology1}, theology2=${theology2})"

        override fun hashCode() = 31 * theology1.hashCode() + theology2.hashCode()
        override fun equals(other: Any?) =
            other === this || (other is TheologyPair && other.theology1 === theology1 && other.theology2 === theology2)
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

    private inner class DeepDeepSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(30)
        override fun perform() {
            val blocks = immutableSetOf<Block>(
                Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
                Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING,
                Blocks.MANGROVE_PROPAGULE, Blocks.GRASS, Blocks.TALL_GRASS,
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

            Async.run {
                val (start, end) = properties.regionFlood
                for (y in start.y..end.y) {
                    delay(1.0)

                    val region = Region(
                        BlockPos(start.x, y, start.z),
                        BlockPos(end.x, y, end.z),
                    )
                    val promise = Editor.edit(world, region) { state, _, _, _ ->
                        if (state.isAir || blocks.contains(state.block)) {
                            Blocks.WATER.defaultState
                        } else if (state.contains(Properties.WATERLOGGED)) {
                            state.with(Properties.WATERLOGGED, true)
                        } else {
                            null
                        }
                    }

                    await(promise)
                }
            }

            world.setWeatherRain()
        }
    }

    private inner class DeepOccultSummon(options: Options) : Summon(options), Stoppable {
        override fun timeout(): Duration = Duration.ofSeconds(60)
        override fun perform() {
            val targets = playersIn.filter { it.team !== team }
            val targetsPool = targets.toMutableList().apply { shuffle() }

            if (targets.isEmpty()) return

            fun target() = targetsPool.removeLastOrNull() ?: targets.random()

            val nbtSpawn = NbtHelper.fromBlockPos(properties.positionSpawn)
            val nbtDimension = World.CODEC.encodeStart(NbtOps.INSTANCE, world.registryKey).result().get()

            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== team) continue

                val target = target()

                entity.give(GameItems.summonCompass.copy().apply {
                    dataDisplay()["Name"] = toNbtElement((text("Sound of ").green() + target.displayName).styled { it.withItalic(false) })
                    data()["LodestonePos"] = nbtSpawn
                    data()["LodestoneDimension"] = nbtDimension
                    data()["LodestoneTracked"] = true
                    data()["MsdTargetPlayer"] = target.uuid
                })
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

                    nbt["LodestonePos"] = NbtHelper.fromBlockPos(targetEntity.blockPos)
                }
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeItem { it.item === COMPASS }
            }
        }
    }

    private inner class DeepCosmosSummon(options: Options) : Summon(options), Stoppable {
        override fun perform() {
            world.setWeatherRain()
        }
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isBeingRainedOn && !entity.hasStatusEffect(StatusEffects.POISON)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.POISON, 40))
                }
            }
        }
        override fun stop() {
            world.setWeatherClear()
        }
    }

    private inner class DeepBarterSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(30)
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isTouchingWater && !entity.hasStatusEffect(StatusEffects.POISON)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.POISON, 60))
                }
            }
        }
    }

    private inner class DeepFlameSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(90)
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
            for (player in players) {
                if (player.team === GameTeam.PLAYER_BLACK) player.kills += 2
            }
            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== GameTeam.PLAYER_BLACK) entity.health /= 2
            }
        }
    }

    private inner class OccultCosmosSummon(options: Options) : Summon(options), Stoppable {
        override fun timeout(): Duration = Duration.ofSeconds(90)
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
        override fun timeout(): Duration = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesIn) {
                if (player.team === team) entity.give(GameItems.summonGoldenSword.copy())
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                entity.removeItem { it.item === GOLDEN_SWORD }
            }
        }
    }

    private inner class OccultFlameSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(30)
        override fun perform() {
            val region = properties.regionBlimp
            val position = BlockPos(region.center.x.toInt(), region.start.y - 7, region.center.z.toInt())
            for (i in 1..3) EntityType.GHAST.spawn(world, position, SpawnReason.COMMAND)
        }
    }

    private inner class CosmosCosmosSummon(options: Options) : Summon(options) {
        override fun update() {
            for ((_, entity) in playerEntitiesNormal) {
                if (!entity.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOW_FALLING, 80))
                }
                if (!entity.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.JUMP_BOOST, 80, 5))
                }
            }
        }
    }

    private inner class CosmosBarterSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.give(GameItems.summonSteak.copyWithCount(8))
            }
        }
    }

    private inner class CosmosFlameSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(60)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.absorptionAmount += 2.0F
            }
        }
    }

    private inner class BarterBarterSummon(options: Options) : Summon(options) {
        override fun perform() {
            val items = immutableSetOf<Item>(
                SHIELD, CARROT_ON_A_STICK, FISHING_ROD, TIPPED_ARROW,
                COOKED_BEEF, GOLDEN_SWORD, FLINT_AND_STEEL, WATER_BUCKET,
                COMPASS, TARGET, FIREWORK_ROCKET, ENDER_PEARL, BLUE_ICE,
                ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL,
            )

            for ((_, entity) in playerEntitiesNormal) {
                entity.removeItem { items.contains(it.item) }
            }
        }
    }

    private inner class BarterFlameSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(90)
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
                .edit(world, properties.regionPlayable) { state, _, _, _ ->
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
    }

    private val summons = immutableMapOf<TheologyPair, (Options) -> Summon>(
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
    private var timeoutDuration = Duration.ZERO

    private var textProvider: TextProvider? = null
    private var textLastUpdate = Instant.now()

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
                Blocks.COMMAND_BLOCK -> OPERATOR
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
            .terminate()
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
            Broadcast.send(OverlayMessageS2CPacket(textProvider.tooltip))
        }
        textTimeUpdate()
    }

    private fun textUpdateTooltip() {
        val textProvider = textProvider
        if (textProvider != null) {
            Broadcast.send(OverlayMessageS2CPacket(textProvider.tooltip))
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

    private enum class Failure {
        TIMEOUT, MISMATCH, REPEATED_DOUBLE, REPEATED_ONCE, REPEATED_SEQUENCE
    }

    private fun failCheck(options: Options): Failure? {
        if (options.altar.theology == OPERATOR) {
            return null
        }

        if (timeoutCheck()) {
            return Failure.TIMEOUT
        }

        val kind = options.kind

        if (kind.theology1 !== options.altar.theology && kind.theology2 !== options.altar.theology) {
            return Failure.MISMATCH
        }
        if (kind.isDouble && summonListGame.any { it.kind == kind }) {
            return Failure.REPEATED_DOUBLE
        }
        if (kind.isOnce && summonListRound.any { it.kind == kind }) {
            return Failure.REPEATED_ONCE
        }
        if (kind == summonListRound.lastOrNull()?.kind) {
            return Failure.REPEATED_SEQUENCE
        }

        return null
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
        summonListGame.add(summon)
        summonListRound.add(summon)
        timeoutSet(summon.timeout())
        try {
            summon.perform()
        } catch (cause : Exception) {
            Game.CONSOLE_OPERATORS.sendError("Summon encountered exception while performing:", summon.javaClass.simpleName)
            Game.LOGGER.error("SpecialSummonsService exception while performing ${summon.javaClass.simpleName}", cause)
        }
    }

    private fun summonEffects(options: Options) {
        val lightning = EntityType.LIGHTNING_BOLT.create(world)!!
        lightning.setPosition(options.altar.pos.toCenterPos())
        lightning.setCosmetic(true)
        world.spawnEntity(lightning)
    }

    private fun summon(options: Options) {
        Game.CONSOLE_OPERATORS.sendInfo(
            "Player", options.player.displayName,
            "attempting summon", options.kind.displayName,
        )

        val failure = failCheck(options)
        if (failure != null) {
            Game.CONSOLE_PLAYERS.sendInfo(options.team.displayName, "failed a summon")
            Game.CONSOLE_OPERATORS.sendInfo("Summon", options.kind.displayName, "failed:", failure)

            textProvider = textProvidersFailure[failure]!!(options)
            failPerform(options)
        } else {
            Game.CONSOLE_PLAYERS.sendInfo(options.team.displayName, "summoned", options.kind.displayName)
            Game.CONSOLE_OPERATORS.sendInfo("Summon", options.kind.displayName, "success!")

            textProvider = textProvidersSuccess[options.kind]!!(options)
            summonPerform(options)
        }

        textUpdateFull()
        summonEffects(options)
    }

    private interface TextProvider {
        val title: Text
        val subtitle: Text
        val tooltip: Text
    }

    private abstract class DefaultTextProvider(val options: Options) : TextProvider
    private abstract class FailureTextProvider(val options: Options) : TextProvider {
        override val title: Text =
            Text.of("Summon failed!")
        override val tooltip: Text =
            Text.empty().append(options.team.displayName).append(" failed to summon.")
    }

    private fun updateActive() {
        val active = summonListGame.map { it.kind }.toImmutableSet()
        isScaldingEarth = active.contains(TheologyPair(FLAME, FLAME))
        isPollutedWater = active.contains(TheologyPair(DEEP, BARTER))
        isAcidRain = active.contains(TheologyPair(DEEP, COSMOS))
        isTracking = active.contains(TheologyPair(DEEP, OCCULT))
    }

    private fun updateBar() {
        val manager = server.bossBarManager

        val bar =
            manager.get(Identifier("msd_timeout")) ?:
            manager.add(Identifier("msd_timeout"), Text.literal("Summon Cooldown").formatted(Formatting.RED)).apply { color = BossBar.Color.RED }

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
        val iterator = summonListGame.iterator()
        while (iterator.hasNext()) {
            val summon = iterator.next()
            try {
                summon.update()
            } catch (cause : Exception) {
                iterator.remove()
                Game.CONSOLE_OPERATORS.sendError("Summon encountered exception while updating:", summon.javaClass.simpleName)
                Game.LOGGER.error("SpecialSummonsService exception while updating ${summon.javaClass.simpleName}", cause)
            }
        }

        updateActive()
        updateBar()

        if (textTimeCheck()) {
            textUpdateTooltip()
        }
    }

    fun handleRoundEnd() {
        for (summon in summonListRound) {
            if (summon is Stoppable) {
                summonListGame.remove(summon)
                try {
                    summon.stop()
                    Game.CONSOLE_OPERATORS.sendInfo("Summon stopped:", summon.javaClass.simpleName)
                } catch (cause : Exception) {
                    Game.CONSOLE_OPERATORS.sendError("Summon encountered exception while stopping:", summon.javaClass.simpleName)
                    Game.LOGGER.error("SpecialSummonsService exception while stopping ${summon.javaClass.simpleName}", cause)
                }
            }
        }

        summonListRound.clear()
        timeoutReset()
        textClear()
    }

    private inner class AltarScreenHandler(val altar: Altar, syncId: Int, playerInventory: PlayerInventory) : AnvilScreenHandler(syncId, playerInventory) {
        private fun theologies(): TheologyPair? {
            fun theologyFor(stack: ItemStack): Theology? {
                if (stack.item !== TIPPED_ARROW) return null

                for (effect in PotionUtil.getPotion(stack).effects) {
                    val theology: Theology? = when (effect.effectType) {
                        StatusEffects.WATER_BREATHING -> DEEP
                        StatusEffects.INSTANT_HEALTH -> OCCULT
                        StatusEffects.INVISIBILITY -> COSMOS
                        StatusEffects.STRENGTH -> BARTER
                        StatusEffects.FIRE_RESISTANCE -> FLAME
                        else -> null
                    }
                    if (theology != null) return theology
                }

                return null
            }

            return TheologyPair(
                theologyFor(input.getStack(0)) ?: return null,
                theologyFor(input.getStack(1)) ?: return null,
            )
        }

        override fun updateResult() {
            val pair = theologies()
            if (pair != null) {
                output.setStack(0, summonItems[pair]!!.copy())
            } else {
                output.setStack(0, ItemStack.EMPTY)
            }
        }

        override fun onTakeOutput(playerEntity: PlayerEntity, stack: ItemStack) {
            val pair = theologies()
            if (pair != null) {
                val player = this@SpecialSummonsService.context.getPlayer(playerEntity as ServerPlayerEntity)
                summon(Options(pair, altar, player))
            }

            stack.count = 0
            input.removeStack(0, 1)
            input.removeStack(1, 1)
        }

        override fun onClosed(player: PlayerEntity) {
            player.giveItem(cursorStack)
            cursorStack = ItemStack.EMPTY

            player.giveItem(input.removeStack(0))
            player.giveItem(input.removeStack(1))
        }

        override fun canTakeOutput(playerEntity: PlayerEntity, present: Boolean): Boolean {
            return theologies() != null
        }

        override fun canUse(player: PlayerEntity?) = true
        override fun canUse(state: BlockState?) = true

        override fun canInsertIntoSlot(slot: Slot?) = true
        override fun canInsertIntoSlot(stack: ItemStack?, slot: Slot?) = true

        override fun setNewItemName(newItemName: String?): Boolean {
            updateResult()
            return true
        }
    }

    private inner class AltarNamedScreenHandlerFactory(val altar: Altar) : NamedScreenHandlerFactory {
        override fun getDisplayName(): Text =
            Text.literal("Altar of the ").append(altar.theology.displayName)
        override fun createMenu(syncId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): ScreenHandler =
            AltarScreenHandler(altar, syncId, playerInventory)
    }

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        if (!player.isOperator) {
            if (state !is RunningGameState) return ActionResult.PASS
            if (!player.isPlaying || !player.isAlive) return ActionResult.PASS
            if (!game.getRuleBoolean(Game.RULE_SUMMONS_ENABLED)) return ActionResult.PASS
        }

        val altar: Altar? = altars[pos]
        if (altar != null) {
            player.entity?.openHandledScreen(AltarNamedScreenHandlerFactory(altar))
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
                    "  -", summon.kind.displayName,
                    "team", summon.team.displayName,
                    "player", summon.player.displayName,
                )
            }
        }

        console.sendInfo("summonListGame:")
        sendSummons(summonListGame)
        console.sendInfo("summonListRound:")
        sendSummons(summonListGame)

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

        summonListGame.forEach {
            if (it is Stoppable) {
                it.stop()
                Game.CONSOLE_OPERATORS.sendInfo("Summon stopped due to reset:", it.javaClass.simpleName)
            }
        }
        summonListGame.clear()
        summonListRound.clear()

        timeoutReset()
        textClear()
    }

    fun executeDebugShowText(console: Console) {
        console.sendInfo("Displaying all summon TextProviders")

        val iterator = iterator {
            fun options() = Options(TheologyPair(DEEP, FLAME), altars.values.random(), players.random())

            for ((key, provider) in textProvidersSuccess) {
                yield(key.displayName to provider(options()))
            }

            for ((key, provider) in textProvidersFailure) {
                yield(Text.of(key.toString()) to provider(options()))
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

    fun executeSummon(console: Console, kind: TheologyPair, player: GamePlayer) {
        console.sendInfo("Performing summon", kind.displayName, "manually")

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

        private fun PlayerEntity.giveItem(stack: ItemStack) {
            this.give(stack)
        }
        private fun PlayerEntity.removeItem(predicate: (ItemStack) -> Boolean) {
            val inventory = inventory
            for (i in 0 until inventory.size()) {
                val stack = inventory.getStack(i)
                if (stack.isEmpty) continue
                if (predicate(stack)) inventory.setStack(i, ItemStack.EMPTY)
            }
        }

        private fun ServerWorld.setWeatherRain() {
            setWeather(0, 24000 * 10, true, false)
        }
        private fun ServerWorld.setWeatherClear() {
            setWeather(24000 * 10, 0, false, false)
        }

        private val theologyPairsDouble = immutableSetOf<TheologyPair>(
            TheologyPair(DEEP, DEEP),
            TheologyPair(OCCULT, OCCULT),
            TheologyPair(COSMOS, COSMOS),
            TheologyPair(BARTER, BARTER),
            TheologyPair(FLAME, FLAME),
        )
        private val theologyPairsOnce = immutableSetOf<TheologyPair>(
            TheologyPair(DEEP, OCCULT),
            TheologyPair(DEEP, COSMOS),
            TheologyPair(DEEP, BARTER),
            TheologyPair(OCCULT, BARTER),
        )

        private fun summonItem(kind: TheologyPair, item: Item, vararg lore: Text): Pair<TheologyPair, ItemStack> =
            kind to stackOf(item, nbtCompoundOf(
                "display" to nbtCompoundOf(
                    "Name" to toNbtElement(kind.displayName),
                    "Lore" to toNbtList(lore.asList()),
                ),
                "MsdIllegal" to true,
                "MsdSummonItem" to true,
            ))

        private val summonItems = immutableMapOf<TheologyPair, ItemStack>(
            summonItem(
                TheologyPair(DEEP, DEEP), WATER_BUCKET,
                Text.of("Flood the entire map?"),
            ),
            summonItem(
                TheologyPair(BARTER, BARTER), BARRIER,
                Text.of("Destroy all special items?"),
            ),
            summonItem(
                TheologyPair(FLAME, FLAME), FIRE_CHARGE,
                Text.of("All blocks become flammable?"),
            ),
            summonItem(
                TheologyPair(COSMOS, COSMOS), RABBIT_FOOT,
                Text.of("Gravity greatly reduced?"),
            ),
            summonItem(
                TheologyPair(OCCULT, OCCULT), WITHER_SKELETON_SKULL,
                Text.of("Black team wins?"),
            ),
            summonItem(
                TheologyPair(DEEP, BARTER), POTION,
                Text.of("Water blocks deal poison damage"),
            ),
            summonItem(
                TheologyPair(BARTER, FLAME), MAGMA_BLOCK,
                Text.of("Receive some really hot blocks"),
            ),
            summonItem(
                TheologyPair(FLAME, COSMOS), GOLDEN_APPLE,
                Text.of("Bonus absorption heart"),
            ),
            summonItem(
                TheologyPair(COSMOS, OCCULT), SCULK,
                Text.of("Night vision for your team"),
                Text.of("(Blindness for others)"),
            ),
            summonItem(
                TheologyPair(DEEP, FLAME), ANVIL,
                Text.of("Receive water buckets and anvils"),
            ),
            summonItem(
                TheologyPair(BARTER, COSMOS), COOKED_BEEF,
                Text.of("Receive some steak"),
            ),
            summonItem(
                TheologyPair(FLAME, OCCULT), GHAST_SPAWN_EGG,
                Text.of("Spawn ghasts in the arena"),
            ),
            summonItem(
                TheologyPair(DEEP, COSMOS), PRISMARINE_SHARD,
                Text.of("Acid rain starts pouring down"),
            ),
            summonItem(
                TheologyPair(BARTER, OCCULT), GOLDEN_SWORD,
                Text.of("Receive very powerful swords"),
            ),
            summonItem(
                TheologyPair(DEEP, OCCULT), COMPASS,
                Text.of("Reveal the locations of your enemies"),
            )
        )

        private val textProvidersSuccess = immutableMapOf<TheologyPair, (Options) -> TextProvider>(
            TheologyPair(DEEP, OCCULT) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("Star and compass, map and sextant;")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" received trackers to hunt their enemies.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" received trackers for enemy players.")
            } },
            TheologyPair(DEEP, COSMOS) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("Whirling clouds cast a stinging spittle!")
                override val subtitle =
                    Text.of("Acid rain! Stay indoors.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned stinging rain.")
            } },
            TheologyPair(DEEP, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("Flowing waters boil and bubble...")
                override val subtitle =
                    Text.of("Bodies of water are now harmful!")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" poisoned the water.")
            } },
            TheologyPair(DEEP, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("From the heart of the forge,")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has summoned water buckets and anvils.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned water buckets and anvils.")
            } },
            TheologyPair(OCCULT, COSMOS) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("A creeping void floods the maze...")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has gained fullbright and blinded everyone else.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has fullbright visibility.")
            } },
            TheologyPair(OCCULT, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("You only have one shot.")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has gained powerful single-use swords.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" was granted powerful swords.")
            } },
            TheologyPair(OCCULT, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("Horrifying screams come from below!")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has summoned some ghasts!")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned some ghasts!")
            } },
            TheologyPair(COSMOS, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("A feast fit for royals!")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has received a bounty of steaks.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned a steak feast.")
            } },
            TheologyPair(COSMOS, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("A lingering flame burns inside!")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has gained additional health.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" gained a health bonus.")
            } },
            TheologyPair(BARTER, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.of("Oh, the weather outside is frightful...")
                override val subtitle =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" gained a full stack of ice blocks.")
                override val tooltip =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" gained blocks of ice.")
            } },
            TheologyPair(DEEP, DEEP) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.literal("FLASH FLOOD").formatted(DEEP.color)
                override val subtitle =
                    Text.of("The map is filling with water!")
                override val tooltip =
                    Text.of("The map is flooding.")
            } },
            TheologyPair(OCCULT, OCCULT) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.literal("DESPERATION").formatted(OCCULT.color)
                override val subtitle =
                    Text.empty()
                        .append(GameTeam.PLAYER_BLACK.displayName)
                        .append(" gains many kills, halving everyone's health.")
                override val tooltip =
                    Text.empty()
                        .append(GameTeam.PLAYER_BLACK.displayName)
                        .append(" players gained two kills.")
            } },
            TheologyPair(COSMOS, COSMOS) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.literal("FINAL FRONTIER").formatted(COSMOS.color)
                override val subtitle =
                    Text.of("Gravity has been significantly reduced!")
                override val tooltip =
                    Text.of("Gravity is significantly reduced.")
            } },
            TheologyPair(BARTER, BARTER) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.literal("MARKET CRASH").formatted(BARTER.color)
                override val subtitle =
                    Text.of("All special items have been lost!")
                override val tooltip =
                    Text.of("All special items have been lost.")
            } },
            TheologyPair(FLAME, FLAME) to { object : DefaultTextProvider(it) {
                override val title =
                    Text.literal("INFERNO").formatted(FLAME.color)
                override val subtitle =
                    Text.of("Most blocks are now flammable.")
                override val tooltip =
                    Text.of("Most blocks are now flammable.")
            } },
        )

        private val textProvidersFailure = immutableMapOf<Failure, (Options) -> TextProvider>(
            Failure.TIMEOUT to { object : FailureTextProvider(it) {
                override val subtitle =
                    Text.of("Let the cooldown complete first!")
            } },
            Failure.MISMATCH to { object : FailureTextProvider(it) {
                override val subtitle: Text =
                    if (options.kind.isDouble) {
                        Text.empty()
                            .append(options.kind.displayName)
                            .append(" can only be performed at a ")
                            .append(options.kind.theology1.displayName)
                            .append(" altar!")
                    } else {
                        Text.empty()
                            .append(options.kind.displayName)
                            .append(" can only be performed at a ")
                            .append(options.kind.theology1.displayName)
                            .append(" or ")
                            .append(options.kind.theology2.displayName)
                            .append(" altar!")
                    }
            } },
            Failure.REPEATED_DOUBLE to { object : FailureTextProvider(it) {
                override val subtitle =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" can only be performed once per game!")
            } },
            Failure.REPEATED_ONCE to { object : FailureTextProvider(it) {
                override val subtitle =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" can only be performed once per round!")
            } },
            Failure.REPEATED_SEQUENCE to { object : FailureTextProvider(it) {
                override val subtitle =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" can't be performed twice in a row!")
            } },
        )

    }

}
