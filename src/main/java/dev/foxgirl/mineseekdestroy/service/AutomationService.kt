package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameContext
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
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
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.world.GameMode
import java.util.concurrent.atomic.AtomicBoolean

class AutomationService : Service() {

    private class Record(player: GamePlayer) {
        val team = player.team
        val kills = player.kills
        val deaths = player.deaths
    }

    private var records = mapOf<GamePlayer, Record>()

    private fun recordsCreate() {
        records = players.associateWith(::Record)
    }
    private fun recordsTake(): Map<GamePlayer, Record> {
        return records.also { records = emptyMap() }
    }

    fun handleRoundBegin() {
        recordsCreate()
    }

    fun handleRoundEnd(losingTeam: GameTeam) {
        val records = recordsTake()

        if (!Rules.automationEnabled) return

        val tasks = mutableListOf<() -> Unit>()

        val players = players

        val teamSkip = GameTeam.SKIP
        val teamRemoved =
            if (Rules.automationGhostsEnabled) GameTeam.GHOST else GameTeam.NONE

        for (player in players) {
            val record = records[player] ?: continue
            if (player.team == GameTeam.PLAYER_BLACK) {
                if (player.kills > record.kills) {
                    tasks.add {
                        player.team = teamSkip
                        logger.info("Automation assigned " + player.name + " to skip")
                    }
                } else {
                    tasks.add {
                        player.team = teamRemoved
                        game.sendInfo(
                            text(player).darkRed(),
                            text("has been removed from the game!").red(),
                        )
                        logger.info("Automation removed " + player.name + " from the game")
                    }
                }
            }
        }

        for (player in players) {
            if (player.team == losingTeam) {
                tasks.add {
                    player.team = GameTeam.PLAYER_BLACK
                    logger.info("Automation assigned " + player.name + " to black")
                }
            }
        }

        val secondsDelay = Rules.automationDelayDuration
        val secondsInterval = Rules.automationIntervalDuration

        Async.go {
            delay(secondsDelay)

            players.forEach { it.isAlive = true }
            logger.info("Automation marked all players alive")

            for (task in tasks) {
                delay(secondsInterval)
                task()
            }
            logger.info("Automation completed all tasks")

            delay()
            context.specialBuddyService.handleAutomationRoundEndCompleted()
        }
    }

    fun handleDuelPrepare() {
        recordsCreate()
    }

    fun handleDuelEnd(winningPlayers: List<GamePlayer>) {
        val records = recordsTake()

        if (!Rules.automationEnabled) return

        val players = players.filter { it.team == GameTeam.PLAYER_DUEL }

        val team = players
            .map { player -> records[player]?.team ?: GameTeam.PLAYER_BLACK }
            .find { team -> team !== GameTeam.PLAYER_BLACK } ?: GameTeam.SKIP

        for (player in players) {
            player.isAlive = true
            player.team = GameTeam.PLAYER_BLACK
        }
        for (player in winningPlayers) {
            player.team = team
        }
    }

    private val ipadCooldownFlag = AtomicBoolean(false)

