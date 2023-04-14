package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Inventories
import net.minecraft.block.Blocks
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.item.ItemStack
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
            .search(world, properties.regionAll) { it.block === Blocks.SMOKER }
            .thenApply { results ->
                logger.info("SmokerService search for smokers returned ${results.size} result(s)")
                positions = results.map { it.pos }
            }
    }

    fun executeClear(console: Console) {
        containers().forEach(Inventories::clear)
        console.sendInfo("Emptied smokers")
    }

    fun executeFill(console: Console) {
        containers().forEach {
            it.setStack(0, ItemStack(Items.POTATO, 64))
            it.setStack(2, ItemStack.EMPTY)
        }
        console.sendInfo("Filled smokers")
    }

}
