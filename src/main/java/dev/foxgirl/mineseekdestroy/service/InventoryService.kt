package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Inventories
import net.minecraft.block.entity.ChestBlockEntity

class InventoryService : Service() {

    fun executeClear(console: Console) = executeClear(console, players)
    fun executeClear(console: Console, targets: Collection<GamePlayer>) {
        var count = 0
        for (target in targets) {
            if (target.isPlaying) {
                val inventory = target.inventory
                if (inventory != null) {
                    Inventories.clear(inventory)
                    count++
                }
            }
        }
        console.sendInfo("Cleared inventories of ${count} player(s)")
    }

    fun executeFill(console: Console) = executeFill(console, players)
    fun executeFill(console: Console, targets: Collection<GamePlayer>) {
        val template = world.getBlockEntity(Game.TEMPLATE_INVENTORY)
        if (template !is ChestBlockEntity) {
            console.sendError("Failed to read template chest at ${Game.TEMPLATE_INVENTORY}")
            return
        }
        var count = 0
        for (target in targets) {
            if (target.isPlaying) {
                val inventory = target.inventory
                if (inventory != null) {
                    Inventories.copy(template, inventory)
                    count++
                }
            }
        }
        console.sendInfo("Updated inventories of ${count} players")
    }

    fun handleUpdate() {
        players.forEach(if (state is RunningGameState) ::mirrorUpdate else ::mirrorReset)
    }

    private companion object {

        private fun mirrorUpdate(player: GamePlayer) {
            val actual = player.inventory ?: return
            val mirror = player.inventoryMirror
            if (player.isSpectator) {
                if (mirror != null) {
                    if (!Inventories.equals(mirror, actual)) Inventories.copy(mirror, actual)
                } else {
                    player.inventoryMirror = Inventories.copyOf(actual)
                }
            } else {
                if (mirror != null) {
                    player.inventoryMirror = null
                }
            }
        }

        private fun mirrorReset(player: GamePlayer) {
            player.inventoryMirror = null
        }

    }

}
