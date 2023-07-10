package dev.foxgirl.mineseekdestroy.service

class SaturationService : Service() {

    override fun update() {
        val running = state.isRunning
        for ((player, entity) in playerEntitiesNormal) {
            if (running && player.isPlayingOrGhost) continue
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
