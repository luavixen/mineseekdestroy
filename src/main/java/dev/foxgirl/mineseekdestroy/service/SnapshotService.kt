package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.state.WaitingGameState
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.util.math.Position

class SnapshotService : Service() {

    private class SnapshotPlayer(val player: GamePlayer) {
        val snapshotTeam = player.team
        val snapshotAlive = player.isAlive

        val snapshotKills = player.kills
        val snapshotDeaths = player.deaths

        val snapshotPosition: Position?
        val snapshotInventory: Inventory?

        init {
            val entity = player.entity
            if (entity != null) {
                snapshotPosition = entity.pos
                snapshotInventory = Inventories.copyOf(entity.inventory)
            } else {
                snapshotPosition = null
                snapshotInventory = null
            }
        }
    }

    private class Snapshot(val players: List<SnapshotPlayer>)

    private val snapshots = mutableListOf<Snapshot>()

    fun executeSnapshotSave(console: Console) {
        Snapshot(players.map(::SnapshotPlayer)).let { snapshot ->
            snapshots.add(snapshot)
            console.sendInfo("Created snapshot of ${snapshot.players.size} players")
        }
    }

    fun executeSnapshotRestore(console: Console) {
        val snapshot = snapshots.removeLastOrNull()
        if (snapshot == null) {
            console.sendError("Snapshot stack is empty, cannot restore")
        } else {
            snapshot.players.forEach {
                it.player.team = it.snapshotTeam
                it.player.isAlive = it.snapshotAlive
                it.player.kills = it.snapshotKills
                it.player.deaths = it.snapshotDeaths
                if (it.snapshotPosition != null) {
                    it.player.teleport(it.snapshotPosition)
                }
                if (it.snapshotInventory != null) {
                    val source = it.snapshotInventory
                    val target = it.player.inventory
                    if (target != null) {
                        Inventories.copy(source, target)
                    }
                }
            }
            state = WaitingGameState()
            console.sendInfo("Restored snapshot of ${snapshot.players.size} players")
        }
    }

}
