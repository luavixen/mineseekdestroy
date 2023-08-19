package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameProperties
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.Selection
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.BlockPos
import kotlin.math.sqrt

class SpecialBoosterService : Service() {

    private class TrackerResults(
        private val entered: List<LivingEntity>,
        private val leaving: List<LivingEntity>,
    ) {
        operator fun component1() = entered
        operator fun component2() = leaving
    }

    private class Tracker(
        private val selection: Selection,
        private val action: (TrackerResults) -> Unit,
    ) {
        private var inside = emptyList<LivingEntity>()

        fun update(all: Array<LivingEntity>) {
            val entered = all.filter { it in selection }.ifEmpty { emptyList() }
            val leaving = inside.filter { it !in entered }.ifEmpty { emptyList() }
            inside = entered
            action(TrackerResults(entered, leaving))
        }
    }

    private lateinit var trackers: List<Tracker>

    override fun setup() {
        val fanTracker = Tracker(properties.regionBlimpFans) { (entered, leaving) ->
            entered.forEach {
                it.frozenTicks = 120
                it.addStatusEffect(StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    (game.getRuleDouble(Game.RULE_FANS_EFFECT_DURATION) * 20.0).toInt(),
                ))
            }
            leaving.forEach {
                it.addVelocity(0.0, game.getRuleDouble(Game.RULE_FANS_KNOCKBACK), 0.0)
                if (it is ServerPlayerEntity) {
                    it.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(it))
                    it.velocityDirty = false
                }
            }
        }

        if (properties is GameProperties.Realm) {
            val towerRegions = listOf(
                Region(BlockPos(-1268, 124, 235), BlockPos(-1262, 131, 241)),
                Region(BlockPos(-1144, 136, 239), BlockPos(-1150, 129, 245)),
                Region(BlockPos(-1379, 143, 177), BlockPos(-1373, 136, 183)),
                Region(BlockPos(-1290, 151, 62), BlockPos(-1296, 144, 68)),
            )
            val towerTrackers = towerRegions.map { region ->
                Tracker(region) { (entered, leaving) ->
                    entered.forEach {
                        it.frozenTicks = 120
                        it.addStatusEffect(StatusEffectInstance(
                            StatusEffects.SLOW_FALLING,
                            (game.getRuleDouble(Game.RULE_TOWER_EFFECT_DURATION) * 20.0).toInt(),
                        ))
                    }
                    leaving.forEach {
                        val p1 = region.center
                        val p2 = it.pos

                        var pushX = p1.x - p2.x
                        var pushY = p1.z - p2.z

                        sqrt(pushX * pushX + pushY * pushY).let { magnitude ->
                            pushX /= magnitude
                            pushY /= magnitude
                        }

                        it.takeKnockback(game.getRuleDouble(Game.RULE_TOWER_KNOCKBACK), pushX, pushY)
                        if (it is ServerPlayerEntity) {
                            it.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(it))
                            it.velocityDirty = false
                        }
                    }
                }
            }
            trackers = towerTrackers + fanTracker
        } else {
            trackers = listOf(fanTracker)
        }
    }

    override fun update() {
        val living = world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity::class.java), { true }).toTypedArray()
        for (tracker in trackers) tracker.update(living)
    }

}
