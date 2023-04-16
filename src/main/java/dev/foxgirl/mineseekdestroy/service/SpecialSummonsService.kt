package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import dev.foxgirl.mineseekdestroy.util.Broadcast
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Inventories
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.Scheduler
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
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
import net.minecraft.nbt.*
import net.minecraft.potion.PotionUtil
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
import net.minecraft.world.GameRules
import net.minecraft.world.World
import java.lang.NullPointerException
import java.time.Duration
import java.time.Instant
import java.util.NoSuchElementException

class SpecialSummonsService : Service() {

    enum class Theology {
        DEEP { override val color = Formatting.DARK_AQUA },
        OCCULT { override val color = Formatting.LIGHT_PURPLE },
        COSMOS { override val color = Formatting.BLUE },
        BARTER { override val color = Formatting.GOLD },
        FLAME { override val color = Formatting.RED };

        abstract val color: Formatting

        val displayName: Text get() = Text.literal(name).formatted(color)
    }

    class TheologyPair(theology1: Theology, theology2: Theology) {
        val theology1 = minOf(theology1, theology2)
        val theology2 = maxOf(theology1, theology2)

        val isDouble get() = theology1 === theology2
        val isOnce get() = theologyPairsOnce.contains(this)

        val displayName: Text get() =
            Text.empty()
                .append(theology1.displayName)
                .append(Text.literal(" X ").formatted(Formatting.GREEN))
                .append(theology2.displayName)

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
            val slices = iterator {
                val (start, end) = properties.regionFlood
                for (y in start.y..end.y) {
                    yield(Region(
                        BlockPos(start.x, y, start.z),
                        BlockPos(end.x, y, end.z),
                    ))
                }
            }
            Scheduler.interval(1.0) { schedule ->
                if (slices.hasNext()) {
                    Editor.edit(world, slices.next()) { state, _, _, _ ->
                        if (state.isAir) {
                            Blocks.WATER.defaultState
                        } else if (state.contains(Properties.WATERLOGGED)) {
                            state.with(Properties.WATERLOGGED, true)
                        } else {
                            null
                        }
                    }
                } else {
                    schedule.cancel()
                }
            }

