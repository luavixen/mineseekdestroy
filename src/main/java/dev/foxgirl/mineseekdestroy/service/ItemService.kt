package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import dev.foxgirl.mineseekdestroy.util.data
import dev.foxgirl.mineseekdestroy.util.give
import dev.foxgirl.mineseekdestroy.util.set
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity

class ItemService : Service() {

    private inline fun Inventory.forEach(action: (ItemStack, Item, Int) -> Unit) {
        for (i in 0 until size()) {
            val stack: ItemStack = getStack(i)
            val item: Item = stack.item
            if (item !== AIR) action(stack, item, i)
        }
    }

    fun handleDropInventory(entity: ServerPlayerEntity) {
        if (!context.getPlayer(entity).isPlaying) return

        val inventory = entity.inventory

        inventory.forEach { stack, item, i ->
            if (droppedItems.contains(item)) {
                inventory.setStack(i, ItemStack.EMPTY)
                entity.dropItem(stack, true, false)
            }
        }
    }

    fun addStackToInventory(entity: ServerPlayerEntity, stack: ItemStack, wait: Boolean) {
        val copy = stack.copy()
        Async.go {
            if (wait) {
                delay(1.0)
            }
            for (i in 1..10) {
                if (entity.give(copy, false)) break
                delay(0.5)
            }
        }
    }

    override fun update() {
        val running = state.isRunning

        for ((player, playerEntity) in playerEntitiesNormal) {
            if (running && player.isSpectator) continue

            val inventory = playerEntity.inventory

            val toolStacks = toolStackMaps[player.team]

            val powderItem = when (player.team) {
                GameTeam.PLAYER_DUEL -> BROWN_CONCRETE_POWDER
                GameTeam.PLAYER_WARDEN -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_BLACK -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_YELLOW -> YELLOW_CONCRETE_POWDER
                GameTeam.PLAYER_BLUE -> BLUE_CONCRETE_POWDER
                else -> MAGENTA_CONCRETE_POWDER
            }

            inventory.forEach { stack, item, i ->
                if (toolStacks != null) {
                    val tool = Tool.from(stack)
                    if (tool != null) {
                        val toolStack = toolStacks[tool]!!
                        if (!ItemStack.areItemsEqual(toolStack, stack)) inventory.setStack(i, toolStack.copy())
                        return@forEach
                    }
                }

                val replacementStack = replacementItems[item]
                if (replacementStack != null && replacementStack.nbt != stack.nbt) {
                    val stackNew = replacementStack.copyWithCount(stack.count)
                    inventory.setStack(i, ItemStack.EMPTY)
                    if (!inventory.insertStack(i, stackNew)) {
                        inventory.setStack(i, stackNew)
                    }
                    return@forEach
                }

                if (powderItem != null && powderItem !== item && powderItems.contains(item)) {
                    val stackNew = ItemStack(powderItem, stack.count)
                    inventory.setStack(i, ItemStack.EMPTY)
                    if (!inventory.insertStack(i, stackNew)) {
                        inventory.setStack(i, stackNew)
                    }
                    return@forEach
                }

                if (illegalItems.contains(item)) {
                    inventory.setStack(i, ItemStack.EMPTY)
                    return@forEach
                }

                val nbt: NbtCompound? = stack.nbt
                if (nbt != null && nbt.contains("MsdIllegal")) {
                    inventory.setStack(i, ItemStack.EMPTY)
                    return@forEach
                }

                if (
                    (bookItems.contains(item)) &&
                    (nbt == null || (!nbt.contains("MsdBook") && !nbt.contains("MsdPage")))
                ) {
                    inventory.setStack(i, ItemStack.EMPTY)
                    return@forEach
                }
            }
        }
    }

