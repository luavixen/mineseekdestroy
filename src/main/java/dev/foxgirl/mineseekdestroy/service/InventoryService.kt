package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

class InventoryService : Service() {

    fun executeClear(console: Console) = executeClear(console, players)
    fun executeClear(console: Console, targets: Collection<GamePlayer>) {
        var count = 0
        for (target in targets) {
            if (target.isPlayingOrGhost) {
                val inventory = target.inventory
                if (inventory != null) {
                    Inventories.clear(inventory)
                    count++
                }
            }
        }
        console.sendInfo("Cleared inventories of ${count} player(s)")
    }

    fun executeFill(console: Console) = executeFill(console, players)
    fun executeFill(console: Console, targets: Collection<GamePlayer>) {
        val template = world.getBlockEntity(properties.templateInventory)
        if (template !is ChestBlockEntity) {
            console.sendError("Failed to read template chest at ${properties.templateInventory}")
            return
        }
        var count = 0
        for (target in targets) {
            if (target.isPlayingOrGhost) {
                val inventory = target.inventory
                if (inventory != null) {
                    Inventories.copy(template, inventory)
                    count++
                }
            }
        }
        console.sendInfo("Updated inventories of ${count} players")
    }

    override fun update() {
        if (state.isRunning) {
            for (player in players) mirrorUpdate(player)
        } else {
            for (player in players) mirrorReset(player)
        }
    }

    private val mirrors = HashMap<GamePlayer, Inventory>(32)

    private fun mirrorUpdate(player: GamePlayer) {
        val actual = player.inventory ?: return
        val mirror = mirrors[player]
        if (player.isSpectator) {
            if (mirror != null) {
                if (!Inventories.equals(mirror, actual)) Inventories.copy(mirror, actual)
            } else {
                mirrors[player] = Inventories.copyOf(actual)
            }
        } else {
            mirrorReset(player)
        }
    }

    private fun mirrorReset(player: GamePlayer) {
        mirrors.remove(player)
    }

    private class InventoryViewScreenHandlerFactory(private val targetInventory: PlayerInventory)
        : DynamicScreenHandlerFactory<InventoryViewScreenHandler>()
    {
        override val name: Text get() = targetInventory.player.nameForScoreboard.asText()
        override fun construct(sync: Int, playerInventory: PlayerInventory) =
            InventoryViewScreenHandler(sync, playerInventory, targetInventory)
    }

    private class InventoryViewScreenHandler(
        sync: Int,
        playerInventory: PlayerInventory,
        targetInventory: PlayerInventory,
    ) : DynamicChestScreenHandler(5, sync, playerInventory) {
        init {
            addChestInventorySlots { index, _, _, x, y ->
                when (index) {
                    in 0..26 -> InputSlot(index + 9, x, y, targetInventory)
                    in 27..35 -> InputSlot(index - 27, x, y, targetInventory)
                    36 -> InputSlot(39, x, y, targetInventory)
                    37 -> InputSlot(38, x, y, targetInventory)
                    38 -> InputSlot(37, x, y, targetInventory)
                    39 -> InputSlot(36, x, y, targetInventory)
                    44 -> InputSlot(40, x, y, targetInventory)
                    else -> OutputSlot(index, x, y)
                }
            }
            addPlayerInventorySlots()
        }
        override fun handleTakeResult(slot: OutputSlot, stack: ItemStack) {
        }
        override fun handleUpdateResult() {
        }
        override fun handleClosed() {
        }
    }

    fun executeView(console: Console, player: GamePlayer, target: GamePlayer) {
        val inventory = target.inventory
        if (inventory != null) {
            player.entity?.openHandledScreen(InventoryViewScreenHandlerFactory(inventory))
            console.sendInfo("Opening inventory of ${target.name}")
        } else {
            console.sendError("Failed to read inventory of ${target.name}")
        }
    }

}
