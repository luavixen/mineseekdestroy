package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameContext
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.WaitingGameState
import dev.foxgirl.mineseekdestroy.util.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.inventory.Inventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Position
import java.time.Instant
import java.time.format.DateTimeFormatter

class SnapshotService : Service() {

    private class SnapshotPlayer {
        val player: GamePlayer

        val snapshotTeam: GameTeam
        val snapshotAlive: Boolean

        val snapshotKills: Int
        val snapshotDeaths: Int

        val snapshotPosition: Position?
        val snapshotInventory: Inventory?

        constructor(player: GamePlayer) {
            this.player = player

            snapshotTeam = player.team
            snapshotAlive = player.isAlive

            snapshotKills = player.kills
            snapshotDeaths = player.deaths

            val entity = player.entity
            if (entity != null) {
                snapshotPosition = entity.pos
                snapshotInventory = Inventories.copyOf(entity.inventory)
            } else {
                snapshotPosition = null
                snapshotInventory = null
            }
        }

        constructor(context: GameContext, nbt: NbtCompound) {
            player = context.getPlayer(nbt["Player"]!!.asCompound())

            snapshotTeam = GameTeam.valueOf(nbt["Team"]!!.toActualString())
            snapshotAlive = nbt["Alive"]!!.toBoolean()
            snapshotKills = nbt["Kills"]!!.toInt()
            snapshotDeaths = nbt["Deaths"]!!.toInt()
            snapshotPosition = nbt["Position"]!!.toBlockPos().toCenterPos()
            snapshotInventory = Inventories.fromNbt(nbt["Inventory"]!!.asCompound())
        }

        fun toNbt() = nbtCompoundOf(
            "Player" to player,
            "Team" to snapshotTeam.name,
            "Alive" to snapshotAlive,
            "Kills" to snapshotKills,
            "Deaths" to snapshotDeaths,
            "Position" to BlockPos.ofFloored(snapshotPosition),
            "Inventory" to snapshotInventory,
        )
    }

    private class Snapshot(val players: List<SnapshotPlayer>) {
        constructor(context: GameContext, nbt: NbtCompound)
            : this(nbt["Players"]!!.asList().map { SnapshotPlayer(context, it.asCompound()) })

        fun toNbt() = nbtCompoundOf("Players" to players)
    }

    private val snapshots = mutableListOf<Snapshot>()

    private var ready = false

    fun executeSnapshotRestore(console: Console) {
        if (ready) {
            ready = false
        } else {
            ready = true
            console.sendError("==== CONFIRMATION NEEDED - PLEASE REVIEW ====")
            console.sendError("Are you sure? Restoring could cause loss of progress!")
            console.sendError("Run `/msd snapshot restore` again to confirm")
            return
        }

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
            context.barrierService.executeBlimpClose(console)

            console.sendInfo("Restored snapshot of ${snapshot.players.size} players")
        }
    }

    fun executeSnapshotSave(console: Console) {
        ready = false

        val snapshot = Snapshot(players.map(::SnapshotPlayer))

        snapshots.add(snapshot)

        val name = "mnsnd-snapshot-${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}.bin"
        val path = FabricLoader.getInstance().getConfigDir().resolve(name)

        try {
            NbtIo.write(nbtCompoundOf("Snapshots" to snapshots), path.toFile())
            logger.info("Saved snapshot to file ${name}")
        } catch (cause : Exception) {
            logger.error("Failed to save snapshot to file ${name}", cause)
        }

        console.sendInfo("Created snapshot of ${snapshot.players.size} players")
    }

    fun executeSnapshotLoadBackup(console: Console, name: String) {
        val path = FabricLoader.getInstance().getConfigDir().resolve(name)

        try {
            val nbt = NbtIo.read(path.toFile())!!
            val snapshots = nbt["Snapshots"]!!.asList().map { Snapshot(context, it.asCompound()) }

            this.snapshots.clear()
            this.snapshots.addAll(snapshots)

            console.sendInfo("Successfully restored ${snapshots.size} entries from snapshot list backup")
        } catch (cause : Exception) {
            console.sendError("Failed to restore snapshot list backup")
            logger.error("Failed to restore snapshot list backup ${name}", cause)
        }
    }

}
