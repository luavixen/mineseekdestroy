package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Inventories
import dev.foxgirl.mineseekdestroy.util.async.terminate
import dev.foxgirl.mineseekdestroy.util.stackOf
import net.minecraft.block.Blocks
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos

class SmokerService : Service() {

    private var positions = listOf<BlockPos>()

    private fun containers() = sequence {
        for (pos in positions) {
            val entity = world.getBlockEntity(pos)
            if (entity is AbstractFurnaceBlockEntity) yield(entity)
        }
    }

    override fun setup() {
        Editor
            .queue(world, properties.regionAll)
            .search { it.block === Blocks.SMOKER }
            .thenApply { results ->
                logger.info("SmokerService search for smokers returned ${results.size} result(s)")
                positions = results.map { it.pos }
            }
            .terminate()
    }

    fun executeClear(console: Console) {
        containers().forEach(Inventories::clear)
        console.sendInfo("Emptied smokers")
    }

    fun executeFill(console: Console) {
        containers().forEach {
            it.setStack(0, stackOf(Items.POTATO, 64))
            it.setStack(2, stackOf())
        }
        console.sendInfo("Filled smokers")
    }

}