            world.setWeather(0, 24000 * 10, true, false)
        }
    }

    private inner class DeepOccultSummon(options: Options) : Summon(options), Stoppable {
        override fun timeout(): Duration = Duration.ofSeconds(60)
        override fun perform() {
            val targets = playersIn.filter { it.team !== team }
            val targetsPool = targets.toMutableList().apply { shuffle() }

            fun target() = targetsPool.removeLastOrNull() ?: targets.random()

            val nbtSpawn = NbtHelper.fromBlockPos(properties.positionSpawn)
            val nbtDimension = World.CODEC.encodeStart(NbtOps.INSTANCE, world.registryKey).result().get()

            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== team) continue

                val target = target()

                val name = Text.literal("Sound of ").formatted(Formatting.GREEN).append(target.displayName)
                val nameJSON = Text.Serializer.toJson(name)

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
        override fun perform() {
            world.setWeather(0, 24000 * 10, true, false)
        }
        override fun update() {
            for ((_, entity) in playerEntitiesIn) {
                if (entity.isBeingRainedOn && !entity.hasStatusEffect(StatusEffects.POISON)) {
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.POISON, 40))
                }
            }
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
                    entity.giveItem(ItemStack(FLINT_AND_STEEL).apply { addEnchantment(Enchantments.UNBREAKING, 3) })
                    entity.giveItem(ItemStack(CHIPPED_ANVIL))
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
        override fun timeout(): Duration = Duration.ofSeconds(30)
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
                    entity.addStatusEffect(StatusEffectInstance(StatusEffects.JUMP_BOOST, 20000000, 5))
                }
            }
        }
    }

    private inner class CosmosBarterSummon(options: Options) : Summon(options) {
        override fun timeout(): Duration = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.giveItem(ItemStack(COOKED_BEEF, 8))
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
        override fun timeout(): Duration = Duration.ofSeconds(90)
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) entity.giveItem(ItemStack(SNOW_BLOCK, 64))
            }
        }
    }

    private inner class FlameFlameSummon(options: Options) : Summon(options) {
        override fun perform() {
            val blocks = setOf(
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

            game.setRuleBoolean(GameRules.DO_FIRE_TICK, true)
            world.setWeather(24000 * 10, 0, false, false)
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
    private var timeoutDuration = Duration.ZERO

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

    private enum class Failure {
        TIMEOUT, MISMATCH, REPEATED_DOUBLE, REPEATED_ONCE, REPEATED_SEQUENCE
    }

    private fun failCheck(options: Options): Failure? {
        val kind = options.kind

        if (timeoutCheck()) {
            return Failure.TIMEOUT
        }

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
        summon.perform()
        timeoutSet(summon.timeout())
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
            Game.CONSOLE_OPERATORS.sendInfo("Summon", options.kind.displayName, "failed:", failure)
            failPerform(options)
        } else {
            Game.CONSOLE_OPERATORS.sendInfo("Summon", options.kind.displayName, "success!")
            summonPerform(options)
        }

        summonEffects(options)
    }

    private interface TextProvider {
        val title: Text
        val subtitle: Text

        val tooltip: Text
    }

    private abstract class FailureTextProvider(val options: Options) : TextProvider {
        override val title: Text get() =
            Text.of("A summon has failed!")
        override val tooltip: Text get() =
            Text.empty().append(options.team.displayName).append(" failed to summon.")
    }

    override fun update() {
        summonListGame.forEach { it.update() }

        val active = summonListGame.map { it.kind }.toSet()
        isScaldingEarth = active.contains(TheologyPair(FLAME, FLAME))
        isPollutedWater = active.contains(TheologyPair(DEEP, BARTER))
        isAcidRain = active.contains(TheologyPair(DEEP, COSMOS))
        isTracking = active.contains(TheologyPair(DEEP, OCCULT))

        val barManager = server.bossBarManager
        val bar =
            barManager.get(Identifier("msd_timeout")) ?:
            barManager.add(Identifier("msd_timeout"), Text.literal("Summon Cooldown").formatted(Formatting.RED)).apply { color = BossBar.Color.RED }

        context.playerManager.playerList.forEach { bar.addPlayer(it) }

        val timeTotal = timeoutDuration
        val timeRemaining = timeoutRemaining()

        bar.isVisible = timeRemaining.seconds > 0

        bar.value = timeRemaining.seconds.toInt()
        bar.maxValue = timeTotal.seconds.toInt()
    }

    fun handleRoundEnd() {
        summonListRound.forEach {
            if (it is Stoppable) {
                it.stop()
                summonListGame.remove(it)
                Game.CONSOLE_OPERATORS.sendInfo("Summon stopped:", it.javaClass.simpleName)
            }
        }

        summonListRound.clear()
        timeoutReset()
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

            playerEntity.removeItem { ItemStack.areEqual(it, stack) }

            stack.count = 0
            input.removeStack(0, 1)
            input.removeStack(1, 1)
        }

        override fun canTakeOutput(playerEntity: PlayerEntity, present: Boolean): Boolean {
            return theologies() != null
        }

        override fun canUse(player: PlayerEntity?) = true
        override fun canUse(state: BlockState?) = true

        override fun canInsertIntoSlot(slot: Slot?) = true
        override fun canInsertIntoSlot(stack: ItemStack?, slot: Slot?) = true

        override fun setNewItemName(newItemName: String?) {
            updateResult()
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
        if (altar == null) {
            Game.LOGGER.info("Player ${player.name} failed to open invalid altar at ${pos}")
            return ActionResult.PASS
        } else {
            Game.LOGGER.info("Player ${player.name} opened altar at ${pos} with theology ${altar.theology}")
            player.entity?.openHandledScreen(AltarNamedScreenHandlerFactory(altar))
            return ActionResult.SUCCESS
        }
    }

    fun executeStateDebug(console: Console) {
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

        console.sendInfo("isScaldingEarth:", isScaldingEarth)
        console.sendInfo("isPollutedWater:", isPollutedWater)
        console.sendInfo("isAcidRain:", isAcidRain)
        console.sendInfo("isTracking:", isTracking)

        console.sendInfo("---")
    }

    fun executeStateReset(console: Console) {
        console.sendInfo("Resetting summon system state")

        summonListGame.forEach {
            if (it is Stoppable) {
                it.stop()
                Game.CONSOLE_OPERATORS.sendInfo("Summon stopped:", it.javaClass.simpleName)
            }
        }
        summonListGame.clear()
        summonListRound.clear()

        timeoutReset()
    }

    fun executeClearTimeout(console: Console) {
        console.sendInfo("Clearing summon timeout")
        timeoutReset()
    }

    fun executeSummon(console: Console, kind: TheologyPair) {
        console.sendInfo("Performing summon", kind.displayName, "manually")

        val altar: Altar = try {
            altars.values.random()
        } catch (cause : NoSuchElementException) {
            console.sendError("Failed, no altars available")
            return
        }

        val player: GamePlayer = try {
            players.find { it.isOperator }!!
        } catch (cause : NullPointerException) {
            console.sendError("Failed, no operators available")
            return
        }

        val options = Options(kind, altar, player)
        summonPerform(options)
        summonEffects(options)
    }

    private companion object {

        private fun PlayerEntity.giveItem(stack: ItemStack) {
            if (!giveItemStack(stack)) dropItem(stack, false)?.let { it.resetPickupDelay(); it.setOwner(uuid) }
        }

        private fun PlayerEntity.removeItem(predicate: (ItemStack) -> Boolean) {
            val inventory = inventory
            for (i in 0 until inventory.size()) {
                if (predicate(inventory.getStack(i))) inventory.setStack(i, ItemStack.EMPTY)
            }
        }

        private val theologyPairsDouble = setOf(
            TheologyPair(DEEP, DEEP),
            TheologyPair(OCCULT, OCCULT),
            TheologyPair(COSMOS, COSMOS),
            TheologyPair(BARTER, BARTER),
            TheologyPair(FLAME, FLAME),
        )
        private val theologyPairsOnce = setOf(
            TheologyPair(DEEP, OCCULT),
            TheologyPair(DEEP, COSMOS),
            TheologyPair(DEEP, BARTER),
            TheologyPair(OCCULT, BARTER),
        )

        private fun summonItem(kind: TheologyPair, item: Item, vararg lore: Text): Pair<TheologyPair, ItemStack> {
            val stack = ItemStack(item)

            val display = stack.getOrCreateSubNbt("display")
            display.putString("Name", Text.Serializer.toJson(kind.displayName))
            display.put("Lore", NbtList().also { list ->
                lore.forEach { list.add(NbtString.of(Text.Serializer.toJson(it))) }
            })

            return kind to stack
        }

        private val summonItems = mapOf<TheologyPair, ItemStack>(
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
                TheologyPair(DEEP, FLAME), FLINT_AND_STEEL,
                Text.of("Receive some firestarters and anvils"),
            ),
            summonItem(
                TheologyPair(BARTER, COSMOS), COOKED_BEEF,
                Text.of("Receive some steak"),
            ),
            summonItem(
                TheologyPair(FLAME, OCCULT), GHAST_SPAWN_EGG,
                Text.of("Spawn a Ghast in the arena"),
            ),
            summonItem(
                TheologyPair(DEEP, COSMOS), PRISMARINE_SHARD,
                Text.of("Acid rain starts pouring down"),
            ),
            summonItem(
                TheologyPair(BARTER, OCCULT), GOLDEN_SWORD,
                Text.of("Receive some instant-kill swords"),
            ),
            summonItem(
                TheologyPair(DEEP, OCCULT), COMPASS,
                Text.of("Reveal the locations of your enemies"),
            )
        )

        private val providersSuccess = mapOf<TheologyPair, (Options) -> TextProvider>(
            TheologyPair(DEEP, OCCULT) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Star and compass, map and sextant;")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" received trackers to hunt their enemies.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" received trackers for enemy players.")
            } },
            TheologyPair(DEEP, COSMOS) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Whirling clouds cast a stinging spittle!")
                override val subtitle get() =
                    Text.of("Acid rain! Stay indoors.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned stinging rain.")
            } },
            TheologyPair(DEEP, BARTER) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Flowing waters boil and bubble, you feel a bath would bring you trouble...")
                override val subtitle get() =
                    Text.of("Bodies of water are now harmful!")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" poisoned the water.")
            } },
            TheologyPair(DEEP, FLAME) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("From the heart of the forge,")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has summoned flint, steel, and anvils.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned flint, steel, and anvils.")
            } },
            TheologyPair(OCCULT, COSMOS) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Shining stars avert their gaze, a creeping void floods the maze...")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has gained fullbright and blinded everyone else.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has fullbright visibility.")
            } },
            TheologyPair(OCCULT, BARTER) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("You only have one shot.")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has gained powerful single-use swords.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" was granted powerful swords.")
            } },
            TheologyPair(OCCULT, FLAME) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Horrifying screams come from the darkness below!")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has summoned a ghast!")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned a ghast!")
            } },
            TheologyPair(COSMOS, BARTER) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Tis' a good season; A feast fit for royals!")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has received a bounty of steaks.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" summoned a steak feast.")
            } },
            TheologyPair(COSMOS, FLAME) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("A lingering flame burns inside!")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" has gained additional health.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" gained a health bonus.")
            } },
            TheologyPair(BARTER, FLAME) to { options -> object : TextProvider {
                override val title get() =
                    Text.of("Oh, the weather outside is frightful...")
                override val subtitle get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" gained a full stack of snow blocks.")
                override val tooltip get() =
                    Text.empty()
                        .append(options.team.displayName)
                        .append(" gained blocks of snow.")
            } },
            TheologyPair(DEEP, DEEP) to { options -> object : TextProvider {
                override val title get() =
                    Text.literal("FLASH FLOOD").formatted(DEEP.color)
                override val subtitle get() =
                    Text.of("The map is filling with water!")
                override val tooltip get() =
                    Text.of("The map is flooding.")
            } },
            TheologyPair(OCCULT, OCCULT) to { options -> object : TextProvider {
                override val title get() =
                    Text.literal("DESPERATION").formatted(OCCULT.color)
                override val subtitle get() =
                    Text.of("The health of the main teams has been halved, and each black team member has gained two kills!")
                override val tooltip get() =
                    Text.of("Team health has been halved, and all of black gained two kills.")
            } },
            TheologyPair(COSMOS, COSMOS) to { options -> object : TextProvider {
                override val title get() =
                    Text.literal("FINAL FRONTIER").formatted(COSMOS.color)
                override val subtitle get() =
                    Text.of("Gravity has been significantly reduced!")
                override val tooltip get() =
                    Text.of("Gravity is significantly reduced.")
            } },
            TheologyPair(BARTER, BARTER) to { options -> object : TextProvider {
                override val title get() =
                    Text.literal("MARKET CRASH").formatted(BARTER.color)
                override val subtitle get() =
                    Text.of("All special items have been lost!")
                override val tooltip get() =
                    Text.of("All special items have been lost.")
            } },
            TheologyPair(FLAME, FLAME) to { options -> object : TextProvider {
                override val title get() =
                    Text.literal("INFERNO").formatted(FLAME.color)
                override val subtitle get() =
                    Text.of("Most blocks are now flammable.")
                override val tooltip get() =
                    Text.of("Most blocks are now flammable.")
            } },
        )

        private val providersFailure = mapOf<Failure, (Options) -> TextProvider>(
            Failure.TIMEOUT to { object : FailureTextProvider(it) {
                override val subtitle get() =
                    Text.of("Let the cooldown complete first!")
            } },
            Failure.MISMATCH to { object : FailureTextProvider(it) {
                override val subtitle get() =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" can only be performed at a ")
                        .append(options.kind.theology1.displayName)
                        .append(" or ")
                        .append(options.kind.theology2.displayName)
                        .append(" altar!")
            } },
            Failure.REPEATED_DOUBLE to { object : FailureTextProvider(it) {
                override val subtitle get() =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" can only be performed once per game!")
            } },
            Failure.REPEATED_ONCE to { object : FailureTextProvider(it) {
                override val subtitle get() =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" can only be performed once per round!")
            } },
            Failure.REPEATED_SEQUENCE to { object : FailureTextProvider(it) {
                override val subtitle get() =
                    Text.empty()
                        .append(options.kind.displayName)
                        .append(" cannot be performed twice in a row!")
            } },
        )

    }

}
