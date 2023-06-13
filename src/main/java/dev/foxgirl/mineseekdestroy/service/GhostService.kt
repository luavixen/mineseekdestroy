package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.state.PlayingGameState
import dev.foxgirl.mineseekdestroy.util.Scheduler
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.block.Blocks
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.damage.DamageTypes.*
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
import net.minecraft.registry.RegistryKey
import net.minecraft.scoreboard.Team
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import java.util.*

class GhostService : Service() {

    private var schedule: Scheduler.Schedule? = null

    private fun updateGhosts() {
        val running = state is PlayingGameState

        for ((player, playerEntity) in playerEntitiesNormal) {

            if (player.isGhost) {

                val goingGhost = running && player.isAlive

                if (goingGhost) {
                    playerEntitiesIn.values.forEach {
                        if (it.squaredDistanceTo(playerEntity) <= 16) {
                            it.frozenTicks = Math.min(it.frozenTicks + 3, it.minFreezeDamageTicks)
                        }
                    }
                    playerEntity.addStatusEffect(StatusEffectInstance(StatusEffects.INVISIBILITY, 80))
                } else {
                    playerEntity.removeStatusEffect(StatusEffects.INVISIBILITY)
                }

                playerEntities.values.forEach {
                    val team = if (goingGhost) it.scoreboardTeam as? Team else player.scoreboardTeam
                    if (team != null) {
                        it.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(team, player.name, TeamS2CPacket.Operation.ADD))
                    }
                }

                playerEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!.addPersistentModifier(healthModifier)

            } else {

                playerEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!.removeModifier(healthModifier)

            }

        }
    }

    override fun update() {
        if (schedule == null) {
            schedule = Scheduler.delay(1.0) { updateGhosts() }
        }
    }

    fun handleInteract(player: GamePlayer, pos: BlockPos): ActionResult {
        world.setBlockState(pos, Blocks.MAGENTA_CONCRETE_POWDER.defaultState)
        player.inventory?.insertStack(Game.stackEctoplasm.copy())
        return ActionResult.SUCCESS
    }

    fun shouldIgnoreDamage(key: RegistryKey<DamageType>?) = ignoredDamageTypes.contains(key)

    private companion object {

        private val healthModifier =
            EntityAttributeModifier(UUID.fromString("95880240-c1f7-4660-8e0e-a14f13e2cf41"), "msd_ghost_health", -16.0, EntityAttributeModifier.Operation.ADDITION)

        private val ignoredDamageTypes = immutableSetOf(
            LIGHTNING_BOLT, LAVA, HOT_FLOOR, IN_WALL, CRAMMING, DROWN, STARVE,
            CACTUS, FALL, FLY_INTO_WALL, DRY_OUT, SWEET_BERRY_BUSH, FREEZE,
            STALAGMITE, FALLING_BLOCK, FALLING_ANVIL, FALLING_STALACTITE,
        )

    }

}
