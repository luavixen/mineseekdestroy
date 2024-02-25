package dev.foxgirl.mineseekdestroy.util

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageSources
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.PositionFlag
import net.minecraft.particle.ParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.min

fun LivingEntity.hurtHearts(hearts: Double, source: (DamageSources) -> DamageSource = { it.generic() }): Boolean =
    damage(source(damageSources), (hearts * 2.0).toFloat())

fun LivingEntity.healHearts(hearts: Double): Boolean =
    health.also { heal((hearts * 2.0).toFloat()) } != health

fun LivingEntity.addEffect(type: StatusEffect, seconds: Double, strength: Int = 1): StatusEffectInstance {
    val duration = if (seconds < Int.MAX_VALUE.toDouble()) (seconds * 20.0).toInt() else StatusEffectInstance.INFINITE
    val amplifier = (strength - 1).coerceAtLeast(0)
    return StatusEffectInstance(type, duration, amplifier).also(::addStatusEffect)
}
fun LivingEntity.getEffect(type: StatusEffect): StatusEffectInstance? =
    getStatusEffect(type)
fun LivingEntity.hasEffect(type: StatusEffect): Boolean =
    hasStatusEffect(type)
fun LivingEntity.removeEffect(type: StatusEffect): Boolean =
    removeStatusEffect(type)

fun PlayerEntity.give(stack: ItemStack) = this.give(stack, true)
fun PlayerEntity.give(stack: ItemStack, drop: Boolean): Boolean {
    if (stack.isEmpty) return true
    if (giveItemStack(stack)) return true
    if (drop) {
        val entity = dropItem(stack, false)
        if (entity != null) {
            entity.resetPickupDelay()
            entity.setOwner(uuid)
            return true
        }
    }
    return false
}

fun ServerPlayerEntity.sendInfo(vararg values: Any?) {
    sendMessage(Console.formatInfo(*values))
}
fun ServerPlayerEntity.sendError(vararg values: Any?) {
    sendMessage(Console.formatError(*values))
}

fun Entity.applyVelocity(velocity: Vec3d) {
    addVelocity(velocity)
    velocityDirty = true
    if (this is ServerPlayerEntity) {
        velocityModified = true
    }
}
fun Entity.applyKnockback(strength: Double, x: Double, z: Double) = applyKnockback(strength, x, z, false)
fun Entity.applyKnockback(strength: Double, x: Double, z: Double, force: Boolean) {
    if (this is LivingEntity && !force) {
        takeKnockback(strength, x, z)
    } else {
        applyKnockbackImpl(this, strength, x, z)
    }
    if (velocityDirty && this is ServerPlayerEntity) {
        velocityModified = true
    }
}
private fun applyKnockbackImpl(entity: Entity, strength: Double, x: Double, z: Double) {
    entity.velocityDirty = true
    entity.velocity = entity.velocity.let {
        val delta = Vec3d(x, 0.0, z).normalize().multiply(strength)
        val x = it.x / 2.0 - delta.x
        val z = it.z / 2.0 - delta.z
        val y = if (entity.isOnGround) min(0.4, it.y / 2.0 + strength) else it.y
        Vec3d(x, y, z)
    }
}

fun Entity.play(
    sound: SoundEvent,
    category: SoundCategory = SoundCategory.PLAYERS,
    volume: Double = 1.0,
    pitch: Double = 1.0,
) {
    Broadcast.sendSound(sound, category, volume.toFloat(), pitch.toFloat(), world, pos)
}
fun Entity.particles(
    particle: ParticleEffect,
    speed: Double = 1.0,
    count: Int = 1,
    position: (Vec3d) -> Vec3d = { it.add(0.0, height / 2.0, 0.0) },
) {
    Broadcast.sendParticles(particle, speed.toFloat(), count, world, position(pos))
}

val Entity.scoreboardName: String get() = nameForScoreboard

data class Translation(val world: World, val pos: Vec3d, val yaw: Float, val pitch: Float, val bodyYaw: Float, val headYaw: Float) {
    constructor(world: World, pos: Vec3d, yaw: Float, pitch: Float) : this(world, pos, yaw, pitch, yaw, yaw)
    constructor(world: World, pos: Vec3d) : this(world, pos, 0.0F, 0.0F)

    constructor(entity: Entity) : this(
        entity.world,
        entity.pos,
        entity.yaw,
        entity.pitch,
        entity.bodyYaw,
        entity.headYaw,
    )

    fun applyTo(entity: Entity) {
        if (entity is ServerPlayerEntity) {
            entity.teleport(world as ServerWorld, pos.x, pos.y, pos.z, yaw, pitch)
        } else {
            entity.teleport(world as ServerWorld, pos.x, pos.y, pos.z, PositionFlag.VALUES, yaw, pitch)
        }
        entity.bodyYaw = bodyYaw
        entity.headYaw = headYaw
    }
}

var Entity.translation: Translation
    get() = Translation(this)
    set(value) = value.applyTo(this)
