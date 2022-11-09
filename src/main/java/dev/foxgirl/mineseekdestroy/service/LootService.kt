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
import net.minecraft.block.entity.ShulkerBoxBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos

class LootService : Service() {

    private var positions = listOf<BlockPos>()

    private fun containers() = sequence {
        for (pos in positions) {
            val container = inventory(world.getBlockEntity(pos)) ?: continue
            if (!locked(container)) { yield(container) }
        }
    }

    override fun setup() {
        BlockFinder
            .search(world, Game.REGION_ALL) {
                containerBlocks.contains(it.block)
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
        val lootTable = Inventories.list(template)
        if (lootTable.isEmpty()) {
            console.sendError("Cannot create loot table, template chest at ${Game.TEMPLATE_LOOTTABLE} is empty")
            return
        }

        containers().forEach { container ->
            val offset = container.size().let { if (it == 54) it / 4 + 9 else it / 2 } - lootCount / 2
            for (i in 0 until lootCount) {
                container.setStack(i + offset, lootTable.random().copy())
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

    private companion object {

        private val containerBlocks = setOf(
            Blocks.CHEST,
            Blocks.BARREL,
            Blocks.SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX,
            Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX,
            Blocks.YELLOW_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX,
            Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX,
            Blocks.GREEN_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX,
        )

        private fun inventory(entity: BlockEntity?): Inventory? {
            if (entity is ChestBlockEntity) {
                val pos = entity.pos
                val world = entity.world
                val state = entity.cachedState
                return ChestBlock.getInventory(state.block as ChestBlock, state, world, pos, true)
            }
            if (entity is BarrelBlockEntity) {
                return entity
            }
            if (entity is ShulkerBoxBlockEntity) {
                return entity
            }
            return null
        }

        private fun locked(container: Inventory): Boolean {
            return Inventories.list(container).any { !it.isEmpty && Game.ILLEGAL_ITEMS.contains(it.item) }
        }

    }

}