    fun executeOpenIpad(console: Console) {
        if (players.none { it.isOperator }) {
            console.sendError("Cannot open iPad, no operators available")
            return
        }

        if (ipadCooldownFlag.getAndSet(true)) {
            console.sendError("Cannot open iPad, already in progress")
            return
        }
        try {
            Scheduler.delay(5.0) { ipadCooldownFlag.set(false) }
        } catch (cause: Exception) {
            ipadCooldownFlag.set(false)
            throw cause
        }

        console.sendInfo("Opening iPad")

        players.forEach { if (it.isOperator) it.entity?.changeGameMode(GameMode.CREATIVE) }

        val buttonState = ButtonState()
        val buttonOperators = ButtonInventoryPart(mappingButtons, buttonState, true)
        val buttonPlayers = ButtonInventoryPart(mappingButtons, buttonState, false)

        val targetYellow = TargetInventoryPart(mappingTargetYellow)
        val targetBlue = TargetInventoryPart(mappingTargetBlue)
        val targetSkip = TargetInventoryPart(mappingTargetSkip)

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

        val inventoryLiveOperators = LiveInventory(parts + buttonOperators)
        val inventoryLivePlayers = LiveInventory(parts + buttonPlayers)

        val inventoryPlayers = ViewInventory(inventoryLivePlayers, buttonState::open)
        val inventoryView = ViewInventory(inventoryLivePlayers)

        val factoryOperators = IpadNamedScreenHandlerFactory(inventoryLiveOperators)
        val factoryPlayers = IpadNamedScreenHandlerFactory(inventoryPlayers)
        val factoryView = IpadNamedScreenHandlerFactory(inventoryView)

        val schedule = Scheduler.interval(1.0) {
            for (player in context.players) {
                val entity = player.entity ?: continue

                if (
                    entity.currentScreenHandler !== null &&
                    entity.currentScreenHandler !== entity.playerScreenHandler
                ) continue

                if (player.isOperator) {
                    entity.openHandledScreen(factoryOperators)
                } else if (player.isOnScoreboard && player.team != GameTeam.PLAYER_BLACK) {
                    entity.openHandledScreen(factoryPlayers)
                } else {
                    entity.openHandledScreen(factoryView)
                }
            }
        }

        Async.go {
            while (true) {
                delay(1.0)
                if (!buttonState.ready()) continue
                delay(1.0)
                if (!buttonState.ready()) continue
                break
            }

            schedule.cancel()

            delay(0.1)
            context.playerManager.playerList.forEach { it.closeHandledScreen() }
            delay(0.1)
            context.playerManager.playerList.forEach { it.closeHandledScreen() }

            logger.info("Automation iPad commit started, assigning selected players to skip")
            players.forEach { it.team = GameTeam.SKIP }

            targetYellow.commit(context, GameTeam.PLAYER_YELLOW)
            targetBlue.commit(context, GameTeam.PLAYER_BLUE)
            targetSkip.commit(context, GameTeam.SKIP)

            Broadcast.sendSoundPing()
        }
    }

    private companion object {

        private class IpadNamedScreenHandlerFactory(private val inventory: Inventory) : NamedScreenHandlerFactory {
            override fun getDisplayName(): Text = text("child's ipad")
            override fun createMenu(sync: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity?): ScreenHandler {
                return IpadScreenHandler(sync, playerInventory, inventory)
            }
        }

        private class IpadScreenHandler(
            sync: Int,
            playerInventory: PlayerInventory,
            containerInventory: Inventory,
        ) : GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, sync, playerInventory, containerInventory, 6)

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

            override fun getStack(slot: Int): ItemStack {
                mapped(slot) { index, part -> return part.get(index) }
                return stackOf()
            }
            override fun setStack(slot: Int, stack: ItemStack?) {
                mapped(slot) { index, part -> part.set(index, stack ?: stackOf()) }
            }
            override fun removeStack(slot: Int, amount: Int) = removeStack(slot)
            override fun removeStack(slot: Int): ItemStack {
                mapped(slot) { index, part -> return part.set(index, stackOf()) }
                return stackOf()
            }

