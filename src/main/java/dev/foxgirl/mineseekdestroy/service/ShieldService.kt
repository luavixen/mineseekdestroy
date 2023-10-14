package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.entity.EntityStatuses
import net.minecraft.item.Items

class ShieldService : Service() {

    override fun update() {
        for ((player, playerEntity) in playerEntitiesIn) {
            if (player.team === GameTeam.PLAYER_YELLOW && playerEntity.isBlocking) {
                playerEntity.itemCooldownManager.set(Items.SHIELD, 100)
                playerEntity.clearActiveItem()
                playerEntity.world.sendEntityStatus(playerEntity, EntityStatuses.BREAK_SHIELD)
            }
        }
    }

}
