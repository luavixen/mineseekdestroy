package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.util.BlockFinder
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Inventories
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.BarrelBlockEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos

class LootService : Service() {

    private val positionsLock = Any()
    private var positions = listOf<BlockPos>()
        get() = synchronized(positionsLock) { field }
        set(value) = synchronized(positionsLock) { field = value }

    private fun inventory(entity: BlockEntity?): Inventory? {
        return when (entity) {
            is ChestBlockEntity -> {
                val state = entity.cachedState
                val world = entity.world
                val pos = entity.pos
                ChestBlock.getInventory(state.block as ChestBlock, state, world, pos, true)
            }
            is BarrelBlockEntity -> {
                entity
            }
            else -> null
        }
    }

    private fun locked(container: Inventory): Boolean {
        return Inventories.list(container).any { !it.isEmpty && Game.ILLEGAL_ITEMS.contains(it.item) }
    }

    private fun containers() = sequence {
        for (pos in positions) {
            val container = inventory(world.getBlockEntity(pos)) ?: continue
            if (!locked(container)) { yield(container) }
        }
    }

    override fun setup() {
        BlockFinder
            .search(world, Game.REGION_ALL) {
                it.block === Blocks.CHEST || it.block === Blocks.BARREL
            }
            .handle { results, err ->
                if (err != null) {
                    logger.error("LootService search for containers failed", err)
                } else {
                    logger.info("LootService search for containers returned ${results.size} result(s)")
                    positions = results.map { it.pos }
                }
            }
    }

    fun executeClear(console: Console) {
        containers().forEach(Inventories::clear)
        console.sendInfo("Cleared all containers")
    }

    fun executeFill(console: Console) {
        val template = inventory(world.getBlockEntity(Game.TEMPLATE_LOOTTABLE))
        if (template == null) {
            console.sendError("Failed to read template chest at ${Game.TEMPLATE_LOOTTABLE}")
            return
        }

        val lootCount = game.getRuleInt(Game.RULE_LOOT_COUNT)
        val lootTable = Inventories.list(template).filter { !it.isEmpty }
        if (lootTable.isEmpty()) {
            console.sendError("Cannot create loot table, template chest at ${Game.TEMPLATE_LOOTTABLE} is empty")
            return
        }

        containers().forEach { container ->
            val offset = container.size().let { if (it == 54) it / 4 + 9 else it / 2 } - lootCount / 2
            for (i in 0 until lootCount) {
                container.setStack(i + offset, lootTable.random())
            }
        }

        console.sendInfo("Filled all containers with loot")
    }

    fun handleContainerOpen(entity: BlockEntity?): ActionResult {
        val container = inventory(entity)
        if (container != null && locked(container)) {
            return ActionResult.FAIL
        }
        return ActionResult.PASS
    }

}
