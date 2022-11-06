package dev.foxgirl.mineseekdestroy.service

import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket
import net.minecraft.server.network.ServerPlayerEntity

class GlowService : Service() {

    private var broadcastTicks = 0

    private fun broadcast() {
        val players = players
        val effect = StatusEffectInstance(StatusEffects.GLOWING, 50, 1, true, false)

        val packets = buildList(players.size) {
            for (player in players) {
                if (!player.isPlaying || !player.isAlive) continue
                val entity = player.entity ?: continue
                add(EntityStatusEffectS2CPacket(entity.id, effect))
            }
        }

        for (player in players) {
            if (player.isPlaying && player.isAlive) continue
            val entity = player.entity ?: continue
            packets.forEach { entity.networkHandler.sendPacket(it) }
        }
    }

    fun handleUpdate() {
        if (broadcastTicks >= 20) {
            broadcastTicks = 0
            broadcast()
        } else {
            broadcastTicks++
        }
    }

}
