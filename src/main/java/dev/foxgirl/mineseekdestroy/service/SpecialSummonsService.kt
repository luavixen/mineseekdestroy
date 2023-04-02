package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
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
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.time.Instant

class SpecialSummonsService : Service() {

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

        override fun toString() = "SummonKind(theology1=${theology1}, theology2=${theology2})"
        override fun hashCode() = 31 * theology1.hashCode() + theology2.hashCode()
        override fun equals(other: Any?) =
            other === this || (other is SummonKind && other.theology1 === theology1 && other.theology2 === theology2)
    }

    private data class SummonOptions(
        val altar: Altar,
        val kind: SummonKind,

        val player: GamePlayer,
        val team: GameTeam,

        val time: Instant,
    )

    private abstract inner class Summon(private val options: SummonOptions) : Comparable<Summon> {
        val altar get() = options.altar
        val kind get() = options.kind

        val player get() = options.player
        val team get() = options.team

        val time get() = options.time

        val pos: BlockPos get() = player.entity?.blockPos ?: altar.pos

        open fun update() {}
        open fun start() {}
        open fun stop() {}

        open fun fail() {
            for (player in players) {
                if (player.team === team) {
                    player.entity?.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 80))
                }
            }
            world.playSoundAtBlockCenter(pos, SoundEvents.ENTITY_HORSE_DEATH, SoundCategory.PLAYERS, 0.5F, 0.5F, true)
        }

        override fun compareTo(other: Summon) = time.compareTo(other.time)

        override fun toString() = "Summon(options=${options})"
        override fun hashCode() = options.hashCode()
        override fun equals(other: Any?) =
            other === this || (other is Summon && other.options == options)
    }

    private abstract inner class DeepSummon(options: SummonOptions) : Summon(options) {
        override fun fail() {
            super.fail()
            EntityType.GUARDIAN.spawn(world, pos, SpawnReason.COMMAND)
        }
    }

    private abstract inner class OccultSummon(options: SummonOptions) : Summon(options) {
        override fun fail() {
            super.fail()
            for (i in 0..5) EntityType.ZOMBIE.spawn(world, pos, SpawnReason.COMMAND)
        }
    }

    private abstract inner class CosmosSummon(options: SummonOptions) : Summon(options) {
        override fun fail() {
            super.fail()
            for (i in 0..2) EntityType.PHANTOM.spawn(world, pos, SpawnReason.COMMAND)
        }
    }

    private abstract inner class BarterSummon(options: SummonOptions) : Summon(options) {
        override fun fail() {
            super.fail()
            EntityType.ILLUSIONER.spawn(world, pos, SpawnReason.COMMAND)
        }
    }

    private abstract inner class FlameSummon(options: SummonOptions) : Summon(options) {
        override fun fail() {
            super.fail()
            for (i in 0..2) EntityType.BLAZE.spawn(world, pos, SpawnReason.COMMAND)
        }
    }

    private inner class OccultOccultSummon(options: SummonOptions) : OccultSummon(options) {
        override fun start() {
            for (player in players) {
                if (player.team === GameTeam.PLAYER_BLACK) player.kills += 2
            }
            for ((player, entity) in playerEntitiesIn) {
                if (player.team !== GameTeam.PLAYER_BLACK) entity.health /= 2
            }
        }
    }

    private inner class OccultCosmosSummon(options: SummonOptions) : OccultSummon(options) {
        override fun start() {
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

    private inner class OccultBarterSummon(options: SummonOptions) : OccultSummon(options) {
        override fun start() {
            val item = ItemStack(Items.GOLDEN_SWORD).apply {
                addEnchantment(Enchantments.SHARPNESS, 666)
                setDamage(32)
            }
            for ((player, entity) in playerEntitiesIn) {
                if (player.team === team) {
                    if (!entity.inventory.insertStack(item.copy())) entity.dropItem(item.copy(), false)
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

    private inner class OccultFlameSummon(options: SummonOptions) : OccultSummon(options) {
        override fun start() {
            val region = properties.regionBlimp
            val position = BlockPos(region.center.x.toInt(), region.end.y - 5, region.center.z.toInt())
            EntityType.GHAST.spawn(world, position, SpawnReason.COMMAND)
        }
    }

    private val summonKinds = mapOf<SummonKind, (SummonOptions) -> Summon>(
        SummonKind(Theology.OCCULT, Theology.OCCULT) to ::OccultOccultSummon,
        SummonKind(Theology.OCCULT, Theology.COSMOS) to ::OccultCosmosSummon,
        SummonKind(Theology.OCCULT, Theology.BARTER) to ::OccultBarterSummon,
        SummonKind(Theology.OCCULT, Theology.FLAME) to ::OccultFlameSummon,
    )

    private val summons = sortedSetOf<Summon>()

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
                                Blocks.DARK_PRISMARINE -> Theology.DEEP
                                Blocks.DARK_OAK_TRAPDOOR -> Theology.OCCULT
                                Blocks.OBSIDIAN -> Theology.COSMOS
                                Blocks.WAXED_CUT_COPPER -> Theology.BARTER
                                Blocks.NETHER_BRICKS -> Theology.FLAME
                                else -> return@mapNotNull null
                            }
                            Altar(it.pos, theology)
                        }
                        .associateBy { it.pos }
                }
            }
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
