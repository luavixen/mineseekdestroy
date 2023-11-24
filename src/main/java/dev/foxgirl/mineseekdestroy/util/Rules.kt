package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.Game.*
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule
import net.minecraft.world.GameRules
import net.minecraft.world.GameRules.BooleanRule
import net.minecraft.world.GameRules.IntRule
import kotlin.reflect.KProperty

object Rules {

    @JvmStatic var messageDirectAllowed by BooleanRuleProperty(RULE_MESSAGE_DIRECT_ALLOWED)
    @JvmStatic var messageDirectBroadcast by BooleanRuleProperty(RULE_MESSAGE_DIRECT_BROADCAST)
    @JvmStatic var messageTeamAllowed by BooleanRuleProperty(RULE_MESSAGE_TEAM_ALLOWED)
    @JvmStatic var messageTeamBroadcast by BooleanRuleProperty(RULE_MESSAGE_TEAM_BROADCAST)

    @JvmStatic var automationEnabled by BooleanRuleProperty(RULE_AUTOMATION_ENABLED)
    @JvmStatic var automationGhostsEnabled by BooleanRuleProperty(RULE_AUTOMATION_GHOSTS_ENABLED)
    @JvmStatic var automationDelayDuration by DoubleRuleProperty(RULE_AUTOMATION_DELAY_DURATION)
    @JvmStatic var automationIntervalDuration by DoubleRuleProperty(RULE_AUTOMATION_INTERVAL_DURATION)

    @JvmStatic var preparingDuration by DoubleRuleProperty(RULE_PREPARING_DURATION)
    @JvmStatic var startingDuration by DoubleRuleProperty(RULE_STARTING_DURATION)
    @JvmStatic var startingEffectDuration by DoubleRuleProperty(RULE_STARTING_EFFECT_DURATION)

    @JvmStatic var pingVolume by DoubleRuleProperty(RULE_PING_VOLUME)
    @JvmStatic var pingPitch by DoubleRuleProperty(RULE_PING_PITCH)

    @JvmStatic var finalizingDuration by DoubleRuleProperty(RULE_FINALIZING_DURATION)

    @JvmStatic var hiddenArmorEnabled by BooleanRuleProperty(RULE_HIDDEN_ARMOR_ENABLED)

    @JvmStatic var enhancedFurnacesEnabled by BooleanRuleProperty(RULE_ENHANCED_FURNACES_ENABLED)

    @JvmStatic var blueMeleeCrits by BooleanRuleProperty(RULE_BLUE_MELEE_CRITS)
    @JvmStatic var blueArrowCrits by BooleanRuleProperty(RULE_BLUE_ARROW_CRITS)

    @JvmStatic var damageFlashEnabled by BooleanRuleProperty(RULE_DAMAGE_FLASH_ENABLED)

    @JvmStatic var ghostsBlackDeathPenaltyAmount by IntRuleProperty(RULE_GHOSTS_BLACK_DEATH_PENALTY_AMOUNT)

    @JvmStatic var lootCount by IntRuleProperty(RULE_LOOT_COUNT)

    @JvmStatic var killzoneBoundsEnabled by BooleanRuleProperty(RULE_KILLZONE_BOUNDS_ENABLED)
    @JvmStatic var killzoneBlimpEnabled by BooleanRuleProperty(RULE_KILLZONE_BLIMP_ENABLED)

    @JvmStatic var knockbackSnowball by DoubleRuleProperty(RULE_KNOCKBACK_SNOWBALL)
    @JvmStatic var knockbackEgg by DoubleRuleProperty(RULE_KNOCKBACK_EGG)

    @JvmStatic var borderCloseDuration by DoubleRuleProperty(RULE_BORDER_CLOSE_DURATION)

    @JvmStatic var towerEffectDuration by DoubleRuleProperty(RULE_TOWER_EFFECT_DURATION)
    @JvmStatic var towerKnockback by DoubleRuleProperty(RULE_TOWER_KNOCKBACK)

    @JvmStatic var fansEffectDuration by DoubleRuleProperty(RULE_FANS_EFFECT_DURATION)
    @JvmStatic var fansKnockback by DoubleRuleProperty(RULE_FANS_KNOCKBACK)

