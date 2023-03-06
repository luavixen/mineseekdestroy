package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.state.RunningGameState

class SaturationService : Service() {

    override fun update() {
        val running = state is RunningGameState
        for (player in players) {
            if (running && player.isPlaying) continue
            val entity = player.entity ?: continue
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
