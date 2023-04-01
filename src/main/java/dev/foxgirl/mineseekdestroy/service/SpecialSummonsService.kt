package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.BlockFinder
import net.minecraft.block.Blocks
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos

class SpecialSummonsService : Service() {

    private enum class Theology {
        DEEP, OCCULT, COSMOS, BARTER, FLAME,
    }

    private class Altar(
        val pos: BlockPos,
        val theology: Theology,
    )

    private var altars = mapOf<BlockPos, Altar>()

    override fun setup() {
        BlockFinder
            .search(world, properties.regionAll) {
                it.block === Blocks.FLETCHING_TABLE
            }
            .handle { results, err ->
                if (err != null) {
                    logger.error("SpecialSummonService search for altars failed")
                } else {
                    logger.info("SpecialSummonService search for altars returned ${results.size} result(s)")
                    altars = results
                        .mapNotNull {
                            val theology = when (world.getBlockState(it.pos.down()).block) {
                                Blocks.DARK_PRISMARINE -> Theology.DEEP
                                Blocks.DARK_OAK_TRAPDOOR -> Theology.OCCULT
                                Blocks.OBSIDIAN -> Theology.COSMOS
                                Blocks.WAXED_CUT_COPPER -> Theology.BARTER
                                Blocks.NETHER_BRICKS -> Theology.FLAME
                                else -> return@mapNotNull null
                            }
                            Altar(it.pos, theology)
                        }
                        .associateBy { it.pos }
                }
            }
    }

    fun handleAltarOpen(player: GamePlayer, pos: BlockPos): ActionResult {
        val altar: Altar? = altars[pos]
        if (altar != null) {
            Game.CONSOLE_OPERATORS.sendInfo("Player", player.displayName, "opened altar at", pos, "with theology", altar.theology.name)
        } else {
            Game.CONSOLE_OPERATORS.sendInfo("Player", player.displayName, "tried to open invalid altar at", pos)
        }
        return ActionResult.FAIL
    }

}
