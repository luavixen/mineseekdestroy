package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.BlockFinder
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
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
                if (player.team === GameTeam.PLAYER_BLACK) {
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
            // TODO: GRAHHHHHH Replace Many Blocks! Ough.
        }
    }

    private val summons = mapOf<TheologyPair, (Options) -> Summon>(
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
    )

    private val summonListGame = mutableListOf<Summon>()
    private val summonListRound = mutableListOf<Summon>()

    private var altars = mapOf<BlockPos, Altar>()

    private var timeout = Instant.now()

    override fun setup() {
        fun findTheologyAt(pos: BlockPos): Theology? {
            return when (world.getBlockState(pos).block) {
                Blocks.DARK_PRISMARINE -> DEEP
                Blocks.DARK_OAK_TRAPDOOR -> OCCULT
                Blocks.OBSIDIAN -> COSMOS
                Blocks.WAXED_CUT_COPPER -> BARTER
                Blocks.NETHER_BRICKS -> FLAME
                else -> null
            }
        }
        fun findTheology(pos: BlockPos): Theology? =
            findTheologyAt(pos.up()) ?: findTheologyAt(pos.down())

        BlockFinder
            .search(world, properties.regionAll) {
                it.block === Blocks.FLETCHING_TABLE
            }
            .handle { results, err ->
                if (err != null) {
                    logger.error("SpecialSummonService search for altars failed")
                } else {
                    logger.info("SpecialSummonService search for altars returned ${results.size} result(s)")
                    altars = results
                        .mapNotNull { Altar(it.pos, findTheology(it.pos) ?: return@mapNotNull null) }
                        .associateBy { it.pos }
                }
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
        if (timeoutCheck()) return true

        val kind = options.kind
        if (kind.isDouble && summonListGame.any { it.kind == kind }) return true
        if (kind.isOnce && summonListRound.any { it.kind == kind }) return true
        if (kind == summonListRound.lastOrNull()?.kind) return true

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
        if (failCheck(options)) {
            failPerform(options)
        } else {
            summonPerform(options)
        }
        summonEffects(options)
    }

    override fun update() {
        summonListGame.forEach { it.update() }
    }

    fun handleRoundEnd() {
        summonListRound.forEach {
            if (it is Stoppable) {
                it.stop()
                summonListGame.remove(it)
            }
        }
        summonListRound.clear()
        timeoutReset()
    }

    // TODO: THIS IS A HACK !! JUST FOR TESTING !!
    var debounce = Instant.now()

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        // TODO: THIS IS A HACK !! JUST FOR TESTING !!
        if (debounce.isAfter(Instant.now())) return ActionResult.FAIL
        else debounce = Instant.now().plusSeconds(3)

        val altar: Altar? = altars[pos]
        if (altar != null) {
            Game.CONSOLE_PLAYERS.sendInfo("Player", player.displayName, "opened altar at", pos, "with theology", altar.theology.name)
            summon(Options(summons.keys.random(), altar, player))
        } else {
            Game.CONSOLE_PLAYERS.sendInfo("Player", player.displayName, "tried to open invalid altar at", pos)
        }

        return ActionResult.FAIL
    }

}
