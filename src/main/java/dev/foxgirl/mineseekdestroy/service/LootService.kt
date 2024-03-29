package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.terminate
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.BarrelBlockEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.ShulkerBoxBlockEntity
import net.minecraft.inventory.DoubleInventory
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

    private fun template(): Inventory? {
        val inventories = sequence {
            for (i in 0 until world.topY) {
                yield(inventory(world.getBlockEntity(properties.templateLoottable.up(i))) ?: break)
            }
        }
        return inventories.reduceOrNull(::DoubleInventory)
    }

    override fun setup() {
        Editor
            .queue(world, properties.regionAll)
            .search { containerBlocks.contains(it.block) }
            .thenApply { results ->
                logger.info("LootService search for containers returned ${results.size} result(s)")
                positions = results.map { it.pos }
            }
            .terminate()
    }

    fun handleRoundEnd() {
        val lootStacks by lazy {
            val template = template()
            if (template != null) template.asList().filter { !it.isEmpty } else listOf()
        }
        for (container in containers()) {
            val stacks = container.asList()
            for (i in stacks.indices) {
                val nbt = stacks[i].nbt ?: continue
                if ("MsdSoul" in nbt) {
                    stacks[i] = lootStacks.randomOrNull()?.copy() ?: stackOf()
                } else if ("MsdBookCobbled" in nbt) {
                    stacks[i] = context.pagesService.randomBook()
                }
            }
        }
    }

    fun executeClear(console: Console) {
        containers().forEach(Inventories::clear)
        console.sendInfo("Cleared all containers")
    }

    fun executeFill(console: Console) {
        val template = template()
        if (template == null) {
            console.sendError("Failed to find template chest(s) at ${properties.templateLoottable}")
            return
        }

        val lootCount = Rules.lootCount
        val lootStacks = template.asList().map { it.copy().also(GameItems::replace) }
        if (lootStacks.isEmpty()) {
            console.sendError("Cannot create loot table, template chest(s) at ${properties.templateLoottable} are empty")
            return
        }

        for (container in containers()) {
            val offset = container.size().let { if (it == 54) it / 4 + 9 else it / 2 } - lootCount / 2
            for (i in 0 until lootCount) {
                container.setStack(i + offset, lootStacks.random().copy())
            }
        }

        console.sendInfo("Filled all containers with loot")
    }

    fun executeDebugClean(console: Console) {
        val template = template()
        if (template == null) {
            console.sendError("Failed to find template chest(s) at ${properties.templateLoottable}")
            return
        }

        template.asList().forEach(GameItems::replace)
        console.sendInfo("Updated all items in template chest(s)")
    }

    fun handleContainerOpen(entity: BlockEntity?): ActionResult {
        val container = inventory(entity)
        if (container != null && locked(container)) {
            return ActionResult.FAIL
        }
        return ActionResult.PASS
    }

    private companion object {

        private val containerBlocks = immutableSetOf(
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
            return container.asList().any { !it.isEmpty && Game.ILLEGAL_ITEMS.contains(it.item) }
        }

    }

}
