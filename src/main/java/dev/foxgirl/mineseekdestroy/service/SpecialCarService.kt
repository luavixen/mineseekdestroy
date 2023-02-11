package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import net.minecraft.entity.EntityType
import net.minecraft.entity.attribute.EntityAttributes

class SpecialCarService : Service() {

    private fun spawnCar() {
        val entity = EntityType.PIG.create(world)!!
        entity.isAiDisabled = true
        entity.health = 5e6F
        entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!.baseValue = 5e6
        entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)!!.baseValue = game.getRuleDouble(Game.RULE_CARS_SPEED)
        entity.saddle(null)
        world.spawnEntity(entity)
    }

}
