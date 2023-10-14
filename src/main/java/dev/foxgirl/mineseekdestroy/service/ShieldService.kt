package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.entity.EntityStatuses
import net.minecraft.item.Items

class ShieldService : Service() {

    override fun update() {
        for ((player, playerEntity) in playerEntitiesIn) {
            if (playerEntity.isBlocking && (player.team === GameTeam.PLAYER_YELLOW || player.team === GameTeam.PLAYER_ARMADILLO)) {
                playerEntity.itemCooldownManager.set(Items.SHIELD, 100)
                playerEntity.clearActiveItem()
                playerEntity.world.sendEntityStatus(playerEntity, EntityStatuses.BREAK_SHIELD)
            }
        }
    }

}
