package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Rules
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.attribute.EntityAttributeInstance
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
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

        fun incrementAndCheck(amount: Int = 1): Boolean{
            value += amount
            return value == healthModifiers.size
        }

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
                attribute.addTemporaryModifier(modifier)
            }
        }
    }

    private val healthValues = HashMap<GamePlayer, GhostHealth>(32)
    private fun healthValue(player: GamePlayer) = healthValues.computeIfAbsent(player) { GhostHealth() }

    private val ghostsFreezingPlayers = HashMap<GamePlayer, MutableSet<GamePlayer>>(32)
    private fun ghostsFreezingPlayer(player: GamePlayer) = ghostsFreezingPlayers.computeIfAbsent(player, ::mutableSetOf)

    private fun markGhostFreezingPlayer(ghostPlayer: GamePlayer, targetPlayer: GamePlayer) {
        ghostsFreezingPlayer(targetPlayer).add(ghostPlayer)
    }

    private fun freezingDamageSourceFor(player: GamePlayer, playerEntity: ServerPlayerEntity): DamageSource {
        val attackerEntity = ghostsFreezingPlayer(player)
            .mapNotNull { it.entity }
            .minByOrNull { it.squaredDistanceTo(playerEntity) }
        val damageType = playerEntity.damageSources.freeze().typeRegistryEntry.key.get()
        val damageSource = playerEntity.damageSources.create(damageType, attackerEntity)
        return damageSource
    }

    private val ghostsToPromote = mutableSetOf<GamePlayer>()

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

                playerEntities.forEach { (_, targetEntity) ->
                    val team = if (goingGhost) targetEntity.scoreboardTeam as? Team else player.scoreboardTeam
                    if (team != null) {
                        targetEntity.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(team, player.name, TeamS2CPacket.Operation.ADD))
                    }
                }

                healthValue.apply(healthAttribute)

                if (goingGhost && playerEntity.health + playerEntity.absorptionAmount >= 19.5F) {
                    if (ghostsToPromote.add(player)) {
                        consolePlayers.sendInfo(player, "escaped the afterlife!")
                    }
                }

            } else {

                if (playerEntity.frozenTicks >= playerEntity.minFreezeDamageTicks) {
                    playerEntity.damage(freezingDamageSourceFor(player, playerEntity), 1.0F)
                }

                healthModifiers.forEach {
                    if (healthAttribute.hasModifier(it)) {
                        healthAttribute.removeModifier(it)
                        playerEntity.health = playerEntity.maxHealth
                    }
                }

            }

        }

        ghostsFreezingPlayers.clear()
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
                    playerEntitiesIn.forEach { (target, targetEntity) ->
                        if (targetEntity.squaredDistanceTo(playerEntity) <= 20) {
                            targetEntity.frozenTicks = Math.min(
                                targetEntity.frozenTicks + Rules.ghostsFreezingTickAmount,
                                targetEntity.minFreezeDamageTicks + 20,
                            )
                            markGhostFreezingPlayer(player, target)
                        }
                    }
                }
            }
        }
        if (state.isWaiting && ghostsToPromote.isNotEmpty()) {
            ghostsToPromote.forEach { it.team = GameTeam.BLACK }
            ghostsToPromote.clear()
        }
    }

    fun handleGhostDeath(
        player: GamePlayer,
        playerEntity: ServerPlayerEntity,
        attacker: GamePlayer,
        attackerEntity: ServerPlayerEntity,
    ) {
        if (attacker.team !== GameTeam.BLACK) return

        Scheduler.now {
            attackerEntity.damage(
                world.damageSources.create(Game.DAMAGE_TYPE_ABYSS, playerEntity, null),
                999999.0F,
            )
        }

        if (healthValue(player).incrementAndCheck(Rules.ghostsBlackDeathPenaltyAmount)) {
            Scheduler.now {
                playerEntity.damage(
                    world.damageSources.create(Game.DAMAGE_TYPE_ABYSS),
                    999999.0F,
                )
                player.team = GameTeam.NONE
            }
        }
    }

    fun handleGhostInteract(player: GamePlayer, blockPos: BlockPos, blockState: BlockState): ActionResult {
        if (properties.unstealableBlocks.contains(blockState.block)) return ActionResult.PASS

        val entity = player.entity
        if (entity != null) {
            context.itemService.addStackToInventory(entity, GameItems.ectoplasm, false)
        }

        world.setBlockState(blockPos, Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultState)

        return ActionResult.SUCCESS
    }

    fun shouldGhostIgnoreDamage(key: RegistryKey<DamageType>?) = ignoredDamageTypes.contains(key)

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
            EntityAttributeModifier(UUID.fromString("95880240-c1f7-4660-8e0e-a14f13e2cf41"), "msd_ghost_health_0", -12.0, EntityAttributeModifier.Operation.ADDITION)
        private val healthModifier1 =
            EntityAttributeModifier(UUID.fromString("44a5c49b-f2c6-4c66-8d41-76849b229510"), "msd_ghost_health_1", -14.0, EntityAttributeModifier.Operation.ADDITION)
        private val healthModifier2 =
            EntityAttributeModifier(UUID.fromString("81fa324f-e299-4988-b9de-8dcc777d3cdc"), "msd_ghost_health_2", -16.0, EntityAttributeModifier.Operation.ADDITION)
        private val healthModifier3 =
            EntityAttributeModifier(UUID.fromString("6eecfa8a-977e-43fa-92c4-c680f25bf42c"), "msd_ghost_health_3", -18.0, EntityAttributeModifier.Operation.ADDITION)

        private val healthModifiers = immutableListOf(healthModifier0, healthModifier1, healthModifier2, healthModifier3)

        private val ignoredDamageTypes = immutableSetOf(
            LIGHTNING_BOLT, LAVA, HOT_FLOOR, IN_WALL, CRAMMING, DROWN, STARVE,
            CACTUS, FALL, FLY_INTO_WALL, DRY_OUT, SWEET_BERRY_BUSH, FREEZE,
            STALAGMITE, FALLING_BLOCK, FALLING_ANVIL, FALLING_STALACTITE,
        )

    }

}
