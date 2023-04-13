package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CompletableFuture

class BarrierService : Service() {

    private class Target(val pos: BlockPos, val state: BlockState)

    private var targetsArena = listOf<Target>()
    private var targetsBlimp = listOf<Target>()

    private fun search(name: String, region: Region, offset: BlockPos): CompletableFuture<List<Target>> {
        return Editor
            .search(world, region) {
                !it.isAir && it.block !== Blocks.MAGENTA_WOOL
            }
            .handle { results, err ->
                if (err != null) {
                    logger.error("BarrierService search in barrier template \"${name}\" failed", err)
                    listOf()
                } else {
                    logger.info("BarrierService search in barrier template \"${name}\" returned ${results.size} result(s)")
                    results.map { Target(it.pos.add(offset), it.state) }
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

    override fun setup() {
        setupArena()
        setupBlimp()
    }

    private val nothing: BlockState = Blocks.AIR.defaultState

    fun executeArenaOpen(console: Console) {
        targetsArena.forEach { world.setBlockState(it.pos, nothing) }
        console.sendInfo("Arena barriers opened")
    }
    fun executeArenaClose(console: Console) {
        targetsArena.forEach { world.setBlockState(it.pos, it.state) }
        console.sendInfo("Arena barriers closed")
    }

    fun executeBlimpOpen(console: Console) {
        targetsBlimp.forEach { world.setBlockState(it.pos, nothing) }
        console.sendInfo("Blimp barriers opened")
    }
    fun executeBlimpClose(console: Console) {
        targetsBlimp.forEach { world.setBlockState(it.pos, it.state) }
        console.sendInfo("Blimp barriers closed")
    }

}
