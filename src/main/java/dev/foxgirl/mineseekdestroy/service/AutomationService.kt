package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameContext
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.Scheduler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtString
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class AutomationService : Service() {

    private class Record(player: GamePlayer) {
        val kills = player.kills
        val deaths = player.deaths
    }

    private var records = mapOf<GamePlayer, Record>()

    fun handleRoundBegin() {
        records = players.associateWith(::Record)
    }

    fun handleRoundEnd(teamLosers: GameTeam) {
        if (!game.getRuleBoolean(Game.RULE_AUTOMATION_ENABLED)) return

        val tasks = mutableListOf<() -> Unit>()

        val players = players

        for (player in players) {
            val record = records[player] ?: continue
            if (player.team == GameTeam.PLAYER_BLACK) {
                if (player.kills > record.kills) {
                    tasks.add { player.team = GameTeam.SKIP }
                } else {
                    tasks.add {
                        player.team = GameTeam.NONE
                        game.sendInfo(
                            Text.literal(player.name).formatted(Formatting.DARK_RED),
                            Text.literal("has been removed from the game!").formatted(Formatting.RED),
                        )
                    }
                }
            }
        }

        for (player in players) {
            if (player.team == teamLosers) {
                tasks.add { player.team = GameTeam.PLAYER_BLACK }
            }
        }

        val iterator = tasks.iterator()

        val secondsDelay = game.getRuleDouble(Game.RULE_AUTOMATION_DELAY_DURATION)
        val secondsInterval = game.getRuleDouble(Game.RULE_AUTOMATION_INTERVAL_DURATION)

        Scheduler.delay(secondsDelay) {
            players.forEach { it.isAlive = true }

            Scheduler.interval(secondsInterval) { schedule ->
                if (iterator.hasNext()) {
                    iterator.next()()
                } else {
                    schedule.cancel()
                }
            }
        }
    }

    fun executeOpenIpad() {
        val buttonState = ButtonState()
        val buttonOperators = ButtonInventoryPart(mappingButtons, buttonState, true)
        val buttonPlayers = ButtonInventoryPart(mappingButtons, buttonState, false)

        val targetYellow = TargetInventoryPart(mappingTargetYellow, buttonState)
        val targetBlue = TargetInventoryPart(mappingTargetBlue, buttonState)
        val targetSkip = TargetInventoryPart(mappingTargetSkip, buttonState)

        val players = players.filter { it.team == GameTeam.PLAYER_YELLOW || it.team == GameTeam.PLAYER_BLUE || it.team == GameTeam.SKIP }

        val listing = ListingInventoryPart(mappingListing, players)

        val parts = listOf(
            backgroundYellow,
            backgroundBlue,
            backgroundSkip,
            targetYellow,
            targetBlue,
            targetSkip,
            listing,
        )

        val inventoryOperators = LiveInventory(parts + listOf(buttonOperators))
        val inventoryPlayers = LiveInventory(parts + listOf(buttonPlayers))
        val inventoryView = ViewInventory(inventoryPlayers)

        val factoryOperators = IpadNamedScreenHandlerFactory(inventoryOperators)
        val factoryPlayers = IpadNamedScreenHandlerFactory(inventoryPlayers)
        val factoryView = IpadNamedScreenHandlerFactory(inventoryView)

        var commitSchedule: Scheduler.Schedule? = null
        fun commit(): Boolean {
            commitSchedule = null
            if (buttonState.ready()) {
                context.playerManager.playerList.forEach { it.closeHandledScreen() }
                players.forEach { it.team = GameTeam.SKIP }
                targetYellow.commit(context, GameTeam.PLAYER_YELLOW)
                targetBlue.commit(context, GameTeam.PLAYER_BLUE)
                targetSkip.commit(context, GameTeam.SKIP)
                return true
            } else {
                return false
            }
        }

        Scheduler.interval(1.0) { schedule ->
            if (commitSchedule == null && buttonState.ready()) {
                commitSchedule = Scheduler.delay(3.0) { if (commit()) schedule.cancel() }
            }
            for (player in players) {
                val entity = player.entity ?: continue
                if (entity.currentScreenHandler === null) continue
                if (entity.currentScreenHandler === entity.playerScreenHandler) continue
                if (player.isOperator) {
                    entity.openHandledScreen(factoryOperators)
                } else if (player.isOnScoreboard && player.team != GameTeam.PLAYER_BLACK) {
                    entity.openHandledScreen(factoryPlayers)
                } else {
                    entity.openHandledScreen(factoryView)
                }
            }
        }
    }

    private companion object {

        private val empty = ItemStack.EMPTY!!

        private class IpadNamedScreenHandlerFactory(private val inventory: Inventory) : NamedScreenHandlerFactory {
            override fun getDisplayName(): Text = Text.of("child's ipad")
            override fun createMenu(syncId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity?): ScreenHandler {
                return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, inventory)
            }
        }

        private class LiveInventory(parts: Iterable<InventoryPart>) : Inventory {
            private val mappings = arrayOfNulls<InventoryPart>(size())
            init {
                parts.forEach { part -> part.mapping.forEach { slot -> mappings[slot] = part } }
            }

            private inline fun mapped(slot: Int, block: (index: Int, part: InventoryPart) -> Unit) {
                val part = mappings[slot]
                if (part != null) {
                    val index = part.mapping.indexOfFirst { it == slot }
                    if (index > -1) block(index, part)
                }
            }

            override fun size() = 54
            override fun isEmpty() = false

            override fun setStack(slot: Int, stack: ItemStack?) {
                mapped(slot) { index, part -> part.set(index, stack ?: empty) }
            }

            override fun removeStack(slot: Int, amount: Int) = removeStack(slot)
            override fun removeStack(slot: Int): ItemStack {
                mapped(slot) { index, part -> return part.set(index, empty) }
                return empty
            }

            override fun getStack(slot: Int): ItemStack {
                mapped(slot) { index, part -> return part.get(index) }
                return empty
            }

            override fun clear() {}
            override fun markDirty() {}
            override fun canPlayerUse(player: PlayerEntity?) = true
        }

        private class ViewInventory(private val inventory: Inventory) : Inventory by inventory {
            override fun setStack(slot: Int, stack: ItemStack?) {}
            override fun removeStack(slot: Int, amount: Int) = empty
            override fun removeStack(slot: Int) = empty
        }

        private abstract class InventoryPart(val mapping: IntArray) {
            abstract fun get(index: Int): ItemStack
            abstract fun set(index: Int, stack: ItemStack): ItemStack
        }

        private class BackgroundInventoryPart(mapping: IntArray, private val stack: ItemStack) : InventoryPart(mapping) {
            override fun get(index: Int) = stack.copy()
            override fun set(index: Int, stack: ItemStack) = empty
        }

        private class ListingInventoryPart(mapping: IntArray, players: List<GamePlayer>) : InventoryPart(mapping) {
            private val slots = arrayOfNulls<ItemStack>(mapping.size)
            init {
                for (i in 0 until Math.min(players.lastIndex, slots.lastIndex)) {
                    val nbt = NbtCompound().also { it.put("SkullOwner", NbtString.of(players[i].name)) }
                    val stack = ItemStack(Items.PLAYER_HEAD).also { it.nbt = nbt }
                    slots[i] = stack
                }
            }

            override fun get(index: Int) = slots[index]?.copy() ?: empty
            override fun set(index: Int, stack: ItemStack) = get(index)
        }

        private class TargetInventoryPart(mapping: IntArray, private val state: ButtonState) : InventoryPart(mapping) {
            private val slots = arrayOfNulls<ItemStack>(mapping.size)

            override fun get(index: Int) = slots[index] ?: empty
            override fun set(index: Int, stack: ItemStack): ItemStack {
                return if (state.open()) {
                    get(index).also { slots[index] = if (stack.item === Items.PLAYER_HEAD) stack else null }
                } else {
                    empty
                }
            }

            fun commit(context: GameContext, team: GameTeam) {
                for (stack in slots) {
                    if (stack == null) continue
                    if (stack.item !== Items.PLAYER_HEAD) continue
                    val element = stack.nbt?.get("SkullOwner") ?: continue
                    val name: String = when (element) {
                        is NbtString -> element.asString()
                        is NbtCompound -> element.getString("Name")
                        else -> continue
                    }
                    val player = context.getPlayer(name)
                    if (player != null) {
                        player.team = team
                    }
                }
            }
        }

        private class ButtonInventoryPart(
            mapping: IntArray,
            private val state: ButtonState,
            private val mutable: Boolean,
        ) : InventoryPart(mapping) {
            override fun get(index: Int) = (if (state.flags[index]) itemLocked else itemOpen).copy()
            override fun set(index: Int, stack: ItemStack): ItemStack {
                if (mutable) state.flags[index] = !state.flags[index]
                return empty
            }
        }

        private class ButtonState {
            val flags = BooleanArray(2)
            fun ready() = flags.all { it }
            fun open() = flags.none { it }
        }

        private val itemOpen = ItemStack(Items.LIME_CONCRETE).setCustomName(Text.of("OPEN"))
        private val itemLocked = ItemStack(Items.RED_CONCRETE).setCustomName(Text.of("LOCKED IN"))

        private val backgroundYellow: BackgroundInventoryPart
        private val backgroundBlue: BackgroundInventoryPart
        private val backgroundSkip: BackgroundInventoryPart

        private val mappingTargetYellow: IntArray
        private val mappingTargetBlue: IntArray
        private val mappingTargetSkip: IntArray
        private val mappingListing: IntArray
        private val mappingButtons: IntArray

        init {
            val itemYellow = ItemStack(Items.YELLOW_STAINED_GLASS_PANE).setCustomName(Text.of("TEAM YELLOW"))
            val itemBlue = ItemStack(Items.BLUE_STAINED_GLASS_PANE).setCustomName(Text.of("TEAM BLUE"))
            val itemSkip = ItemStack(Items.LIME_STAINED_GLASS_PANE).setCustomName(Text.of("SKIP!"))

            val mappingYellow = buildList {
                addAll(9..11)
                addAll(45..47)
            }.toIntArray()
            val mappingBlue = buildList {
                addAll(15..17)
                addAll(51..53)
            }.toIntArray()
            val mappingSkip = buildList {
                addAll(12..14)
                addAll(21..23)
                add(30)
                add(32)
                addAll(39..41)
                addAll(48..50)
            }.toIntArray()

            backgroundYellow = BackgroundInventoryPart(mappingYellow, itemYellow)
            backgroundBlue = BackgroundInventoryPart(mappingBlue, itemBlue)
            backgroundSkip = BackgroundInventoryPart(mappingSkip, itemSkip)

            mappingTargetYellow = buildList {
                addAll(18..20)
                addAll(27..29)
                addAll(36..38)
            }.toIntArray()
            mappingTargetBlue = buildList {
                addAll(24..26)
                addAll(33..35)
                addAll(42..44)
            }.toIntArray()
            mappingTargetSkip = intArrayOf(31)
            mappingListing = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            mappingButtons = intArrayOf(28, 34)
        }

    }

}