    @JvmStatic var ghoulsEnabled by BooleanRuleProperty(RULE_GHOULS_ENABLED)
    @JvmStatic var ghoulsSpawnDelayMin by DoubleRuleProperty(RULE_GHOULS_SPAWN_DELAY_MIN)
    @JvmStatic var ghoulsSpawnDelayMax by DoubleRuleProperty(RULE_GHOULS_SPAWN_DELAY_MAX)

    @JvmStatic var carsCooldownDuration by DoubleRuleProperty(RULE_CARS_COOLDOWN_DURATION)
    @JvmStatic var carsBoostDuration by DoubleRuleProperty(RULE_CARS_BOOST_DURATION)
    @JvmStatic var carsKnockback by DoubleRuleProperty(RULE_CARS_KNOCKBACK)
    @JvmStatic var carsDamage by DoubleRuleProperty(RULE_CARS_DAMAGE)
    @JvmStatic var carsSpeed by DoubleRuleProperty(RULE_CARS_SPEED)

    @JvmStatic var summonsEnabled by BooleanRuleProperty(RULE_SUMMONS_ENABLED)
    @JvmStatic var summonsAltarGlowDuration by DoubleRuleProperty(RULE_SUMMONS_ALTAR_GLOW_DURATION)
    @JvmStatic var summonsShowTooltipEnabled by BooleanRuleProperty(RULE_SUMMONS_SHOW_TOOLTIP_ENABLED)
    @JvmStatic var summonsDeepdeepEnabled by BooleanRuleProperty(RULE_SUMMONS_DEEPDEEP_ENABLED)

    @JvmStatic var soulsDroppingEnabled by BooleanRuleProperty(RULE_SOULS_DROPPING_ENABLED)
    @JvmStatic var soulsConsumingEnabled by BooleanRuleProperty(RULE_SOULS_CONSUMING_ENABLED)
    @JvmStatic var soulsConsumingEffectDuration by DoubleRuleProperty(RULE_SOULS_CONSUMING_EFFECT_DURATION)
    @JvmStatic var soulsConsumingEffectJumpStrength by IntRuleProperty(RULE_SOULS_CONSUMING_EFFECT_JUMP_STRENGTH)
    @JvmStatic var soulsConsumingEffectSpeedStrength by IntRuleProperty(RULE_SOULS_CONSUMING_EFFECT_SPEED_STRENGTH)
    @JvmStatic var soulsGiveYellowOwnSoulEnabled by BooleanRuleProperty(RULE_SOULS_GIVE_YELLOW_OWN_SOUL_ENABLED)
    @JvmStatic var soulsGiveBlueOwnSoulEnabled by BooleanRuleProperty(RULE_SOULS_GIVE_BLUE_OWN_SOUL_ENABLED)
    @JvmStatic var soulsGiveHerobrinesSoulEnabled by BooleanRuleProperty(RULE_SOULS_GIVE_HEROBRINES_SOUL_ENABLED)

    @JvmStatic var buddyEnabled by BooleanRuleProperty(RULE_BUDDY_ENABLED)
    @JvmStatic var buddyHealthPenalty by DoubleRuleProperty(RULE_BUDDY_HEALTH_PENALTY)
    @JvmStatic var buddyAbsorptionStrength by IntRuleProperty(RULE_BUDDY_ABSORPTION_STRENGTH)

    @JvmStatic var chaosEnabled by BooleanRuleProperty(RULE_CHAOS_ENABLED)

    @JvmStatic var countdownEnabled by BooleanRuleProperty(RULE_COUNTDOWN_ENABLED)
    @JvmStatic var countdownAutostartEnabled by BooleanRuleProperty(RULE_COUNTDOWN_AUTOSTART_ENABLED)
    @JvmStatic var countdownProgressionEnabled by BooleanRuleProperty(RULE_COUNTDOWN_PROGRESSION_ENABLED)
    @JvmStatic var countdownDamageAmount by DoubleRuleProperty(RULE_COUNTDOWN_DAMAGE_AMOUNT)
    @JvmStatic var countdownTextFadeinDuration by IntRuleProperty(RULE_COUNTDOWN_TEXT_FADEIN_DURATION)
    @JvmStatic var countdownTextStayDuration by IntRuleProperty(RULE_COUNTDOWN_TEXT_STAY_DURATION)
    @JvmStatic var countdownTextFadeoutDuration by IntRuleProperty(RULE_COUNTDOWN_TEXT_FADEOUT_DURATION)

    private sealed class RuleProperty<T : GameRules.Rule<T>>(protected val key: GameRules.Key<T>) {
        protected val game get() = getGame()
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
