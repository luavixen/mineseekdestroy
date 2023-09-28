package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.text.Text

class SpecialBuddyService : Service() {

    private data class Buddy(val playerTarget: GamePlayer, val playerFollower: GamePlayer) {
        val displayName get(): Text =
            Text.empty()
                .append(playerFollower.displayName)
                .append(" → ")
                .append(playerTarget.displayName)
    }

    private val enabled get() = Rules.buddyEnabled
    private var warning = true

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
        if (!enabled && warning) {
            warning = false; Scheduler.delay(30.0) { warning = true }
            console.sendError("Buddy system is currently disabled, be warned")
        }
    }

    fun executeBuddyAdd(console: Console, playerTarget: GamePlayer, playerFollower: GamePlayer) =
        buddyUpdate(console, playerTarget, playerFollower, "Added new buddy pair", "Buddy pair already exists", buddies::add)

    fun executeBuddyRemove(console: Console, playerTarget: GamePlayer, playerFollower: GamePlayer) =
        buddyUpdate(console, playerTarget, playerFollower, "Removed buddy pair", "Buddy pair does not exist", buddies::remove)

    fun executeBuddyList(console: Console, title: String = "Buddy pairs:") {
        console.sendInfo(title)
        buddies.forEach { console.sendInfo("  -", it.displayName) }
    }

    fun handleDeath(player: GamePlayer) {
        if (!enabled) return
        for ((playerTarget, playerFollower) in buddies) {
            if (playerTarget == player) {
                val playerFollowerEntity = playerFollower.entity
                if (playerFollowerEntity != null) {
                    val damageSource = world.damageSources.create(Game.DAMAGE_TYPE_HEARTBREAK, playerTarget.entity, null)
                    val damageAmount = Rules.buddyHealthPenalty.toFloat()
                    Scheduler.now { playerFollowerEntity.damage(damageSource, damageAmount) }
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
                        val playerFollowerEntity = playerFollower.entity
                        if (playerFollowerEntity != null) {
                            playerFollowerEntity.addStatusEffect(StatusEffectInstance(
                                StatusEffects.ABSORPTION,
                                StatusEffectInstance.INFINITE,
                                Rules.buddyAbsorptionStrength - 1,
                            ))
                            playerFollowerEntity.sendMessage(text(playerTarget, "won last game, so you got absorption").lightPurple())
                            Game.CONSOLE_OPERATORS.sendInfo("Player", playerFollower, "given absorption for", text(playerTarget.displayName) + "'s", "win")
                        }
                    }
                }
            }
        }
        logger.info("Buddy system updated absorption statuses")
    }

}
