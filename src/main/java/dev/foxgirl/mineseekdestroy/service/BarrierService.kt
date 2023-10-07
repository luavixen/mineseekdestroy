package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.await
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.concurrent.CompletableFuture

class BarrierService : Service() {

    private class BlockInfo(val state: BlockState, val nbt: NbtCompound? = null) {
        constructor(world: World, pos: BlockPos) : this(world.getBlockState(pos), world.getBlockEntity(pos))
        constructor(state: BlockState, entity: BlockEntity?) : this(state, entity?.createNbtWithId())
        fun set(world: World, pos: BlockPos) {
            world.setBlockState(pos, state)
            if (nbt != null) world.getBlockEntity(pos)?.readNbt(nbt)
        }
    }

    private class Target(val pos: BlockPos, val infoOpen: BlockInfo, val infoClosed: BlockInfo)

    private var targetsArena = listOf<Target>()
    private var targetsBlimp = listOf<Target>()

    private fun search(name: String, template: Region, target: Region): CompletableFuture<List<Target>> =
        Async.execute {
            val results = Editor.queue(world, template).search { !it.isAir && it.block !== Blocks.MAGENTA_WOOL }.await()
            logger.info("BarrierService search in barrier template \"${name}\" returned ${results.size} result(s)")

            val offset = target.start.subtract(template.start)

            results.map {
                val pos = it.pos.add(offset)
                val state = if (it.state.block !== Blocks.ORANGE_WOOL) it.state else blockAir
                Target(pos, BlockInfo(world, pos), BlockInfo(state, world.getBlockEntity(it.pos)))
            }
        }

    private suspend fun setupArena() {
        targetsArena = search("arena", properties.regionBarrierArenaTemplate, properties.regionBarrierArenaTarget).await()
    }
    private suspend fun setupBlimp() {
        val tasks = buildList<CompletableFuture<List<Target>>> {
            add(search("blimp", properties.regionBarrierBlimpTemplate, properties.regionBarrierBlimpTarget))
            addAll(properties.regionBarrierBlimpBalloonTargets.mapIndexed { i, target -> search("blimp-balloon-${i}", properties.regionBarrierBlimpBalloonTemplate, target) })
        }
        targetsBlimp = Async.awaitAll(tasks).flatten()
    }

    private fun apply(targets: List<Target>, provider: (Target) -> BlockInfo) {
        targets.forEach { provider(it).set(world, it.pos) }
    }

    override fun setup() {
        Async.go {
            awaitAll(
                execute(::setupArena),
                execute(::setupBlimp),
            )
        }
    }

    fun executeArenaOpen(console: Console) {
        apply(targetsArena, Target::infoOpen)
        console.sendInfo("Arena barriers opened")
    }
    fun executeArenaClose(console: Console) {
        apply(targetsArena, Target::infoClosed)
        console.sendInfo("Arena barriers closed")
    }

    fun executeBlimpOpen(console: Console) {
        Async.go {
            val action = Editor.Action { state, _, _, _ -> if (state.block === Blocks.RED_STAINED_GLASS) blockAir else null }
            awaitAll(properties.regionBarrierBlimpFills.map { region -> Editor.queue(world, region).edit(action) })
            apply(targetsBlimp) { infoAir }
            console.sendInfo("Blimp barriers opened")
        }
    }
    fun executeBlimpClose(console: Console) {
        Async.go {
            val action = Editor.Action { state, _, _, _ -> if (state.isAir) blockBarrier else null }
            awaitAll(properties.regionBarrierBlimpFills.map { region -> Editor.queue(world, region).edit(action) })
            apply(targetsBlimp, Target::infoClosed)
            console.sendInfo("Blimp barriers closed")
        }
    }

    private companion object {

        val blockAir = Blocks.AIR.defaultState!!
        val blockBarrier = Blocks.RED_STAINED_GLASS.defaultState!!
        val infoAir = BlockInfo(blockAir)
        val infoBarrier = BlockInfo(blockBarrier)

    }

}
