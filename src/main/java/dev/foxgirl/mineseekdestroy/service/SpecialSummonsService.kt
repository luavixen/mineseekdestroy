package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.BlockFinder
import dev.foxgirl.mineseekdestroy.util.Inventories
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import java.time.Duration
import java.time.Instant

class SpecialSummonsService : Service() {

    private fun ServerPlayerEntity.give(stack: ItemStack) {
        if (!this.giveItemStack(stack)) this.dropItem(stack, false)?.let { it.resetPickupDelay(); it.setOwner(this.uuid) }
    }

    private enum class Theology {
        DEEP, OCCULT, COSMOS, BARTER, FLAME,
    }

    private data class Altar(
        val pos: BlockPos,
        val theology: Theology,
    )

    private class SummonKind(theology1: Theology, theology2: Theology) {
        val theology1 = minOf(theology1, theology2)
        val theology2 = maxOf(theology1, theology2)

        val isDouble get() = theology1 === theology2

        fun isTheology(theology: Theology) = theology1 === theology || theology2 === theology

        override fun toString() = "SummonKind(theology1=${theology1}, theology2=${theology2})"
        override fun hashCode() = 31 * theology1.hashCode() + theology2.hashCode()
        override fun equals(other: Any?) =
            other === this || (other is SummonKind && other.theology1 === theology1 && other.theology2 === theology2)
    }

    private data class SummonOptions(
        val kind: SummonKind,
        val altar: Altar,
        val player: GamePlayer,
        val team: GameTeam,
        val time: Instant = Instant.now(),
    ) {
        val pos: BlockPos get() = player.entity?.blockPos ?: altar.pos
    }

    private abstract inner class Summon(private val options: SummonOptions) : Comparable<Summon> {
        val kind get() = options.kind
        val altar get() = options.altar
        val player get() = options.player
        val team get() = options.team
        val time get() = options.time
        val pos get() = options.pos

        open fun timeout(): Duration = Duration.ZERO
        val expires = time.plus(timeout())

        open fun perform() {}
        open fun update() {}

        override fun compareTo(other: Summon) = time.compareTo(other.time)
    }

    private interface Stoppable {
        fun stop()
    }

    private inner class DeepFlameSummon(options: SummonOptions) : Summon(options) {
        override fun perform() {
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) {
                    entity.give(ItemStack(Items.FLINT_AND_STEEL).apply { addEnchantment(Enchantments.UNBREAKING, 3) })
                    entity.give(ItemStack(Items.ANVIL))
                }
            }
        }
    }

    private inner class OccultOccultSummon(options: SummonOptions) : Summon(options) {
        override fun perform() {
            for (player in players) {
                if (player.team === GameTeam.PLAYER_BLACK) player.kills += 2
            }
            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== GameTeam.PLAYER_BLACK) entity.health /= 2
            }
        }
    }

    private inner class OccultCosmosSummon(options: SummonOptions) : Summon(options), Stoppable {
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

    private inner class OccultBarterSummon(options: SummonOptions) : Summon(options), Stoppable {
        override fun perform() {
            val item = ItemStack(Items.GOLDEN_SWORD).apply {
                addEnchantment(Enchantments.SHARPNESS, 666)
                setDamage(32)
            }
            for ((player, entity) in playerEntitiesNormal) {
                if (player.team === team) {
                    entity.give(item.copy())
                }
            }
        }
        override fun stop() {
            for ((_, entity) in playerEntitiesNormal) {
                Inventories.list(entity.inventory).let { list ->
                    list.forEachIndexed { i, stack ->
                        if (stack.item === Items.GOLDEN_SWORD) list[i] = ItemStack.EMPTY
                    }
                }
            }
        }
    }

    private inner class OccultFlameSummon(options: SummonOptions) : Summon(options) {
        override fun perform() {
            val region = properties.regionBlimp
            val position = BlockPos(region.center.x.toInt(), region.end.y - 5, region.center.z.toInt())
            EntityType.GHAST.spawn(world, position, SpawnReason.COMMAND)
        }
    }

    private val summons = mapOf<SummonKind, (SummonOptions) -> Summon>(
        SummonKind(DEEP, FLAME) to ::DeepFlameSummon,
        SummonKind(OCCULT, OCCULT) to ::OccultOccultSummon,
        SummonKind(OCCULT, COSMOS) to ::OccultCosmosSummon,
        SummonKind(OCCULT, BARTER) to ::OccultBarterSummon,
        SummonKind(OCCULT, FLAME) to ::OccultFlameSummon,
    )

    private val summonSetGame = sortedSetOf<Summon>()
    private val summonSetRound = sortedSetOf<Summon>()

    private var altars = mapOf<BlockPos, Altar>()

    override fun setup() {
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
                        .mapNotNull {
                            val theology = when (world.getBlockState(it.pos.down()).block) {
                                Blocks.DARK_PRISMARINE -> DEEP
                                Blocks.DARK_OAK_TRAPDOOR -> OCCULT
                                Blocks.OBSIDIAN -> COSMOS
                                Blocks.WAXED_CUT_COPPER -> BARTER
                                Blocks.NETHER_BRICKS -> FLAME
                                else -> return@mapNotNull null
                            }
                            Altar(it.pos, theology)
                        }
                        .associateBy { it.pos }
                }
            }
    }

    private fun fail(options: SummonOptions) {
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
        world.playSoundAtBlockCenter(options.pos, SoundEvents.ENTITY_HORSE_DEATH, SoundCategory.PLAYERS, 0.5F, 0.5F, true)
    }

    private fun willFail(kind: SummonKind): Boolean {
        if (summonSetGame.last()?.kind == kind) return true
        return false
    }

    private fun summon(options: SummonOptions) {
        val failure = (if (options.kind.isDouble) summonSetGame else summonSetRound).any { it.kind == options.kind }
        if (failure) {
            fail(options)
        } else {
            val summon = summons[options.kind]?.invoke(options) ?: return
            summonSetGame.add(summon)
            summonSetRound.add(summon)
            summon.perform()
        }
    }

    override fun update() {
        summonSetGame.forEach { it.update() }
    }

    fun handleRoundEnd() {
        summonSetRound.forEach {
            if (it is Stoppable) {
                it.stop()
                summonSetGame.remove(it)
            }
        }
        summonSetRound.clear()
    }

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        val altar: Altar? = altars[pos]
        if (altar != null) {
            Game.CONSOLE_OPERATORS.sendInfo("Player", player.displayName, "opened altar at", pos, "with theology", altar.theology.name)
        } else {
            Game.CONSOLE_OPERATORS.sendInfo("Player", player.displayName, "tried to open invalid altar at", pos)
        }
        return ActionResult.FAIL
    }

}
