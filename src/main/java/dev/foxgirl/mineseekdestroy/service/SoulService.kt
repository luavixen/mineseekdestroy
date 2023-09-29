package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.DuelingGameState
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.block.Blocks
import net.minecraft.block.RespawnAnchorBlock
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import java.util.*
import kotlin.random.Random

class SoulService : Service() {

    private enum class SoulKind {
        YELLOW {
            override val team get() = GameTeam.PLAYER_YELLOW
        },
        BLUE {
            override val team get() = GameTeam.PLAYER_BLUE
        };

        abstract val team: GameTeam
    }

    private inner class Soul {
        val kind: SoulKind
        val uuid: UUID

        constructor(kind: SoulKind, uuid: UUID) {
            this.kind = kind
            this.uuid = uuid
        }

        constructor(nbt: NbtCompound) {
            kind = nbt["MsdSoulKind"].toEnum()
            uuid = nbt["MsdSoulPlayer"].toUUID()
        }

        val player get() = context.getPlayer(uuid)!!

        val displayName: Text get() = text("Soul of", player.name) * kind.team

        fun toNbt() = nbtCompoundOf(
            "MsdSoul" to true,
            "MsdSoulKind" to kind,
            "MsdSoulPlayer" to uuid,
            "MsdGlowing" to true,
        )

        fun toStack(): ItemStack {
            val item = when (kind) {
                SoulKind.YELLOW -> Items.LANTERN
                SoulKind.BLUE -> Items.SOUL_LANTERN
            }
            val lore = when (kind) {
                SoulKind.YELLOW -> listOf(
                    text("can be used by a ") + text("blue").teamBlue() + " player to enact a " + text("pure summon").bold(),
                    text("can be eaten by a ") + text("yellow").teamYellow() + " player for " + text("temp. jump & speed boost").bold(),
                    text("will become a random item in the loot pool if left in a chest between rounds!"),
                )
                SoulKind.BLUE -> listOf(
                    text("can be used by a ") + text("yellow").teamYellow() + " player to enact a " + text("pure summon").bold(),
                    text("can be eaten by a ") + text("yellow").teamYellow() + " player for " + text("temp. jump & speed boost").bold(),
                    text("will become a random item in the loot pool if left in a chest between rounds!"),
                )
            }
            return stackOf(item, toNbt(), displayName, lore)
        }
    }

    private fun createSoulFor(player: GamePlayer, team: GameTeam = player.team): Soul {
        val kind = when (team) {
            GameTeam.PLAYER_YELLOW -> SoulKind.YELLOW
            GameTeam.PLAYER_BLUE -> SoulKind.BLUE
            else -> if (Random.nextBoolean()) SoulKind.YELLOW else SoulKind.BLUE
        }
        return Soul(kind, player.uuid)
    }

    private fun containsSoulNbt(stack: ItemStack): Boolean {
        val nbt = stack.nbt
        return nbt != null && nbt.contains("MsdSoul")
    }
    private fun createSoulFrom(stack: ItemStack): Soul? {
        return if (containsSoulNbt(stack)) Soul(stack.nbt!!) else null
    }

    fun handleDeath(player: GamePlayer, playerEntity: ServerPlayerEntity) {
        if (player.team === GameTeam.PLAYER_YELLOW || player.team === GameTeam.PLAYER_BLUE) {
            if (!Rules.soulsDroppingEnabled) return
            playerEntity.dropItem(createSoulFor(player).toStack(), true, false)
        }
    }

    fun handleSoulConsume(player: GamePlayer, playerEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        if (player.team === GameTeam.PLAYER_YELLOW && Rules.soulsConsumingEnabled) {
            val duration = (Rules.soulsConsumingEffectDuration * 20.0).toInt()
            playerEntity.addStatusEffect(StatusEffectInstance(
                StatusEffects.JUMP_BOOST, duration,
                Rules.soulsConsumingEffectJumpStrength - 1,
            ))
            playerEntity.addStatusEffect(StatusEffectInstance(
                StatusEffects.SPEED, duration,
                Rules.soulsConsumingEffectSpeedStrength - 1,
            ))
            stack.decrement(1)
            return ActionResult.SUCCESS
        }
        return ActionResult.FAIL
    }

