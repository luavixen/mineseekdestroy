package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.DuelingGameState
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import net.minecraft.block.Blocks
import net.minecraft.block.RespawnAnchorBlock
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.ActionResult

class DuelService : Service() {

    private data class Duel(
        val attacker: GamePlayer,
        val victim: GamePlayer,
    ) {
        val attackerDamage = attacker.givenDamage
        val victimDamage = victim.takenDamage

        val displayName: Text get() = text(attacker, "VS", victim)

        fun message(): List<Text> = listOf(
            text(
                "  -", text(attacker), "(${(attackerDamage / 2.0F).toInt()})",
                "VS", text(victim), "(${(victimDamage / 2.0F).toInt()})",
            ),
            text(
                "  ↳",
                text("[START]").green().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel start uuid ${attacker.name} ${victim.name}")) },
                text("[CANCEL]").red().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel cancel uuid ${attacker.name} ${victim.name}")) },
            ),
        )
    }

    private var duels = mutableSetOf<Duel>()
    private var duelsEnabled = false

    fun handleAnchorOpen(player: GamePlayer): ActionResult {
        if (duelsEnabled && !(player.isSpectator || player.isGhost)) {
            player.entity?.openHandledScreen(DuelScreenHandlerFactory())
            return ActionResult.SUCCESS
        }
        return ActionResult.FAIL
    }

    private fun updateAnchors(enabled: Boolean) {
        val queue = Editor.queue(world, properties.regionBlimp)
        if (enabled) {
            queue.edit { state, _, _, _ ->
                if (state.block === Blocks.RESPAWN_ANCHOR) state.with(RespawnAnchorBlock.CHARGES, RespawnAnchorBlock.MAX_CHARGES) else null
            }
        } else {
            queue.edit { state, _, _, _ ->
                if (state.block === Blocks.RESPAWN_ANCHOR) state.with(RespawnAnchorBlock.CHARGES, RespawnAnchorBlock.NO_CHARGES) else null
            }
        }
    }

    fun executeDuelOpen(console: Console) {
        if (duelsEnabled) {
            console.sendError("Duel submission already open")
            return
        }
        duelsEnabled = true
        updateAnchors(true)
        console.sendInfo("Duel submission opened")
    }
    fun executeDuelClose(console: Console) {
        if (!duelsEnabled) {
            console.sendError("Duel submission already closed")
            return
        }
        duelsEnabled = false
        updateAnchors(false)
        console.sendInfo("Duel submission closed")
    }

    fun executeDuelStart(console: Console, attacker: GamePlayer, victim: GamePlayer, force: Boolean) {
        val duel = Duel(attacker, victim)

        if (!duels.remove(duel) && !force) {
            console.sendError("Duel", duel, "is not available, cannot start")
            return
        }

        context.snapshotService.executeSnapshotSave(console)

        context.damageService.addRecord(attacker, 0.0F - (attacker.givenDamage - victim.givenDamage).coerceAtLeast(0.0F))
        context.automationService.handleDuelPrepare()

        attacker.team = GameTeam.DUELIST
        attacker.isAlive = true
        victim.team = GameTeam.DUELIST
        victim.isAlive = true

        Async.background().withTimeout(0.1).go {
            while (true) {
                attacker.teleport(properties.positionDuel1)
                victim.teleport(properties.positionDuel2)
                delay()
            }
        }

        state = DuelingGameState()

        Game.CONSOLE_PLAYERS.sendInfo("Duel", duel, "has started, FIGHT!")
    }
    fun executeDuelCancel(console: Console, aggressor: GamePlayer, victim: GamePlayer) {
        val duel = Duel(aggressor, victim)

        if (!duels.remove(duel)) {
            console.sendError("Duel", duel, "is not available, cannot cancel")
            return
        }

        console.sendInfo("Duel", duel, "cancelled")
    }

    fun executeDuelList(console: Console) {
        console.sendInfo("Duels available:")
        for (duel in duels) {
            duel.message().forEach(console::sendInfo)
        }
    }

    private inner class DuelScreenHandlerFactory : DynamicScreenHandlerFactory<DuelScreenHandler>() {
        override val name get() = text("baby fight")
        override fun construct(sync: Int, playerInventory: PlayerInventory) = DuelScreenHandler(sync, playerInventory)
    }

    private inner class DuelScreenHandler(sync: Int, playerInventory: PlayerInventory)
        : DynamicScreenHandler(ScreenHandlerType.GENERIC_9X6, sync, playerInventory)
    {
    }

}
