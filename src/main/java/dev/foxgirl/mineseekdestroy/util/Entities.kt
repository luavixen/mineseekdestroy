package dev.foxgirl.mineseekdestroy.util

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageSources
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent

fun LivingEntity.hurtHearts(hearts: Double, source: (DamageSources) -> DamageSource = { it.generic() }): Boolean =
    damage(source(damageSources), (hearts * 2.0).toFloat())

fun LivingEntity.healHearts(hearts: Double): Boolean =
    health.also { heal((hearts * 2.0).toFloat()) } != health

fun LivingEntity.addEffect(type: StatusEffect, seconds: Double, strength: Int = 1): StatusEffectInstance {
    val duration = if (seconds < Int.MAX_VALUE.toDouble()) (seconds * 20.0).toInt() else StatusEffectInstance.INFINITE
    val amplifier = (strength - 1).coerceAtLeast(0)
    return StatusEffectInstance(type, duration, amplifier).also(::addStatusEffect)
}
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

fun Entity.play(
    sound: SoundEvent,
    category: SoundCategory = SoundCategory.PLAYERS,
    volume: Double = 1.0,
    pitch: Double = 1.0,
) {
    Broadcast.sendSound(sound, category, volume.toFloat(), pitch.toFloat(), world, pos)
}
