package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.Broadcast
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.time.Instant

class SpecialPianoService : Service() {

    private class Note(val index: Int, val time: Instant)

    private inner class Piano(val positions: List<BlockPos>) {
        private val notes = ArrayDeque<Note>(16)

        private fun notesAdd(index: Int) {
            while (notes.size >= 16) notes.removeFirst()
            notes.add(Note(index, Instant.now()))
        }

        private fun notesCheck(song: IntArray): Boolean {
            if (notes.size < song.size) {
                return false
            }

            val notes = notes.takeLast(song.size)

            val time = notes.first().time
            if (time.isBefore(Instant.now().minusSeconds(song.size.toLong() + 1))) {
                return false
            }

            for (i in song.indices) {
                if (notes[i].index != song[i]) return false
            }

            return true
        }

        fun play(player: GamePlayer, pos: BlockPos) {
            val index = positions.indexOf(pos)

            notesAdd(index)

            if (notesCheck(songAllStar)) {
                logger.info("Player ${player.nameQuoted} played piano song All-Star")
                player.entity?.networkHandler?.disconnect(Text.of("get out"))
            } else if (notesCheck(songSongOfTime)) {
                logger.info("Player ${player.nameQuoted} played piano song Song of Time")
                player.entity?.giveItemStack(ItemStack(Items.CLOCK))
            } else if (notesCheck(songMegalovania) || notesCheck(songSongOfStorms)) {
                logger.info("Player ${player.nameQuoted} played piano song Megalovania / Song of Storms")
                player.entity?.let { entity ->
                    entity.damage(entity.damageSources.outOfWorld(), Float.MAX_VALUE)
                    EntityType.LIGHTNING_BOLT.spawn(world, entity.blockPos, SpawnReason.COMMAND)
                }
            }

            val pitch = tuning[index]

            Broadcast.sendParticles(
                ParticleTypes.NOTE,
                pitch / 24.0F, 1,
                world, Vec3d(
                    pos.x.toDouble() + 0.5,
                    pos.y.toDouble() + 1.0,
                    pos.z.toDouble() + 0.5,
                ),
            )
            Broadcast.sendSound(
                SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(),
                SoundCategory.RECORDS,
                3.0F, tuning[index],
                world, pos.toCenterPos(),
            )
        }
    }

    private val pianos = HashMap<BlockPos, Piano>()

    fun handleInteract(player: GamePlayer, pos: BlockPos): ActionResult {
        if (state.isRunning && !(player.isAlive && player.isPlaying)) return ActionResult.PASS

        val piano1 = pianos[pos]
        if (piano1 != null) {
            piano1.play(player, pos)
            return ActionResult.SUCCESS
        }

        if (world.getBlockState(pos.down()).block !== Blocks.DARK_OAK_TRAPDOOR) return ActionResult.PASS

        val forward = arrayOf(
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
        )
            .find { world.getBlockState(pos.offset(it)).isAir } ?: return ActionResult.PASS

        val positions = ArrayDeque<BlockPos>(4).apply { add(pos) }

        fun collect(pos: BlockPos, direction: Direction, commit: (BlockPos) -> Unit) {
            val posNext = pos.offset(direction)
            if (world.getBlockState(posNext).block === Blocks.QUARTZ_SLAB) {
                commit(posNext)
                collect(posNext, direction, commit)
            }
        }

        collect(pos, forward.rotateYClockwise(), positions::addFirst)
        collect(pos, forward.rotateYCounterclockwise(), positions::addLast)

        if (positions.size != 4) return ActionResult.PASS

        val piano2 = Piano(positions.map(BlockPos::toImmutable))
        piano2.positions.forEach { pianos[it] = piano2 }
        piano2.play(player, pos)

        return ActionResult.SUCCESS
    }

    private companion object {

        private val tuning = listOf(8, 11, 15, 20).map { Math.pow(2.0, (it - 12).toDouble() / 12.0).toFloat() }.toFloatArray()

        private val songAllStar = intArrayOf(2, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 2)
        private val songMegalovania = intArrayOf(0, 0, 3, 2)
        private val songSongOfStorms = intArrayOf(0, 1, 3, 0, 1, 3)
        private val songSongOfTime = intArrayOf(2, 0, 1, 2, 0, 1)

    }

}
