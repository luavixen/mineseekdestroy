package dev.foxgirl.mineseekdestroy.service

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
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
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
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.time.Duration
import java.time.Instant

class SpecialSummonsService : Service() {

    private enum class Theology {
        DEEP {
            override val color = Formatting.DARK_AQUA
        },
        OCCULT {
            override val color = Formatting.LIGHT_PURPLE
        },
        COSMOS {
            override val color = Formatting.BLUE
        },
        BARTER {
            override val color = Formatting.GOLD
        },
        FLAME {
            override val color = Formatting.RED
        };

        abstract val color: Formatting
        val displayName: Text get() = Text.literal(name).formatted(color)
    }

    private class TheologyPair(theology1: Theology, theology2: Theology) {
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

            world.setWeather(24000 * 10, 0, true, false)
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
        world.playSound(
            null,
            options.pos,
            SoundEvents.ENTITY_HORSE_DEATH,
            SoundCategory.PLAYERS,
            3.0F, 0.5F,
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
        EntityType.LIGHTNING_BOLT.spawn(world, options.pos, SpawnReason.COMMAND)
    }

    private fun summon(options: Options) {
        Game.CONSOLE_OPERATORS.sendInfo(
            "Player", options.player.displayName,
            "attempting summon of type", options.kind.displayName,
            "with theology", options.altar.theology.displayName,
            "with altar at", options.altar.pos,
        )

        val failure = failCheck(options)
        if (failure != null) {
            Game.CONSOLE_OPERATORS.sendError("Summon", options.kind, "failed:", failure)
            failPerform(options)
        } else {
            Game.CONSOLE_OPERATORS.sendError("Summon", options.kind, "success!")
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
                Game.CONSOLE_OPERATORS.sendInfo("Summon stopping:", it.javaClass.simpleName)

                summonListGame.remove(it)
                it.stop()
            }
        }

        summonListRound.clear()
        timeoutReset()
    }

    private inner class AltarScreenHandler(
        val altar: Altar,
        syncId: Int,
        val playerInventory: PlayerInventory,
        val playerEntity: ServerPlayerEntity,
    ) : AnvilScreenHandler(syncId, playerInventory) {
        private fun player(): GamePlayer =
            this@SpecialSummonsService.context.getPlayer(playerEntity)

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
                val failure = failCheck(Options(pair, altar, player()))
                if (failure != null) {
                    newItemName = "SUMMON WILL FAIL: " + failure.name
                } else {
                    newItemName = "SUMMON IS READY!"
                }
            } else {
                output.setStack(0, ItemStack.EMPTY)
                newItemName = "SUMMON INVALID."
            }
        }

        override fun onTakeOutput(playerEntity: PlayerEntity, stack: ItemStack) {
            stack.count = 0
            input.removeStack(0, 1)
            input.removeStack(0, 1)

            val pair = theologies()
            if (pair != null) {
                summon(Options(pair, altar, player()))
            }
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
            AltarScreenHandler(altar, syncId, playerInventory, playerEntity as ServerPlayerEntity)
    }

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        // TODO: this code sucks!! fix it. make it so that it doesnt also use your item.

        val altar: Altar? = altars[pos]
        if (altar == null) {
            Game.LOGGER.info("Player ${player.name} failed to open invalid altar at ${pos}")
            return ActionResult.PASS
        } else {
            Game.LOGGER.info("Player ${player.name} opened altar at ${pos} with theology ${altar.theology}")
            player.entity?.openHandledScreen(AltarNamedScreenHandlerFactory(altar))
            return ActionResult.FAIL
        }
    }

    private companion object {

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
        )

    }

}
