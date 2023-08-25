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

    private fun search(name: String, template: Region, target: Region): CompletableFuture<List<Target>> =
        Async.go {
            val results = Editor.search(world, template) { !it.isAir && it.block !== Blocks.MAGENTA_WOOL }.await()
            logger.info("BarrierService search in barrier template \"${name}\" returned ${results.size} result(s)")

            val offset = target.start.subtract(template.start)

            results.map {
                val pos = it.pos.add(offset)
                val state = if (it.state.block !== Blocks.ORANGE_WOOL) it.state else blockAir
                Target(pos, world.getBlockState(pos), state)
            }
        }

    private suspend fun setupArena() {
        targetsArena = search("arena", properties.regionBarrierArenaTemplate, properties.regionBarrierArenaTarget).await()
    }
    private suspend fun setupBlimp() {
        val tasks = buildList<CompletableFuture<List<Target>>> {
            add(search("blimp", properties.regionBarrierBlimpTemplate, properties.regionBarrierBlimpTarget))
            addAll(properties.regionBarrierBlimpBalloonTargets.map { template -> search("blimp-balloon", properties.regionBarrierBlimpBalloonTemplate, template) })
        }
        targetsBlimp = Async.await(tasks).flatten()
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
        Async.run {
            fun fillOpen(region: Region) = Editor.edit(world, region) { state, _, _, _ -> if (state.block === Blocks.RED_STAINED_GLASS) blockAir else null }
            await(properties.regionBarrierBlimpFills.map(::fillOpen))
            apply(targetsBlimp) { blockAir }
            console.sendInfo("Blimp barriers opened")
        }
    }
    fun executeBlimpClose(console: Console) {
        Async.run {
            fun fillClose(region: Region) = Editor.edit(world, region) { state, _, _, _ -> if (state.isAir) blockBarrier else null }
            await(properties.regionBarrierBlimpFills.map(::fillClose))
            apply(targetsBlimp, Target::stateClosed)
            console.sendInfo("Blimp barriers closed")
        }
    }

    private companion object {

        val blockAir = Blocks.AIR.defaultState!!
        val blockBarrier = Blocks.RED_STAINED_GLASS.defaultState!!

    }

}
