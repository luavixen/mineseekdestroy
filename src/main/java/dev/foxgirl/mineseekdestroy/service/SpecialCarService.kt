package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Reflector
import net.minecraft.entity.EntityType
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.passive.PigEntity
import net.minecraft.util.math.Vec3d

class SpecialCarService : Service() {

    private fun cars(): List<PigEntity> =
        world.getEntitiesByType(EntityType.PIG) { it.isAiDisabled && it.health > 100.0F }

    fun executeSpawnCar(console: Console, position: Vec3d) {
        val entity = EntityType.PIG.create(world)!!

        entity.setPosition(position)
        entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!.baseValue = 5e6
        entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)!!.baseValue = game.getRuleDouble(Game.RULE_CARS_SPEED)
        entity.isAiDisabled = true
        entity.health = 5e6F
        entity.saddle(null)

        world.spawnEntity(entity)

        context.scoreboard.addPlayerToTeam(entity.entityName, context.getTeam(GameTeam.OPERATOR))

        console.sendInfo("Spawned new car")
    }

    fun executeKillCars(console: Console) {
        cars().forEach { it.kill() }
        console.sendInfo("Killed all cars")
    }

    fun executeShowCars(console: Console) {
        cars().forEach { it.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, 20000000)) }
        console.sendInfo("Showing cars")
    }

    fun executeHideCars(console: Console) {
        cars().forEach { it.removeStatusEffect(StatusEffects.GLOWING) }
        console.sendInfo("Hiding cars")
    }

    fun cooldownIsReady(entity: PigEntity): Boolean {
        return Reflector.methodHandle(PigEntity::class.java, "mineseekdestroy\$cooldownIsReady")!!.invoke(entity) as Boolean
    }

    fun cooldownActivate(entity: PigEntity) {
        Reflector.methodHandle(PigEntity::class.java, "mineseekdestroy\$cooldownActivate")!!.invoke(entity)
    }

}
