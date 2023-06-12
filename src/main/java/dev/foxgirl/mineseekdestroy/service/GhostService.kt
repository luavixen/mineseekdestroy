package dev.foxgirl.mineseekdestroy.service

import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
import net.minecraft.scoreboard.Team

class GhostService : Service() {

    override fun update() {
        for ((player, playerEntity) in playerEntitiesNormal) {
            if (player.isGhost) {
                playerEntity.addStatusEffect(StatusEffectInstance(StatusEffects.INVISIBILITY, 80))
                playerEntitiesIn.values.forEach {
                    it.setInPowderSnow(it.squaredDistanceTo(playerEntity) <= 16)
                    val team = it.scoreboardTeam as? Team
                    if (team != null) {
                        it.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(team, player.name, TeamS2CPacket.Operation.ADD))
                    }
                }
            }
        }
    }

}
