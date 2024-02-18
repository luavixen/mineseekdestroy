package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import java.util.*

class TemporalGearService : Service() {

    companion object {
        @JvmStatic
        fun isTemporalGear(stack: ItemStack): Boolean =
            stack.hasNbt() && "MsdTemporal" in stack.nbt!!
        @JvmStatic
        fun isTemporalStand(entity: Entity): Boolean =
            entity is ArmorStandEntity && entity.getEquippedStack(EquipmentSlot.HEAD).let { it.hasNbt() && "MsdTemporalStand" in it.nbt!! }
    }

    private val trackedArmorStands = mutableSetOf<ArmorStandEntity>()

    private val ArmorStandEntity.linkedUUID: UUID?
        get() = getEquippedStack(EquipmentSlot.HEAD).nbt?.get("MsdTemporalStand")?.toUUID()
    private val ArmorStandEntity.linkedPlayer: GamePlayer?
        get() = linkedUUID?.let { context.getPlayer(it) }

    private fun getTrackedArmorStand(uuid: UUID) = trackedArmorStands.find { it.linkedUUID == uuid }

    private fun LivingEntity.poof() {
        play(SoundEvents.ENTITY_ARMOR_STAND_BREAK, SoundCategory.NEUTRAL)
        particles(ParticleTypes.POOF, 0.1, 5)
    }
    private fun LivingEntity.poofAndRemove() {
        poof()
        kill()
    }

    private fun ItemStack.copyIllegal(): ItemStack {
        if (isEmpty) return ItemStack.EMPTY
        return copy().apply { getOrCreateNbt()["MsdIllegal"] = true }
    }

    private fun updateTrackedArmorStand(standEntity: ArmorStandEntity): Boolean {
        if (standEntity.isRemoved) {
            return true
        }

        val player = standEntity.linkedPlayer
        if (player == null || !player.isAlive || !standEntity.isAlive) {
            standEntity.poofAndRemove()
            logger.info("Removing temporal stand for ${player?.nameQuoted ?: "unknown"} (player or stand dead)")
            return true
        }

        return false
    }

    override fun update() {
        trackedArmorStands.removeIf(::updateTrackedArmorStand)
    }

    fun handleArmorStandBroken(standEntity: ArmorStandEntity, source: DamageSource) {
        val player = standEntity.linkedPlayer ?: return

        val attackerEntity = arrayOf(source.source, source.attacker).find { it is ServerPlayerEntity } as? ServerPlayerEntity ?: return
        val attacker = context.getPlayer(attackerEntity)

        if (attacker != player) {
            val playerEntity = player.entity
            if (playerEntity != null && playerEntity.health > 1.0F) {
                val damage = Math.max(playerEntity.health - 1.0F, 0.1F)
                playerEntity.damage(playerEntity.damageSources.magic(), damage)
                playerEntity.particles(ParticleTypes.DAMAGE_INDICATOR, 1.0, (damage / 2.0F).toInt().coerceAtLeast(3))
            }
            attackerEntity.sendInfo(text("You destroyed ").append(player.displayName).append("'s temporal stand!"))
            player.entity?.sendInfo(text("Your temporal stand was destroyed by ").append(attacker.displayName).append("!"))
        } else {
            player.entity?.sendInfo("You destroyed your temporal stand!")
        }

        logger.info("Removing temporal stand for ${player.nameQuoted} (attacked by ${attacker.nameQuoted})")
        standEntity.poofAndRemove()
    }

    fun handleGearPlace(
        playerEntity: ServerPlayerEntity,
        hand: Hand, stack: ItemStack,
        hit: BlockHitResult,
    ): ActionResult {
        if (!isTemporalGear(stack)) return ActionResult.PASS

        val player = context.getPlayer(playerEntity)

        if (!state.isPlaying) {
            playerEntity.sendError("Cannot place temporal stand, game is not running")
            return ActionResult.PASS
        }
        if (!player.isAlive || !(player.isPlaying || player.isOperator)) {
            playerEntity.sendError("Cannot place temporal stand, not currently playing")
            return ActionResult.PASS
        }
        if (getTrackedArmorStand(player.uuid) != null) {
            playerEntity.sendError("You already have an active temporal stand!")
            return ActionResult.PASS
        }

        val standEntity = EntityType.ARMOR_STAND.create(world)!!.also {
            for (slot in EquipmentSlot.entries) {
                it.equipStack(slot, playerEntity.getEquippedStack(slot).copyIllegal())
            }
            it.equipStack(
                EquipmentSlot.HEAD,
                stackOf(Items.PLAYER_HEAD, nbtCompoundOf(
                    "SkullOwner" to player.name,
                    "MsdIllegal" to true,
                    "MsdTemporalStand" to player.uuid,
                )),
            )
            it.translation = playerEntity.translation.copy(pos = hit.pos)
            it.setShowArms(true)
            it.setHideBasePlate(false)
        }

        trackedArmorStands.add(standEntity)
        world.spawnEntityAndPassengers(standEntity)

        playerEntity.sendInfo("You placed your temporal stand!")
        logger.info("Player ${player.nameQuoted} has placed their temporal stand at ${standEntity.pos}")

        return ActionResult.SUCCESS
    }

    fun handleGearUse(
        playerEntity: ServerPlayerEntity,
        hand: Hand, stack: ItemStack,
    ): ActionResult {
        if (!isTemporalGear(stack)) return ActionResult.PASS

        val player = context.getPlayer(playerEntity)

        if (!state.isPlaying) {
            playerEntity.sendError("Cannot swap with your temporal stand, game is not running")
            return ActionResult.FAIL
        }
        if (!player.isAlive || !(player.isPlaying || player.isOperator)) {
            playerEntity.sendError("Cannot swap with your temporal stand, not currently playing")
            return ActionResult.FAIL
        }

        val standEntity = getTrackedArmorStand(player.uuid)
        if (standEntity == null) {
            playerEntity.sendError("You do not have an active temporal stand!")
            return ActionResult.FAIL
        }

        val trans1 = standEntity.translation
        val trans2 = playerEntity.translation

        trans1.applyTo(playerEntity)
        trans2.applyTo(standEntity)

        standEntity.poof()
        standEntity.play(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL)
        playerEntity.poof()
        playerEntity.play(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS)

        playerEntity.sendInfo("You swapped with your temporal stand!")
        logger.info("Player ${player.nameQuoted} swapped with their temporal stand ${trans1.pos} <-> ${trans2.pos}")

        return ActionResult.CONSUME
    }

}
