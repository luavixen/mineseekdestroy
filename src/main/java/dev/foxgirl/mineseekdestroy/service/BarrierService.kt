package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CompletableFuture

class BarrierService : Service() {

    private class Target(val pos: BlockPos, val stateOpen: BlockState, val stateClosed: BlockState)

    private var targetsArena = listOf<Target>()
    private var targetsBlimp = listOf<Target>()

    private fun search(name: String, region: Region, offset: BlockPos): CompletableFuture<List<Target>> {
        return Editor
            .search(world, region) {
                !it.isAir && it.block !== Blocks.MAGENTA_WOOL
            }
            .thenApply { results ->
                logger.info("BarrierService search in barrier template \"${name}\" returned ${results.size} result(s)")

                results.map {
                    val pos = it.pos.add(offset)
                    Target(pos, world.getBlockState(pos), if (it.state.block !== Blocks.ORANGE_WOOL) it.state else air)
                }
            }
    }

    private fun setupArena() {
        val target = properties.regionBarrierArenaTarget
        val template = properties.regionBarrierArenaTemplate
        search("arena", template, target.start.subtract(template.start)).thenAccept { targetsArena = it }
    }
    private fun setupBlimp() {
        val target = properties.regionBarrierBlimpTarget
        val template = properties.regionBarrierBlimpTemplate
        search("blimp", template, target.start.subtract(template.start)).thenAccept { targetsBlimp = it }
    }

    private fun apply(targets: List<Target>, provider: (Target) -> BlockState) {
        targets.forEach { world.setBlockState(it.pos, provider(it)) }
    }

    override fun setup() {
        setupArena()
        setupBlimp()
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
        apply(targetsBlimp) { air }
        console.sendInfo("Blimp barriers opened")
    }
    fun executeBlimpClose(console: Console) {
        apply(targetsBlimp, Target::stateClosed)
        console.sendInfo("Blimp barriers closed")
    }

    private companion object {

        @JvmStatic
        val air = Blocks.AIR.defaultState!!

    }

}
