package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.block.entity.BarrelBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos

class SpecialGiftService : Service() {

    private val gifts = mutableMapOf<GamePlayer, ItemStack>()

    fun giftFor(player: GamePlayer): ItemStack? = gifts[player]

    fun handleDropInventory(player: GamePlayer, playerEntity: ServerPlayerEntity) {
        if (player.isGhost && Rules.giftsEnabled) {
            playerEntity.dropItem(giftFor(player) ?: return, true, false)
        }
    }

    private fun giftSet(player: GamePlayer, stack: ItemStack) {
        if (!stack.contentEquals(giftFor(player))) {
            gifts.put(player, stack.copy())
            player.entity?.sendMessage(Console.formatInfo(
                "Your ghostly item has been set to a",
                text(stack.name)
                    .styleParent { Style.EMPTY.withColor(Formatting.WHITE) }
                    .style { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(stack))) },
            ))
        }
    }
    private fun giftClear(player: GamePlayer) {
        if (gifts.remove(player) != null) {
            player.entity?.sendMessage(Console.formatInfo("Your ghostly item has been cleared"))
        }
    }

    fun handleLeverUse(player: GamePlayer, pos: BlockPos) {
        if (!Rules.giftsEnabled) return

        val container = world.getBlockEntity(pos.down())
        if (container !is BarrelBlockEntity) return

        val stack = container.asList().find { !it.isEmpty }
        if (stack != null) {
            giftSet(player, stack)
        } else {
            giftClear(player)
        }
    }

}
