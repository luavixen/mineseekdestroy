package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameProperties
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import kotlin.math.sqrt

class SpecialTowerService : Service() {

    private val regions = Region.Set(
        Region(BlockPos(-1268, 124, 235), BlockPos(-1262, 131, 241)),
        Region(BlockPos(-1144, 136, 239), BlockPos(-1150, 129, 245)),
        Region(BlockPos(-1379, 143, 177), BlockPos(-1373, 136, 183)),
        Region(BlockPos(-1290, 151, 62), BlockPos(-1296, 144, 68)),
    )

    private val duration get() = (game.getRuleDouble(Game.RULE_TOWER_EFFECT_DURATION) * 20.0).toInt()

    private var entities = listOf<Pair<ServerPlayerEntity, Region>>()

    override fun update() {
        if (properties !== GameProperties.Realm) return

        val entitiesOld = entities
        val entitiesNew = mutableListOf<Pair<ServerPlayerEntity, Region>>()

        for ((_, entity) in playerEntitiesIn) {
            val region = regions.firstOrNull { it.contains(entity) }
            if (region != null) {
                entitiesNew.add(entity to region)
                entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOW_FALLING, duration))
            }
        }

        for ((entity, region) in entitiesOld) {
            if (entitiesNew.any { it.first === entity }) continue

            val p1 = region.center
            val p2 = entity.pos

            var pushX = p1.x - p2.x
            var pushY = p1.z - p2.z

            sqrt(pushX * pushX + pushY * pushY).let {
                pushX /= it
                pushY /= it
            }

            entity.takeKnockback(game.getRuleDouble(Game.RULE_TOWER_KNOCKBACK), pushX, pushY)

            entity.networkHandler.sendPacket(EntityVelocityUpdateS2CPacket(entity))
            entity.velocityDirty = false

            entity.frozenTicks = 120
        }

        if (entitiesNew.isNotEmpty()) {
            this.entities = entitiesNew
        } else {
            this.entities = listOf()
        }
    }

}
