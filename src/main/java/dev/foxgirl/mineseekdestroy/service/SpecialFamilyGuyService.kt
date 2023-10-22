package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.Broadcast
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import dev.foxgirl.mineseekdestroy.util.async.await
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.*

class SpecialFamilyGuyService : Service() {

    fun handleFamilyGuyBlockPlaced(player: GamePlayer, blockHit: BlockHitResult) {
        val playerEntity = player.entity ?: return
        Async.go { handleFamilyGuyBlockPlacedAsync(player, playerEntity, blockHit) }
    }

    private suspend fun handleFamilyGuyBlockPlacedAsync(player: GamePlayer, playerEntity: PlayerEntity, blockHit: BlockHitResult) {
        val structure = structures[playerEntity.horizontalFacing]!!

        val targets =
            targetOffsets(blockHit.blockPos) +
            targetOffsets(blockHit.blockPos.offset(blockHit.side))

        Async.delay()

        logger.info("Player '${player.name}' attempting to place a family guy block")

        val target = targets.find { pos -> world.getBlockState(pos).block === Blocks.TARGET }
        if (target == null) return

        logger.info("Player '${player.name}' placed family guy block at ${target.toShortString()}")

        val offset = target.subtract(structure.center)
        val region = structure.region.offset(offset)

        val center = region.center

        val positions = ArrayList<BlockPos>(128)

        Editor
            .queue(world, region)
            .edit { _, x, y, z ->
                val pos = BlockPos(x, y, z)
                val state = world.getBlockState(pos.subtract(offset))
                if (state.isAir) {
                    null
                } else {
                    positions.add(pos)
                    state
                }
            }
            .await()

        positions.forEach {
            Broadcast.sendParticles(ParticleTypes.POOF, 0.25F, 5, world, it.toCenterPos())
        }

        Scheduler.delay(0.05) {
            notesHarp.forEach { Broadcast.sendSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.BLOCKS, 2.0F, it, world, center) }
        }
        Scheduler.delay(0.45) {
            notesBass.forEach { Broadcast.sendSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 2.0F, it, world, center) }
        }

        for (entity in world.players) {
            if (!region.contains(entity)) continue

            var i = 0

            while (i < 64) {
                if (!world.isAir(entity.blockPos.add(0, i + 1, 0))) {
                    i += 2
                } else if (!world.isAir(entity.blockPos.add(0, i, 0))) {
                    i += 1
                } else {
                    break
                }
            }

            if (i != 0) {
                entity.networkHandler.requestTeleport(
                    entity.x,
                    entity.y + i.toDouble(),
                    entity.z,
                    entity.yaw, entity.pitch,
                )
            }
        }
    }

    private companion object {

        private fun targetOffsets(pos: BlockPos) =
            arrayOf<BlockPos>(
                pos,
                pos.offset(DOWN),
                pos.offset(UP),
                pos.offset(NORTH),
                pos.offset(SOUTH),
                pos.offset(WEST),
                pos.offset(EAST),
            )

        private fun convertNote(note: Int) = Math.pow(2.0, (note - 12).toDouble() / 12.0).toFloat()

        private val notesHarp = intArrayOf(4, 6, 10, 15, 18).map(::convertNote).toFloatArray()
        private val notesBass = intArrayOf(6, 18).map(::convertNote).toFloatArray()

        private class Structure(val region: Region, val center: BlockPos)

        private val structures = enumMapOf<Direction, Structure>(
            NORTH to Structure(Region(BlockPos(4, -41, -594), BlockPos(1, -47, -597)), BlockPos(2, -47, -595)),
            EAST to Structure(Region(BlockPos(1, -41, -585), BlockPos(4, -47, -588)), BlockPos(2, -47, -587)),
            SOUTH to Structure(Region(BlockPos(9, -41, -588), BlockPos(12, -47, -585)), BlockPos(11, -47, -587)),
            WEST to Structure(Region(BlockPos(12, -41, -597), BlockPos(9, -47, -594)), BlockPos(11, -47, -595)),
        )

    }

}