    fun executeSoulGive(console: Console, players: List<GamePlayer>, soulPlayer: GamePlayer, soulTeam: GameTeam = soulPlayer.team) {
        val soul = createSoulFor(soulPlayer, soulTeam)
        val stack = soul.toStack()
        players.forEach { it.entity?.give(stack.copy()) }
        console.sendInfo("Gave", soul, "to ${players.size} player(s)")
    }

    fun executeSoulClear(console: Console, players: List<GamePlayer>) {
        for (player in players) {
            val stacks = player.inventory?.asList() ?: continue
            for (i in stacks.indices) if (containsSoulNbt(stacks[i])) stacks[i] = ItemStack.EMPTY
        }
        console.sendInfo("Removed souls from ${players.size} player(s)")
    }

    fun executeSoulList(console: Console, players: List<GamePlayer>) {
        console.sendInfo("Souls possessed by ${players.size} player(s):")
        for (player in players) {
            val stacks = player.inventory?.asList() ?: continue
            for (stack in stacks) {
                val soul = createSoulFrom(stack) ?: continue
                console.sendInfo("  -", player, "has ${stack.count}x", soul)
            }
        }
    }

    private data class Duel(
        val aggressor: GamePlayer,
        val victim: GamePlayer,
    ) {
        val displayName: Text get() = text(aggressor, "VS", victim)
        fun message(): Text = text(
            displayName, "-",
            text("[START]").green().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel start ${aggressor.name} ${victim.name}")) },
            text("[CANCEL]").red().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel cancel ${aggressor.name} ${victim.name}")) },
        )
    }

    private var duels = mutableSetOf<Duel>()
    private var duelsEnabled = false

    fun handleAnchorOpen(player: GamePlayer): ActionResult {
        if (duelsEnabled && !(player.isSpectator || player.isGhost)) {
            player.entity?.openHandledScreen(DuelScreenHandlerFactory())
            return ActionResult.SUCCESS
        }
        return ActionResult.FAIL
    }

    private fun updateAnchors(enabled: Boolean) {
        val queue = Editor.queue(world, properties.regionBlimp)
        if (enabled) {
            queue.edit { state, _, _, _ ->
                if (state.block === Blocks.RESPAWN_ANCHOR) state.with(RespawnAnchorBlock.CHARGES, RespawnAnchorBlock.MAX_CHARGES) else null
            }
        } else {
            queue.edit { state, _, _, _ ->
                if (state.block === Blocks.RESPAWN_ANCHOR) state.with(RespawnAnchorBlock.CHARGES, RespawnAnchorBlock.NO_CHARGES) else null
            }
        }
    }

    fun executeDuelOpen(console: Console) {
        if (duelsEnabled) {
            console.sendError("Duel submission already open")
            return
        }
        duelsEnabled = true
        updateAnchors(true)
        console.sendInfo("Duel submission opened")
    }
    fun executeDuelClose(console: Console) {
        if (!duelsEnabled) {
            console.sendError("Duel submission already closed")
            return
        }
        duelsEnabled = false
        updateAnchors(false)
        console.sendInfo("Duel submission closed")
    }

    fun executeDuelStart(console: Console, aggressor: GamePlayer, victim: GamePlayer, force: Boolean) {
        val duel = Duel(aggressor, victim)

        if (!duels.remove(duel) && !force) {
            console.sendError("Duel", duel, "is not available, cannot start")
            return
        }

        aggressor.team = GameTeam.PLAYER_DUEL
        aggressor.isAlive = true
        victim.team = GameTeam.PLAYER_DUEL
        victim.isAlive = true

        aggressor.teleport(properties.positionDuel1)
        victim.teleport(properties.positionDuel2)

        state = DuelingGameState()
        context.snapshotService.executeSnapshotSave(console)

        console.sendInfo("Duel", duel, "starting")
    }
    fun executeDuelCancel(console: Console, aggressor: GamePlayer, victim: GamePlayer) {
        val duel = Duel(aggressor, victim)

        if (!duels.remove(duel)) {
            console.sendError("Duel", duel, "is not available, cannot cancel")
            return
        }

        val aggressorEntity = duel.aggressor.entity
        if (aggressorEntity != null) {
            aggressorEntity.give(createSoulFor(duel.victim).toStack())
        } else {
            console.sendError("Duel", duel, "failed to return soul to aggressor while being cancelled")
        }

        console.sendInfo("Duel", duel, "cancelled")
    }

