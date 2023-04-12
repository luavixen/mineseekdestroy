package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import dev.foxgirl.mineseekdestroy.util.ArrayMap
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.time.Instant

class SpecialPianoService : Service() {

    private class Note(val index: Int, val time: Instant)

    private inner class Piano(val positions: List<BlockPos>) {
        private val notes = ArrayDeque<Note>(4)

        private fun notesAdd(index: Int) {
            while (notes.size >= 4) notes.removeFirst()
            notes.add(Note(index, Instant.now()))
        }

        private fun notesCheck(player: GamePlayer) {
            if (notes.size < 4) return

            val n1 = notes[notes.size - 4]
            val n2 = notes[notes.size - 3]
            val n3 = notes[notes.size - 2]
            val n4 = notes[notes.size - 1]

            if (n1.time.isBefore(Instant.now().minusSeconds(5))) return

            if (
                n1.index == 0 &&
                n2.index == 0 &&
                n3.index == 3 &&
                n4.index == 2
            ) {
                player.entity?.let { entity ->
                    entity.damage(entity.damageSources.outOfWorld(), Float.MAX_VALUE)
                    EntityType.LIGHTNING_BOLT.spawn(world, entity.blockPos, SpawnReason.COMMAND)
                }
            }
        }

        fun play(player: GamePlayer, pos: BlockPos) {
            val index = positions.indexOf(pos)

            notesAdd(index)
            notesCheck(player)

            val pitch = tuning[index]

            world.spawnParticles(
                ParticleTypes.NOTE,
                pos.x.toDouble() + 0.5,
                pos.y.toDouble() + 1.0,
                pos.z.toDouble() + 0.5,
                1,
                0.0, 0.0, 0.0,
                pitch.toDouble() / 24.0,
            )
            world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(),
                SoundCategory.RECORDS,
                3.0F,
                tuning[index],
            )
        }
    }

    private val pianos = HashMap<BlockPos, Piano>()

    fun handleInteract(player: GamePlayer, pos: BlockPos): Boolean {
        if (state is RunningGameState && !(player.isAlive && player.isPlaying)) return false

        val piano1 = pianos[pos]
        if (piano1 != null) {
            piano1.play(player, pos)
            return true
        }

        if (world.getBlockState(pos.down()).block !== Blocks.DARK_OAK_TRAPDOOR) return false

        val forward = arrayOf(
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
        )
            .find { world.getBlockState(pos.offset(it)).isAir } ?: return false

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

        if (positions.size != 4) return false

        val piano2 = Piano(positions.map(BlockPos::toImmutable))
        piano2.positions.forEach { pianos[it] = piano2 }
        piano2.play(player, pos)

        return true
    }

    private companion object {

        private val tuning = listOf(8, 11, 15, 20).map { Math.pow(2.0, (it - 12).toDouble() / 12.0).toFloat() }.toFloatArray()

    }

}