    private companion object {

        private val replacementItems = GameItems.replacements

        private val droppedItems = Game.DROPPED_ITEMS

        private val illegalItems = Game.ILLEGAL_ITEMS

        private val powderItems = immutableSetOf(
            WHITE_CONCRETE_POWDER, WHITE_CONCRETE,
            ORANGE_CONCRETE_POWDER, ORANGE_CONCRETE,
            MAGENTA_CONCRETE_POWDER, MAGENTA_CONCRETE,
            LIGHT_BLUE_CONCRETE_POWDER, LIGHT_BLUE_CONCRETE,
            YELLOW_CONCRETE_POWDER, YELLOW_CONCRETE,
            LIME_CONCRETE_POWDER, LIME_CONCRETE,
            PINK_CONCRETE_POWDER, PINK_CONCRETE,
            GRAY_CONCRETE_POWDER, GRAY_CONCRETE,
            LIGHT_GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE,
            CYAN_CONCRETE_POWDER, CYAN_CONCRETE,
            PURPLE_CONCRETE_POWDER, PURPLE_CONCRETE,
            BLUE_CONCRETE_POWDER, BLUE_CONCRETE,
            BROWN_CONCRETE_POWDER, BROWN_CONCRETE,
            GREEN_CONCRETE_POWDER, GREEN_CONCRETE,
            RED_CONCRETE_POWDER, RED_CONCRETE,
            BLACK_CONCRETE_POWDER, BLACK_CONCRETE,
            WHITE_TERRACOTTA, LIGHT_GRAY_TERRACOTTA,
        )

        private val bookItems = immutableSetOf(
            WHITE_BANNER, ORANGE_BANNER, MAGENTA_BANNER, LIGHT_BLUE_BANNER,
            YELLOW_BANNER, LIME_BANNER, PINK_BANNER, GRAY_BANNER,
            LIGHT_GRAY_BANNER, CYAN_BANNER, PURPLE_BANNER, BLUE_BANNER,
            BROWN_BANNER, GREEN_BANNER, RED_BANNER, BLACK_BANNER,
            PAPER, BOOK,
            WRITTEN_BOOK,
            WRITABLE_BOOK,
            KNOWLEDGE_BOOK,
            ENCHANTED_BOOK,
        )

        private enum class Tool {
            Tool1, Tool2, Tool3, Tool4;

            val key = "MsdTool${ordinal + 1}".intern()

            fun stack(stack: ItemStack) =
                this to stack.copy().apply { data()[key] = true }

            companion object {
                fun from(stack: ItemStack): Tool? = stack.nbt?.let(::from)
                fun from(nbt: NbtCompound): Tool? {
                    if (nbt.contains(Tool1.key)) return Tool1
                    if (nbt.contains(Tool2.key)) return Tool2
                    if (nbt.contains(Tool3.key)) return Tool3
                    if (nbt.contains(Tool4.key)) return Tool4
                    return null
                }
            }
        }

        private val toolStackMaps: Map<GameTeam, Map<Tool, ItemStack>> = enumMapOf(
            GameTeam.GHOST to enumMapOf(
                Tool.Tool1.stack(GameItems.toolSkull),
                Tool.Tool2.stack(GameItems.toolSkull),
                Tool.Tool3.stack(GameItems.toolSkull),
                Tool.Tool4.stack(GameItems.toolSkull),
            ),
            GameTeam.PLAYER_WARDEN to enumMapOf(
                Tool.Tool1.stack(GameItems.toolAxe),
                Tool.Tool2.stack(GameItems.toolShovel),
                Tool.Tool4.stack(GameItems.toolBow),
                Tool.Tool3.stack(GameItems.toolCrossbow),
            ),
            GameTeam.PLAYER_DUEL to enumMapOf(
                Tool.Tool1.stack(GameItems.toolSword),
                Tool.Tool2.stack(GameItems.toolAxe),
                Tool.Tool3.stack(GameItems.toolBow),
                Tool.Tool4.stack(GameItems.toolCrossbow),
            ),
            GameTeam.PLAYER_BLACK to enumMapOf(
                Tool.Tool3.stack(GameItems.toolBow),
                Tool.Tool1.stack(GameItems.toolAxe),
                Tool.Tool4.stack(GameItems.toolTrident),
                Tool.Tool2.stack(GameItems.toolShovel),
            ),
            GameTeam.PLAYER_YELLOW to enumMapOf(
                Tool.Tool1.stack(GameItems.toolYellowBow),
                Tool.Tool4.stack(GameItems.toolYellowSword),
                Tool.Tool2.stack(GameItems.toolYellowConduit),
                Tool.Tool3.stack(GameItems.toolShovel),
            ),
            GameTeam.PLAYER_BLUE to enumMapOf(
                Tool.Tool1.stack(GameItems.toolBlueCrossbow),
                Tool.Tool4.stack(GameItems.toolAxe),
                Tool.Tool2.stack(GameItems.toolBlueConduit),
                Tool.Tool3.stack(GameItems.toolBluePickaxe),
            ),
        )

    }

}
