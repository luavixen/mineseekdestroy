package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Scheduler
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class AutomationService : Service() {

    private class Record(player: GamePlayer) {
        val kills = player.kills
        val deaths = player.deaths
    }

    private var records = mapOf<GamePlayer, Record>()

    fun handleRoundBegin() {
        records = players.associateWith(::Record)
    }

    fun handleRoundEnd(teamLosers: GameTeam) {
        if (!game.getRuleBoolean(Game.RULE_AUTOMATION_ENABLED)) return

        val tasks = mutableListOf<() -> Unit>()

        val players = players

        for (player in players) {
            if (player.team == teamLosers) {
                tasks.add { player.team = GameTeam.PLAYER_BLACK }
            }
        }

        for (player in players) {
            val record = records[player] ?: continue
            if (player.team == GameTeam.PLAYER_BLACK && player.kills <= record.kills) {
                tasks.add {
                    player.team = GameTeam.NONE
                    game.sendInfo(
                        Text.literal(player.name).formatted(Formatting.DARK_RED),
                        Text.literal("has been removed from the game!").formatted(Formatting.RED),
                    )
                }
            }
        }

        val iterator = tasks.iterator()

        val secondsDelay = game.getRuleDouble(Game.RULE_AUTOMATION_DELAY_DURATION)
        val secondsInterval = game.getRuleDouble(Game.RULE_AUTOMATION_INTERVAL_DURATION)

        Scheduler.delay(secondsDelay) {
            players.forEach { it.isAlive = true }

            Scheduler.interval(secondsInterval) { schedule ->
                if (iterator.hasNext()) {
                    iterator.next()()
                } else {
                    schedule.cancel()
                }
            }
        }
    }

}
