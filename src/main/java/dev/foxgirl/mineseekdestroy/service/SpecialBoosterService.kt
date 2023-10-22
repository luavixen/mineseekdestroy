package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameProperties
import dev.foxgirl.mineseekdestroy.util.Broadcast
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.Rules
import dev.foxgirl.mineseekdestroy.util.Selection
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import kotlin.math.sqrt

class SpecialBoosterService : Service() {

    private class Tracker(
        val selection: Selection,
        val actionEntered: (ServerPlayerEntity) -> Unit,
        val actionLeaving: (ServerPlayerEntity) -> Unit,
    ) {
        private val entries = HashMap<GamePlayer, TrackerEntry>()

        fun tick(playersEntities: List<Pair<GamePlayer, ServerPlayerEntity>>) {
            playersEntities.forEach { (player, playerEntity) ->
                entries
                    .computeIfAbsent(player) { TrackerEntry() }
                    .tick(this, playerEntity)
            }
        }
    }

    private class TrackerEntry {
        private var ticksSinceLeavingSelection = 0
        private var previouslyInSelection = false

        fun tick(tracker: Tracker, playerEntity: ServerPlayerEntity) {
            if (tracker.selection.contains(playerEntity)) {
                tracker.actionEntered(playerEntity)
                previouslyInSelection = true
            } else {
                if (previouslyInSelection) {
                    previouslyInSelection = false
                    if (ticksSinceLeavingSelection > 10) {
                        ticksSinceLeavingSelection = 0
                        tracker.actionLeaving(playerEntity)
                    }
                } else {
                    ticksSinceLeavingSelection++
                }
            }
        }
    }

    private val trackers = mutableListOf<Tracker>()

    override fun setup() {
        trackers += Tracker(
            properties.regionBlimpFans,
            {
                logger.info("Player '${it.entityName}' entered blimp fans")

                it.frozenTicks = 120
                it.addStatusEffect(StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    (Rules.fansEffectDuration * 20.0).toInt(),
                ))
            },
            {
                logger.info("Player '${it.entityName}' launched by blimp fans")

                it.addVelocity(0.0, Rules.fansKnockback, 0.0)
                it.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(it))
                it.velocityDirty = false

                Broadcast.sendSound(
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS,
                    1.0F, 1.0F,
                    world, it.pos,
                )
            },
        )

        if (properties is GameProperties.Realm) {
            for (region in arrayOf(
                Region(BlockPos(-1268, 124, 235), BlockPos(-1262, 131, 241)),
                Region(BlockPos(-1144, 136, 239), BlockPos(-1150, 129, 245)),
                Region(BlockPos(-1379, 143, 177), BlockPos(-1373, 136, 183)),
                Region(BlockPos(-1290, 151, 62), BlockPos(-1296, 144, 68)),
            )) {
                trackers += Tracker(
                    region,
                    {
                        logger.info("Player '${it.entityName}' entered tower")

                        it.frozenTicks = 120
                        it.addStatusEffect(StatusEffectInstance(
                            StatusEffects.SLOW_FALLING,
                            (Rules.towerEffectDuration * 20.0).toInt(),
                        ))
                    },
                    {
                        logger.info("Player '${it.entityName}' launched by tower")

                        val p1 = region.center
                        val p2 = it.pos

                        var pushX = p1.x - p2.x
                        var pushY = p1.z - p2.z

                        sqrt(pushX * pushX + pushY * pushY).let { magnitude ->
                            pushX /= magnitude
                            pushY /= magnitude
                        }

                        it.takeKnockback(Rules.towerKnockback, pushX, pushY)
                        it.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(it))
                        it.velocityDirty = false

                        Broadcast.sendSound(
                            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                            SoundCategory.PLAYERS,
                            1.0F, 1.0F,
                            world, it.pos,
                        )
                    },
                )
            }
        }
    }

    override fun update() {
        trackers.forEach { it.tick(playerEntitiesNormal) }
    }

}
