package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.MobEntity

class DisguiseService : Service() {

    private val disguiseEntities = mutableMapOf<GamePlayer, LivingEntity>()

    override fun setup() {
    }

    override fun update() {
        for ((player, disguiseEntity) in disguiseEntities) {
            val playerEntity = player.entity
            if (playerEntity == null) {
                disguiseEntity.translation = Translation(world, properties.positionHell)
            } else {
                disguiseEntity.translation = playerEntity.translation
                if (disguiseEntity.velocity != playerEntity.velocity) {
                    disguiseEntity.velocity = playerEntity.velocity
                    disguiseEntity.velocityDirty = true
                }
                for (slot in EquipmentSlot.entries) {
                    val stackExpected = playerEntity.getEquippedStack(slot)
                    val stackActual = disguiseEntity.getEquippedStack(slot)
                    if (stackExpected contentNotEquals stackActual) {
                        disguiseEntity.equipStack(slot, stackExpected.copy())
                    }
                }
                for (effect in disguiseEntity.statusEffects) {
                    if (!playerEntity.hasEffect(effect.effectType)) {
                        disguiseEntity.removeEffect(effect.effectType)
                    }
                }
                for (playerEffect in playerEntity.statusEffects) {
                    if (!disguiseEntity.hasEffect(playerEffect.effectType)) {
                        disguiseEntity.addEffect(playerEffect.effectType, 30.0)
                    }
                }
                disguiseEntity.setHealth(playerEntity.health)
                disguiseEntity.setOnFire(playerEntity.isOnFire)
                disguiseEntity.setSprinting(playerEntity.isSprinting)
                disguiseEntity.setSneaking(playerEntity.isSneaking)
                disguiseEntity.setSwimming(playerEntity.isSwimming)
                disguiseEntity.setInvisible(playerEntity.isInvisible)
                disguiseEntity.setGlowing(playerEntity.isGlowing)
                if (playerEntity.isUsingItem) {
                    disguiseEntity.setCurrentHand(playerEntity.activeHand)
                    if (disguiseEntity is MobEntity) disguiseEntity.isAttacking = true
                } else {
                    disguiseEntity.clearActiveItem()
                    if (disguiseEntity is MobEntity) disguiseEntity.isAttacking = false
                }
                if (playerEntity.handSwinging && playerEntity.handSwingTicks < 0) {
                    disguiseEntity.swingHand(playerEntity.activeHand)
                }
                if (disguiseEntity.scoreboardTeam != playerEntity.scoreboardTeam) {
                    context.scoreboard.addScoreHolderToTeam(disguiseEntity.scoreboardName, playerEntity.scoreboardTeam)
                }
                playerEntity.networkHandler.sendPacket(InvisibilityService.createInvisiblePositionPacket(disguiseEntity.id))
            }
        }
    }

    fun activateDisguise(player: GamePlayer) = activateDisguise(player, EntityType.SKELETON)

    fun <T : Entity> activateDisguise(player: GamePlayer, type: EntityType<T>, block: (T) -> Unit = {}): T {
        val disguiseEntity = type.create(world)!!
        if (disguiseEntity !is LivingEntity) {
            throw IllegalArgumentException("Disguise entity is not a LivingEntity")
        }
        deactivateDisguise(player)
        logger.info("Disguising ${player.nameQuoted} as ${type.name.string}")
        if (disguiseEntity is MobEntity) {
            disguiseEntity.setAiDisabled(true)
            disguiseEntity.setAttacking(false)
        }
        disguiseEntity.setInvulnerable(true)
        disguiseEntity.setNoGravity(true)
        disguiseEntity.setSilent(true)
        disguiseEntity.setCustomName(player.name.asText())
        block(disguiseEntity)
        disguiseEntities[player] = disguiseEntity
        world.spawnEntity(disguiseEntity)
        return disguiseEntity
    }

    fun deactivateDisguise(player: GamePlayer): Boolean {
        val disguiseEntity = disguiseEntities.remove(player)
        if (disguiseEntity != null) {
            logger.info("Undisguising ${player.nameQuoted}")
            disguiseEntity.remove(Entity.RemovalReason.DISCARDED)
            return true
        }
        return false
    }

    fun isDisguised(player: GamePlayer) = disguiseEntities.containsKey(player)
    fun isDisguise(entity: Entity) = disguiseEntities.containsValue(entity)

    fun getDisguise(player: GamePlayer) = disguiseEntities[player]

    fun getDisguisedPlayer(entity: Entity): GamePlayer? {
        return disguiseEntities.entries.find { it.value == entity }?.key
    }

}
