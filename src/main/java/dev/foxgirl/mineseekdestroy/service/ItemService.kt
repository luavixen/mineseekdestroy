package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.RunningGameState
import dev.foxgirl.mineseekdestroy.util.collect.immutableMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.enchantment.Enchantments
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.nbt.NbtByte
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

    override fun update() {
        val running = state is RunningGameState

        for (player in players) {
            if (running && player.isSpectator) continue

            val inventory = player.inventory ?: continue

            val toolStacks = toolStackMaps[player.team]

            val powderItem = when (player.team) {
                GameTeam.PLAYER_DUEL -> BROWN_CONCRETE_POWDER
                GameTeam.PLAYER_WARDEN -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_BLACK -> BLACK_CONCRETE_POWDER
                GameTeam.PLAYER_YELLOW -> YELLOW_CONCRETE_POWDER
                GameTeam.PLAYER_BLUE -> BLUE_CONCRETE_POWDER
                else -> continue
            }

            inventory.forEach { stack, item, i ->
                if (toolStacks != null) {
                    val tool = Tool.from(stack)
                    if (tool != null) {
                        val toolStack = toolStacks[tool]!!
                        if (!ItemStack.areItemsEqual(toolStack, stack)) inventory.setStack(i, toolStack.copy())
                    }
                }
                if (powderItems.contains(item) && item !== powderItem) {
                    val count = stack.count
                    inventory.setStack(i, ItemStack.EMPTY)
                    inventory.insertStack(ItemStack(powderItem, count))
                }
                if (illegalItems.contains(item)) {
                    inventory.setStack(i, ItemStack.EMPTY)
                }
                val nbt: NbtCompound? = stack.nbt
                if (nbt != null && nbt.contains("MsdIllegal")) {
                    inventory.setStack(i, ItemStack.EMPTY)
                }
            }
        }
    }

    private companion object {

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

        private enum class Tool {
            Tool1, Tool2, Tool3, Tool4;

            val key = "MsdTool${ordinal + 1}".intern()

            fun stack(item: Item) = stack(ItemStack(item))
            fun stack(stack: ItemStack): Pair<Tool, ItemStack> =
                this to stack.also { it.getOrCreateNbt().put(key, NbtByte.ONE) }

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

        private val toolStackMaps: Map<GameTeam, Map<Tool, ItemStack>> = immutableMapOf(
            GameTeam.PLAYER_DUEL to immutableMapOf(
                Tool.Tool1.stack(IRON_SWORD),
                Tool.Tool2.stack(IRON_AXE),
                Tool.Tool3.stack(CROSSBOW),
                Tool.Tool4.stack(BOW),
            ),
            GameTeam.PLAYER_BLACK to immutableMapOf(
                Tool.Tool1.stack(IRON_AXE),
                Tool.Tool2.stack(IRON_PICKAXE),
                Tool.Tool3.stack(BOW),
                Tool.Tool4.stack(TRIDENT)
                    .also { (_, stack) -> stack.addEnchantment(Enchantments.LOYALTY, 3) },
            ),
            GameTeam.PLAYER_YELLOW to immutableMapOf(
                Tool.Tool1.stack(IRON_AXE),
                Tool.Tool2.stack(IRON_SHOVEL),
                Tool.Tool3.stack(CROSSBOW),
                Tool.Tool4.stack(CROSSBOW),
            ),
            GameTeam.PLAYER_BLUE to immutableMapOf(
                Tool.Tool1.stack(IRON_SWORD),
                Tool.Tool2.stack(IRON_PICKAXE),
                Tool.Tool3.stack(BOW),
                Tool.Tool4.stack(BOW),
            ),
        )

    }

}
