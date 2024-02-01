package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.*
import dev.foxgirl.mineseekdestroy.state.FrozenGameState
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.inventory.Inventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.io.path.listDirectoryEntries

class SnapshotService : Service() {

    private class SnapshotPlayer {
        val player: GamePlayer

        val snapshotTeam: GameTeam
        val snapshotAlive: Boolean

        val snapshotKills: Int
        val snapshotDeaths: Int

        val snapshotHealth: Float?
        val snapshotPosition: Vec3d?
        val snapshotInventory: Inventory?

        constructor(player: GamePlayer) {
            this.player = player

            snapshotTeam = player.team
            snapshotAlive = player.isAlive

            snapshotKills = player.kills
            snapshotDeaths = player.deaths

            val entity = player.entity
            if (entity != null) {
                snapshotHealth = entity.health
                snapshotPosition = entity.pos
                snapshotInventory = Inventories.copyOf(entity.inventory)
            } else {
                snapshotHealth = null
                snapshotPosition = null
                snapshotInventory = null
            }
        }

        constructor(context: GameContext, nbt: NbtCompound) {
            player = context.getPlayer(nbt["Player"].asCompound())

            snapshotTeam = nbt["Team"].toEnum()
            snapshotAlive = nbt["Alive"].toBoolean()
            snapshotKills = nbt["Kills"].toInt()
            snapshotDeaths = nbt["Deaths"].toInt()
            snapshotHealth = nbt["Health"]?.toFloat()
            snapshotPosition = nbt["Position"]?.let { it.toBlockPos().toCenterPos() }
            snapshotInventory = nbt["Inventory"]?.let { Inventories.fromNbt(it.asCompound()) }
        }

        fun toNbt(): NbtCompound {
            val nbt = nbtCompoundOf(
                "Player" to player,
                "Team" to snapshotTeam,
                "Alive" to snapshotAlive,
                "Kills" to snapshotKills,
                "Deaths" to snapshotDeaths,
            )
            if (snapshotHealth != null) {
                nbt["Health"] = snapshotHealth
            }
            if (snapshotPosition != null) {
                nbt["Position"] = toNbt(snapshotPosition.let(BlockPos::ofFloored))
            }
            if (snapshotInventory != null) {
                nbt["Inventory"] = toNbt(snapshotInventory)
            }
            return nbt
        }
    }

    private class Snapshot {
        val properties: GameProperties
        val players: List<SnapshotPlayer>


        constructor(properties: GameProperties, players: List<SnapshotPlayer>) {
            this.properties = properties
            this.players = players
        }

        constructor(context: GameContext, nbt: NbtCompound) {
            properties = GameProperties.instancesByName[nbt["Properties"].toActualString()]!!
            players = nbt["Players"].asList().map { SnapshotPlayer(context, it.asCompound()) }
        }

        fun toNbt() = nbtCompoundOf("Properties" to properties.name, "Players" to players)

        fun restore() {
            val game = Game.getGame()
            if (game.properties != properties) {
                game.destroy()
                game.initialize(properties)
            }
            val state = FrozenGameState()
            players.forEach {
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
                if (it.snapshotHealth != null && it.snapshotPosition != null) {
                    state.setFrozenPlayer(it.player.uuid, it.snapshotPosition, it.snapshotHealth)
                }
            }
            game.state = state
        }
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
            snapshot.restore()
            console.sendInfo("Restored snapshot of ${snapshot.players.size} players")
            context.barrierService.executeBlimpClose(console)
        }
    }

    fun executeSnapshotSave(console: Console) {
        ready = false

        val snapshot = Snapshot(properties, players.map(::SnapshotPlayer))
        snapshots.add(snapshot)

        val name = "mnsnd-snapshot-${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}.bin"
        val path = Game.CONFIGDIR.resolve(name)
        try {
            NbtIo.write(nbtCompoundOf("Snapshots" to snapshots), path.toFile())
            logger.info("Saved snapshot to file ${name}")
        } catch (cause: Exception) {
            logger.error("Failed to save snapshot to file ${name}", cause)
        }

        console.sendInfo("Created snapshot of ${snapshot.players.size} players")
    }

    fun executeSnapshotLoadBackup(console: Console, name: String?) {
        val path = if (name != null) {
            Game.CONFIGDIR.resolve(name)
        } else {
            try {
                Game.CONFIGDIR.listDirectoryEntries("mnsnd-snapshot-*").maxOf { it }
            } catch (ignored: NoSuchElementException) {
                console.sendError("No snapshot list backups available to load")
                return
            }
        }
        try {
            val nbt = NbtIo.read(path.toFile())!!
            val snapshots = nbt["Snapshots"].asList().map { Snapshot(context, it.asCompound()) }

            this.snapshots.clear()
            this.snapshots.addAll(snapshots)

            console.sendInfo("Successfully loaded ${snapshots.size} entries from snapshot list backup")

            val snapshot = snapshots.lastOrNull()
            if (snapshot != null) {
                snapshot.restore()
                console.sendInfo("Restored last snapshot of ${snapshot.players.size} players")
                context.barrierService.executeBlimpClose(console)
            }
        } catch (cause : Exception) {
            console.sendError("Failed to load snapshot list backup")
            logger.error("Failed to load snapshot list backup ${name}", cause)
        }
    }

}
