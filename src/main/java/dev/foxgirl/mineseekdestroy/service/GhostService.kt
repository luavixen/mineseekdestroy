package dev.foxgirl.mineseekdestroy.service

class GhostService : Service() {

    override fun update() {
        for ((player, playerEntity) in playerEntitiesNormal) {
            if (player.isGhost) {
                playerEntitiesIn.values.forEach {
                    it.setInPowderSnow(it.squaredDistanceTo(playerEntity) <= 16)
                }
            }
        }
    }

}
