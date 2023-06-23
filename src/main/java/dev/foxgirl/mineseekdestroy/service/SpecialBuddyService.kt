package dev.foxgirl.mineseekdestroy.service

import com.mojang.serialization.Lifecycle
import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Console
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.SimpleRegistry
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class SpecialBuddyService : Service() {

    val damageTypeKey: RegistryKey<DamageType> =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier("mineseekdestroy", "heartbreak"))

    private data class Buddy(val playerTarget: GamePlayer, val playerFollower: GamePlayer) {
        val displayName get(): Text =
            Text.empty()
                .append(playerFollower.displayName)
                .append(" → ")
                .append(playerTarget.displayName)
    }

    private val enabled get() = Game.getGame().getRuleBoolean(Game.RULE_BUDDY_ENABLED)

    private val buddies = mutableSetOf<Buddy>()

    private fun buddyUpdate(
        console: Console,
        playerTarget: GamePlayer, playerFollower: GamePlayer,
        messageSuccess: String, messageFailure: String,
        action: (Buddy) -> Boolean,
    ) {
        val buddy = Buddy(playerTarget, playerFollower)
        if (action(buddy)) {
            console.sendInfo(messageSuccess, buddy.displayName)
        } else {
            console.sendError(messageFailure, buddy.displayName)
        }
        if (!enabled) {
            console.sendError("Buddy system is currently disabled, be warned")
        }
    }

    fun executeBuddyAdd(console: Console, playerTarget: GamePlayer, playerFollower: GamePlayer) =
        buddyUpdate(console, playerTarget, playerFollower, "Added new buddy pair", "Buddy pair already exists", buddies::add)

    fun executeBuddyRemove(console: Console, playerTarget: GamePlayer, playerFollower: GamePlayer) =
        buddyUpdate(console, playerTarget, playerFollower, "Removed buddy pair", "Buddy pair does not exist", buddies::remove)

    fun executeBuddyList(console: Console) {
        console.sendInfo("Buddy pairs:")
        buddies.forEach { console.sendInfo("  -", it.displayName) }
    }

    fun handleDeath(player: GamePlayer) {
        if (!enabled) return
        for ((playerTarget, playerFollower) in buddies) {
            if (playerTarget == player) {
                val playerFollowerEntity = playerFollower.entity
                if (playerFollowerEntity != null) {
                    playerFollowerEntity.damage(
                        world.damageSources.create(damageTypeKey, playerTarget.entity, null),
                        game.getRuleDouble(Game.RULE_BUDDY_HEALTH_PENALTY).toFloat(),
                    )
                }
            }
        }
    }

    fun handleAutomationRoundEndCompleted() {
        if (!enabled) return
        for ((_, playerEntity) in playerEntitiesNormal) {
            playerEntity.removeStatusEffect(StatusEffects.ABSORPTION)
        }
        for (player in playersNormal) {
            if (
                player.team === GameTeam.PLAYER_YELLOW ||
                player.team === GameTeam.PLAYER_BLUE ||
                player.team === GameTeam.SKIP
            ) {
                for ((playerTarget, playerFollower) in buddies) {
                    if (playerTarget == player) {
                        playerFollower.entity?.addStatusEffect(StatusEffectInstance(
                            StatusEffects.ABSORPTION,
                            StatusEffectInstance.INFINITE,
                            game.getRuleInt(Game.RULE_BUDDY_ABSORPTION_STRENGTH) - 1,
                        ))
                    }
                }
            }
        }
        logger.info("Buddy system updated absorption statuses")
    }

    override fun setup() {
        // This is such a painful hack, but I don't care!
        val registry = world.registryManager.get(RegistryKeys.DAMAGE_TYPE) as SimpleRegistry<DamageType>
        if (registry.getEntry(damageTypeKey).isEmpty) {
            registry.frozen = false
            registry.add(damageTypeKey, DamageType("heartbreak", 0.0F), Lifecycle.experimental())
            registry.freeze()
        }
    }

}