    fun executeDuelList(console: Console) {
        console.sendInfo("Duels available:")
        for (duel in duels) {
            console.sendInfo("  -", duel.message())
        }
    }

    private inner class DuelScreenHandlerFactory : NamedScreenHandlerFactory {
        override fun getDisplayName() = text("baby fight")
        override fun createMenu(syncId: Int, playerInventory: PlayerInventory, playerEntity: PlayerEntity): ScreenHandler {
            return DuelScreenHandler(syncId, playerInventory)
        }
    }

    private inner class DuelScreenHandler : ScreenHandler {
        private val inventory: Inventory

        constructor(syncId: Int, playerInventory: PlayerInventory) : super(ScreenHandlerType.GRINDSTONE, syncId) {
            inventory = Inventories.create(3)
            inventory.setStack(1, stackOf(
                Items.NETHERITE_SWORD,
                nbtCompoundOf("MsdIllegal" to true),
            ))

            addSlot(object : Slot(inventory, 0, 49, 19) {
                override fun canInsert(stack: ItemStack) = containsSoulNbt(stack)
                override fun canTakeItems(playerEntity: PlayerEntity) = true
                override fun markDirty() {
                    super.markDirty()
                    onUpdateResult()
                }
            })
            addSlot(object : Slot(inventory, 1, 49, 40) {
                override fun canInsert(stack: ItemStack) = false
                override fun canTakeItems(playerEntity: PlayerEntity) = false
            })
            addSlot(object : Slot(inventory, 2, 129, 34) {
                override fun canInsert(stack: ItemStack) = false
                override fun canTakeItems(playerEntity: PlayerEntity) = true
                override fun onTakeItem(playerEntity: PlayerEntity, stack: ItemStack) {
                    super.onTakeItem(playerEntity, stack)
                    onTakeResult(playerEntity, stack)
                }
            })

            for (x in 0 until 9) {
                for (y in 0 until 3) {
                    addSlot(Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18))
                }
            }
            for (x in 0 until 9) {
                addSlot(Slot(playerInventory, x, 8 + x * 18, 142))
            }
        }

        private fun onUpdateResult() {
            val soul = createSoulFrom(inventory.getStack(0))
            if (soul == null) {
                inventory.setStack(2, stackOf())
                return
            }
            val nbt = soul.toNbt() + nbtCompoundOf(
                "SkullOwner" to soul.player.name,
                "MsdIllegal" to true,
            )
            val title = text("Duel ") + soul.player.displayName + "?"
            val lore = listOf(
                text("initiating a duel spends the soul you use here!"),
                text("you can only initiate a duel ") + text("twice").bold() + " per round, against " + text("two different").bold() + " players!",
                text("the winner of a duel assumes the position of the owner of this head!"),
                text("the loser of a duel is transferred to ") + text("black team").teamBlack() + "!",
            )
            inventory.setStack(2, stackOf(Items.PLAYER_HEAD, nbt, title, lore))
        }

        private fun onTakeResult(playerEntity: PlayerEntity, stack: ItemStack) {
            val player = context.getPlayer(playerEntity) ?: return
            val soul = createSoulFrom(stack) ?: return

            stack.count = 0
            inventory.removeStack(0, 1)

            val duel = Duel(player, soul.player)
            duels.add(duel)

            Game.CONSOLE_OPERATORS.sendInfo("Duel submitted:", duel.message())
        }

        override fun onClosed(playerEntity: PlayerEntity) {
            playerEntity.give(inventory.removeStack(0))
            playerEntity.give(cursorStack)
            cursorStack = ItemStack.EMPTY
        }

        override fun quickMove(playerEntity: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY
        override fun canUse(playerEntity: PlayerEntity) = true
    }

}
