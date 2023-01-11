package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameProperties
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.math.BlockPos

class TowerService : Service() {

    private val regions = listOf(
        Region(BlockPos(-1268, 124, 235), BlockPos(-1262, 131, 241)),
        Region(BlockPos(-1144, 136, 239), BlockPos(-1150, 129, 239)),
        Region(BlockPos(-1379, 143, 177), BlockPos(-1373, 136, 183)),
        Region(BlockPos(-1290, 151, 62), BlockPos(-1296, 144, 68)),
    )

    private val duration get() = (game.getRuleDouble(Game.RULE_TOWER_EFFECT_DURATION) * 20.0).toInt()

    fun handleUpdate() {
        if (properties != GameProperties.Realm) return
        for (player in players) {
            val entity = player.entity ?: continue
            if (regions.any { it.contains(entity) }) {
                entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOW_FALLING, duration))
            }
        }
    }

}
