package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Rules
import dev.foxgirl.mineseekdestroy.util.addEffect
import dev.foxgirl.mineseekdestroy.util.player
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity

class SyphonService : Service() {

    fun handleDamageTaken(playerEntity: ServerPlayerEntity, damageSource: DamageSource, damageAmount: Float) {
        if (damageAmount < 0.1F) return

        val player = context.getPlayer(playerEntity)
        if (player.team != GameTeam.BLACK || !Rules.blackSyphonHealthBackEnabled) return

        val attacker = damageSource.player ?: return
        val attackerEntity = attacker.entity ?: return

        val syphonAmount = damageAmount / Rules.blackSyphonHealthBackDivisor.toFloat()

        attackerEntity.absorptionAmount.let { absorptionAmount ->
            attackerEntity.addEffect(StatusEffects.ABSORPTION, Double.MAX_VALUE, 128)
            attackerEntity.absorptionAmount = absorptionAmount + syphonAmount
            logger.info("Syphoning ${String.format("%.1f", damageAmount)} damage from ${player.nameQuoted} to ${attacker.nameQuoted}, now has ${String.format("%.1f", attackerEntity.absorptionAmount)} absorption")
        }
    }

}
