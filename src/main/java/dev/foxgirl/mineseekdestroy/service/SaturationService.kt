package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.state.PlayingGameState

class SaturationService : Service() {

    fun handleUpdate() {
        val running = state is PlayingGameState
        for (player in players) {
            if (running && player.isPlaying) continue
            val entity = player.entity ?: continue
            entity.health = entity.maxHealth
            entity.hungerManager.apply {
                foodLevel = 20
                saturationLevel = 5.0F
                exhaustion = 0.0F
            }
        }
    }

}
