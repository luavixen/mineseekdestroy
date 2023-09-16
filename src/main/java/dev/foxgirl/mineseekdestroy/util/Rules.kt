package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.Game
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule
import net.minecraft.world.GameRules
import net.minecraft.world.GameRules.BooleanRule
import net.minecraft.world.GameRules.IntRule
import kotlin.reflect.KProperty

object Rules {

    @JvmStatic var messageDirectAllowed by BooleanRuleProperty(Game.RULE_MESSAGE_DIRECT_ALLOWED)
    @JvmStatic var messageDirectBroadcast by BooleanRuleProperty(Game.RULE_MESSAGE_DIRECT_BROADCAST)
    @JvmStatic var messageTeamAllowed by BooleanRuleProperty(Game.RULE_MESSAGE_TEAM_ALLOWED)
    @JvmStatic var messageTeamBroadcast by BooleanRuleProperty(Game.RULE_MESSAGE_TEAM_BROADCAST)

    @JvmStatic var automationEnabled by BooleanRuleProperty(Game.RULE_AUTOMATION_ENABLED)
    @JvmStatic var automationGhostsEnabled by BooleanRuleProperty(Game.RULE_AUTOMATION_GHOSTS_ENABLED)
    @JvmStatic var automationDelayDuration by DoubleRuleProperty(Game.RULE_AUTOMATION_DELAY_DURATION)
    @JvmStatic var automationIntervalDuration by DoubleRuleProperty(Game.RULE_AUTOMATION_INTERVAL_DURATION)

    @JvmStatic var preparingDuration by DoubleRuleProperty(Game.RULE_PREPARING_DURATION)
    @JvmStatic var startingDuration by DoubleRuleProperty(Game.RULE_STARTING_DURATION)
    @JvmStatic var startingEffectDuration by DoubleRuleProperty(Game.RULE_STARTING_EFFECT_DURATION)

    @JvmStatic var pingVolume by DoubleRuleProperty(Game.RULE_PING_VOLUME)
    @JvmStatic var pingPitch by DoubleRuleProperty(Game.RULE_PING_PITCH)

    @JvmStatic var finalizingDuration by DoubleRuleProperty(Game.RULE_FINALIZING_DURATION)

    @JvmStatic var lootCount by IntRuleProperty(Game.RULE_LOOT_COUNT)

    @JvmStatic var killzoneBoundsEnabled by BooleanRuleProperty(Game.RULE_KILLZONE_BOUNDS_ENABLED)
    @JvmStatic var killzoneBlimpEnabled by BooleanRuleProperty(Game.RULE_KILLZONE_BLIMP_ENABLED)

    @JvmStatic var knockbackSnowball by DoubleRuleProperty(Game.RULE_KNOCKBACK_SNOWBALL)
    @JvmStatic var knockbackEgg by DoubleRuleProperty(Game.RULE_KNOCKBACK_EGG)

    @JvmStatic var borderCloseDuration by DoubleRuleProperty(Game.RULE_BORDER_CLOSE_DURATION)

    @JvmStatic var towerEffectDuration by DoubleRuleProperty(Game.RULE_TOWER_EFFECT_DURATION)
    @JvmStatic var towerKnockback by DoubleRuleProperty(Game.RULE_TOWER_KNOCKBACK)

    @JvmStatic var fansEffectDuration by DoubleRuleProperty(Game.RULE_FANS_EFFECT_DURATION)
    @JvmStatic var fansKnockback by DoubleRuleProperty(Game.RULE_FANS_KNOCKBACK)

    @JvmStatic var ghoulsEnabled by BooleanRuleProperty(Game.RULE_GHOULS_ENABLED)
    @JvmStatic var ghoulsSpawnDelayMin by DoubleRuleProperty(Game.RULE_GHOULS_SPAWN_DELAY_MIN)
    @JvmStatic var ghoulsSpawnDelayMax by DoubleRuleProperty(Game.RULE_GHOULS_SPAWN_DELAY_MAX)

    @JvmStatic var carsCooldownDuration by DoubleRuleProperty(Game.RULE_CARS_COOLDOWN_DURATION)
    @JvmStatic var carsBoostDuration by DoubleRuleProperty(Game.RULE_CARS_BOOST_DURATION)
    @JvmStatic var carsKnockback by DoubleRuleProperty(Game.RULE_CARS_KNOCKBACK)
    @JvmStatic var carsDamage by DoubleRuleProperty(Game.RULE_CARS_DAMAGE)
    @JvmStatic var carsSpeed by DoubleRuleProperty(Game.RULE_CARS_SPEED)

    @JvmStatic var summonsEnabled by BooleanRuleProperty(Game.RULE_SUMMONS_ENABLED)

    @JvmStatic var soulsDroppingEnabled by BooleanRuleProperty(Game.RULE_SOULS_DROPPING_ENABLED)
    @JvmStatic var soulsConsumingEnabled by BooleanRuleProperty(Game.RULE_SOULS_CONSUMING_ENABLED)
    @JvmStatic var soulsConsumingEffectDuration by DoubleRuleProperty(Game.RULE_SOULS_CONSUMING_EFFECT_DURATION)
    @JvmStatic var soulsConsumingEffectJumpStrength by IntRuleProperty(Game.RULE_SOULS_CONSUMING_EFFECT_JUMP_STRENGTH)
    @JvmStatic var soulsConsumingEffectSpeedStrength by IntRuleProperty(Game.RULE_SOULS_CONSUMING_EFFECT_SPEED_STRENGTH)

    @JvmStatic var buddyEnabled by BooleanRuleProperty(Game.RULE_BUDDY_ENABLED)
    @JvmStatic var buddyHealthPenalty by DoubleRuleProperty(Game.RULE_BUDDY_HEALTH_PENALTY)
    @JvmStatic var buddyAbsorptionStrength by IntRuleProperty(Game.RULE_BUDDY_ABSORPTION_STRENGTH)

    @JvmStatic var chaosEnabled by BooleanRuleProperty(Game.RULE_CHAOS_ENABLED)

    private sealed class RuleProperty<T : GameRules.Rule<T>>(protected val key: GameRules.Key<T>) {
        protected val game get() = Game.getGame()
    }
    private class DoubleRuleProperty(key: GameRules.Key<DoubleRule>) : RuleProperty<DoubleRule>(key) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = game.getRuleDouble(key)
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) = game.setRuleDouble(key, value)
    }
    private class IntRuleProperty(key: GameRules.Key<IntRule>) : RuleProperty<IntRule>(key) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = game.getRuleInt(key)
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = game.setRuleInt(key, value)
    }
    private class BooleanRuleProperty(key: GameRules.Key<BooleanRule>) : RuleProperty<BooleanRule>(key) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = game.getRuleBoolean(key)
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = game.setRuleBoolean(key, value)
    }

}
