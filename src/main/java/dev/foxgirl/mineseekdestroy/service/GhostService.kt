package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Scheduler
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.block.Blocks
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.damage.DamageTypes.*
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
import net.minecraft.registry.RegistryKey
import net.minecraft.scoreboard.Team
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import java.util.*

class GhostService : Service() {

    private class GhostHealth {
        var value = 0; set(value) { field = value.coerceIn(0, healthModifiers.size) }
        fun count() = ++value == healthModifiers.size

        fun apply(attribute: EntityAttributeInstance) {
            val modifier = healthModifiers.getOrNull(value) ?: healthModifier3
            var missing = true
            healthModifiers.forEach {
                if (attribute.hasModifier(it)) {
                    if (it == modifier) {
                        missing = false
                    } else {
                        attribute.removeModifier(it)
                    }
                }
            }
            if (missing) {
                attribute.addPersistentModifier(modifier)
            }
        }
    }

    private val healthValues = HashMap<GamePlayer, GhostHealth>()
    private fun healthValue(player: GamePlayer) = healthValues.getOrPut(player, ::GhostHealth)

    private fun updateGhosts() {
        val running = state.isPlaying

        for ((player, playerEntity) in playerEntitiesNormal) {

            val healthAttribute = playerEntity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!
            val healthValue = healthValue(player)

            if (player.isGhost) {

                val goingGhost = running && player.isAlive

                if (goingGhost) {
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

                healthValue.apply(healthAttribute)

            } else {

                if (playerEntity.frozenTicks >= playerEntity.minFreezeDamageTicks) {
                    playerEntity.damage(playerEntity.damageSources.freeze(), 1.0F)
                }

                healthModifiers.forEach {
                    if (healthAttribute.hasModifier(it)) {
                        healthAttribute.removeModifier(it)
                        playerEntity.health = playerEntity.maxHealth
                    }
                }

            }

        }
    }

    private var schedule: Scheduler.Schedule? = null

    override fun update() {
        if (schedule == null) {
            schedule = Scheduler.delay(1.0) { schedule = null }
            updateGhosts()
        }
        if (state.isPlaying) {
            for ((player, playerEntity) in playerEntitiesNormal) {
                if (player.isGhost) {
                    playerEntitiesIn.values.forEach {
                        if (it.squaredDistanceTo(playerEntity) <= 20) {
                            it.frozenTicks = Math.min(it.frozenTicks + 4, it.minFreezeDamageTicks + 20)
                        }
                    }
                }
            }
        }
    }

    fun handleDeath(
        player: GamePlayer,
        playerEntity: ServerPlayerEntity,
        attacker: GamePlayer,
        attackerEntity: ServerPlayerEntity,
    ) {
        if (attacker.team !== GameTeam.PLAYER_BLACK) return

        Scheduler.now {
            attackerEntity.damage(
                world.damageSources.create(Game.DAMAGE_TYPE_ABYSS, playerEntity, null),
                999999.0F,
            )
        }

        if (healthValue(player).count()) {
            Scheduler.now {
                playerEntity.damage(
                    world.damageSources.create(Game.DAMAGE_TYPE_ABYSS),
                    999999.0F,
                )
                player.team = GameTeam.NONE
            }
        }
    }

    fun handleInteract(player: GamePlayer, pos: BlockPos): ActionResult {
        val entity = player.entity
        if (entity != null) {
            context.itemService.addStackToInventory(entity, GameItems.ectoplasm, false)
        }
        world.setBlockState(pos, Blocks.MAGENTA_CONCRETE_POWDER.defaultState)
        return ActionResult.SUCCESS
    }

    fun shouldIgnoreDamage(key: RegistryKey<DamageType>?) = ignoredDamageTypes.contains(key)

    fun executeSetBlackDeaths(console: Console, targets: Collection<GamePlayer>, value: Int) {
        for (target in targets) healthValue(target).value = value
        console.sendInfo("Set ghost black death counter for ${targets.size} player(s)")
    }
    fun executeClearBlackDeaths(console: Console, targets: Collection<GamePlayer>) {
        for (target in targets) healthValue(target).value = 0
        console.sendInfo("Cleared ghost black death counter for ${targets.size} player(s)")
    }

    private companion object {

        private val healthModifier0 =
            EntityAttributeModifier(UUID.fromString("95880240-c1f7-4660-8e0e-a14f13e2cf41"), "msd_ghost_health", -12.0, EntityAttributeModifier.Operation.ADDITION)
        private val healthModifier1 =
            EntityAttributeModifier(UUID.fromString("44a5c49b-f2c6-4c66-8d41-76849b229510"), "msd_ghost_health", -14.0, EntityAttributeModifier.Operation.ADDITION)
        private val healthModifier2 =
            EntityAttributeModifier(UUID.fromString("81fa324f-e299-4988-b9de-8dcc777d3cdc"), "msd_ghost_health", -16.0, EntityAttributeModifier.Operation.ADDITION)
        private val healthModifier3 =
            EntityAttributeModifier(UUID.fromString("6eecfa8a-977e-43fa-92c4-c680f25bf42c"), "msd_ghost_health", -18.0, EntityAttributeModifier.Operation.ADDITION)

        private val healthModifiers = immutableListOf(healthModifier0, healthModifier1, healthModifier2, healthModifier3)

        private val ignoredDamageTypes = immutableSetOf(
            LIGHTNING_BOLT, LAVA, HOT_FLOOR, IN_WALL, CRAMMING, DROWN, STARVE,
            CACTUS, FALL, FLY_INTO_WALL, DRY_OUT, SWEET_BERRY_BUSH, FREEZE,
            STALAGMITE, FALLING_BLOCK, FALLING_ANVIL, FALLING_STALACTITE,
        )

    }

}
