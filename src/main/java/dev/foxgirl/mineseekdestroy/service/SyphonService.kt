package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Rules
import dev.foxgirl.mineseekdestroy.util.addEffect
import dev.foxgirl.mineseekdestroy.util.getEffect
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity

class SyphonService : Service() {

    fun handleDamageTaken(player: GamePlayer, playerEntity: ServerPlayerEntity, damageSource: DamageSource, damageAmount: Float) {
        if (player.team != GameTeam.BLACK || !Rules.blackSyphonHealthBackEnabled) return

        val attackerEntity = damageSource.source as? ServerPlayerEntity ?: return
        val attacker = context.getPlayer(attackerEntity)

        val amplifier =
            (attackerEntity.getEffect(StatusEffects.ABSORPTION)?.amplifier ?: 0) +
            (damageAmount.coerceAtMost(playerEntity.health) / Rules.blackSyphonHealthBackDivisor).toInt()

        attackerEntity.addEffect(StatusEffects.ABSORPTION, Double.MAX_VALUE, amplifier)

        logger.info("Syphoning ${String.format("%.1f", damageAmount)} damage from ${player.name} to ${attacker.name}, now has absorption ${amplifier + 1}")
    }

}
