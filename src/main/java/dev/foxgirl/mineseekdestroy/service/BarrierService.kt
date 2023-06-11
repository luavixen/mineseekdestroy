package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CompletableFuture

class BarrierService : Service() {

    private class Target(val pos: BlockPos, val stateOpen: BlockState, val stateClosed: BlockState)

    private var targetsArena = listOf<Target>()
    private var targetsBlimp = listOf<Target>()

    private suspend fun search(name: String, region: Region, offset: BlockPos): List<Target> =
        Editor
            .search(world, region) { !it.isAir && it.block !== Blocks.MAGENTA_WOOL }
            .await()
            .also {
                logger.info("BarrierService search in barrier template \"${name}\" returned ${it.size} result(s)")
            }
            .map {
                val pos = it.pos.add(offset)
                Target(pos, world.getBlockState(pos), if (it.state.block !== Blocks.ORANGE_WOOL) it.state else blockAir)
            }

    private suspend fun setupArena() {
        val target = properties.regionBarrierArenaTarget
        val template = properties.regionBarrierArenaTemplate
        targetsArena = search("arena", template, target.start.subtract(template.start))
    }
    private suspend fun setupBlimp() {
        val target = properties.regionBarrierBlimpTarget
        val template = properties.regionBarrierBlimpTemplate
        targetsBlimp = search("blimp", template, target.start.subtract(template.start))
    }

    private fun apply(targets: List<Target>, provider: (Target) -> BlockState) {
        targets.forEach { world.setBlockState(it.pos, provider(it)) }
    }

    override fun setup() {
        Async.run {
            await(
                go(::setupArena),
                go(::setupBlimp),
            )
        }
    }

    fun executeArenaOpen(console: Console) {
        apply(targetsArena, Target::stateOpen)
        console.sendInfo("Arena barriers opened")
    }
    fun executeArenaClose(console: Console) {
        apply(targetsArena, Target::stateClosed)
        console.sendInfo("Arena barriers closed")
    }

    fun executeBlimpOpen(console: Console) {
        properties.regionBarrierBlimpAdditions.forEach { Editor.edit(world, it) { _, _, _, _ -> blockAir }.terminate() }
        apply(targetsBlimp) { blockAir }
        console.sendInfo("Blimp barriers opened")
    }
    fun executeBlimpClose(console: Console) {
        properties.regionBarrierBlimpAdditions.forEach { Editor.edit(world, it) { _, _, _, _ -> blockBarrier }.terminate() }
        apply(targetsBlimp, Target::stateClosed)
        console.sendInfo("Blimp barriers closed")
    }

    private companion object {

        val blockAir = Blocks.AIR.defaultState!!
        val blockBarrier = Blocks.RED_STAINED_GLASS.defaultState!!

    }

}