            override fun clear() {}
            override fun markDirty() {}
            override fun canPlayerUse(player: PlayerEntity?) = true
        }

        private class ViewInventory(private val inventory: Inventory, private val mutable: () -> Boolean = { false }) : Inventory by inventory {
            override fun getStack(slot: Int): ItemStack {
                val stack = inventory.getStack(slot)
                return if (mutable()) stack else stack.copy()
            }
            override fun setStack(slot: Int, stack: ItemStack?) {
                if (mutable()) inventory.setStack(slot, stack)
            }
            override fun removeStack(slot: Int, amount: Int) =
                if (mutable()) inventory.removeStack(slot, amount) else stackOf()
            override fun removeStack(slot: Int) =
                if (mutable()) inventory.removeStack(slot) else stackOf()
        }

        private abstract class InventoryPart(@JvmField val mapping: IntArray) {
            abstract fun get(index: Int): ItemStack
            abstract fun set(index: Int, stack: ItemStack): ItemStack
        }

        private class BackgroundInventoryPart(mapping: IntArray, private val stack: ItemStack) :
            InventoryPart(mapping) {
            override fun get(index: Int) = stackOf(stack)
            override fun set(index: Int, stack: ItemStack) = stackOf()
        }

        private class ListingInventoryPart(mapping: IntArray, players: List<GamePlayer>) : InventoryPart(mapping) {
            private val slots = arrayOfNulls<ItemStack>(mapping.size)

            init {
                for (i in 0 until Math.min(mapping.size, players.size)) {
                    slots[i] = stackOf(
                        Items.PLAYER_HEAD, nbtCompoundOf(
                            "SkullOwner" to players[i].name,
                            "MsdIllegal" to true,
                        )
                    )
                }
            }

            override fun get(index: Int) = slots[index]?.copy() ?: stackOf()
            override fun set(index: Int, stack: ItemStack) = get(index)
        }

        private class TargetInventoryPart(mapping: IntArray) : InventoryPart(mapping) {
            private val slots = arrayOfNulls<ItemStack>(mapping.size)

            override fun get(index: Int): ItemStack {
                return slots[index] ?: stackOf()
            }
            override fun set(index: Int, stack: ItemStack): ItemStack {
                val stackNew = if (stack.item === Items.PLAYER_HEAD) stack.copy() else null
                val stackOld = slots[index] ?: stackOf()
                slots[index] = stackNew
                return stackOld
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
                        Game.LOGGER.info("Automation iPad assigned " + player.name + " to team " + team)
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
                return stackOf()
            }
        }

        private class ButtonState {
            val flags = BooleanArray(2)
            fun ready() = flags.all { it }
            fun open() = flags.none { it }
        }

        private val itemLocked = stackOf(Items.RED_CONCRETE, nbtCompoundOf("MsdIllegal" to true), text("LOCKED IN").red())
        private val itemOpen = stackOf(Items.LIME_CONCRETE, nbtCompoundOf("MsdIllegal" to true), text("OPEN").green())

        private val backgroundYellow: BackgroundInventoryPart
        private val backgroundBlue: BackgroundInventoryPart
        private val backgroundSkip: BackgroundInventoryPart

        private val mappingTargetYellow: IntArray
        private val mappingTargetBlue: IntArray
        private val mappingTargetSkip: IntArray
        private val mappingButtons: IntArray
        private val mappingListing: IntArray

        init {
            val itemYellow = stackOf(Items.YELLOW_STAINED_GLASS_PANE, nbtCompoundOf("MsdIllegal" to true), text("TEAM YELLOW").teamYellow())
            val itemBlue = stackOf(Items.BLUE_STAINED_GLASS_PANE, nbtCompoundOf("MsdIllegal" to true), text("TEAM BLUE").teamBlue())
            val itemSkip = stackOf(Items.LIME_STAINED_GLASS_PANE, nbtCompoundOf("MsdIllegal" to true), text("SKIP!").green())

            val mappingYellow = intArrayOf(18, 20, 21)
            val mappingBlue = intArrayOf(23, 24, 26)
            val mappingSkip = buildList {
                add(22)
                addAll(30..32)
                add(39)
                add(41)
                addAll(48..50)
            }.toIntArray()

            backgroundYellow = BackgroundInventoryPart(mappingYellow, itemYellow)
            backgroundBlue = BackgroundInventoryPart(mappingBlue, itemBlue)
            backgroundSkip = BackgroundInventoryPart(mappingSkip, itemSkip)

            mappingTargetYellow = buildList {
                addAll(27..29)
                addAll(36..38)
                addAll(45..47)
            }.toIntArray()
            mappingTargetBlue = buildList {
                addAll(33..35)
                addAll(42..44)
                addAll(51..53)
            }.toIntArray()
            mappingTargetSkip = intArrayOf(40)
            mappingButtons = intArrayOf(19, 25)
            mappingListing = (0..17).toList().toIntArray()
        }

    }

}
