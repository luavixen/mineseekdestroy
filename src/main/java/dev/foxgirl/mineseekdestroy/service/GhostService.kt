package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import net.minecraft.block.Blocks
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
import net.minecraft.scoreboard.Team
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos

class GhostService : Service() {

    override fun update() {
        for ((player, playerEntity) in playerEntitiesNormal) {
            if (player.isGhost) {
                playerEntity.addStatusEffect(StatusEffectInstance(StatusEffects.INVISIBILITY, 80))
                playerEntitiesIn.values.forEach {
                    it.setInPowderSnow(it.squaredDistanceTo(playerEntity) <= 16)
                    val team = it.scoreboardTeam as? Team
                    if (team != null) {
                        it.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(team, player.name, TeamS2CPacket.Operation.ADD))
                    }
                }
            }
        }
    }

    fun handleInteract(player: GamePlayer, pos: BlockPos): ActionResult {
        world.setBlockState(pos, Blocks.MAGENTA_CONCRETE_POWDER.defaultState)
        player.inventory?.insertStack(ectoplasm.copy())
        return ActionResult.SUCCESS
    }

    private companion object {

        private val ectoplasm =
            ItemStack(Items.SLIME_BLOCK).setCustomName(Text.literal("Ectogasm").styled { it.withColor(Formatting.GREEN).withItalic(false) })

    }

}
