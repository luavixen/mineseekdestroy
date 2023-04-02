package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.state.RunningGameState

class SaturationService : Service() {

    override fun update() {
        val running = state is RunningGameState
        for ((player, entity) in playerEntities) {
            if (running && player.isPlaying) continue
            if (entity.isDead || entity.isRemoved) continue
            entity.health = entity.maxHealth
            entity.hungerManager.apply {
                foodLevel = 20
                saturationLevel = 5.0F
                exhaustion = 0.0F
            }
        }
    }

}
