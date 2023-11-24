package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.DuelingGameState
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import net.minecraft.block.Blocks
import net.minecraft.block.RespawnAnchorBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import java.util.*
import kotlin.random.Random

class SoulService : Service() {

    private enum class SoulKind {
        YELLOW {
            override val team get() = GameTeam.YELLOW
        },
        BLUE {
            override val team get() = GameTeam.BLUE
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

        val player get() = context.getPlayer(uuid) ?: context.playerHerobrine

        val displayName: Text get() = text("Soul of", player.name) * kind.team

        fun toNbt() = nbtCompoundOf(
            "MsdSoul" to true,
            "MsdSoulKind" to kind,
            "MsdSoulPlayer" to uuid,
            "MsdGlowing" to true,
        )

        fun toSoulStack(): ItemStack {
            val item = when (kind) {
                SoulKind.YELLOW -> Items.LANTERN
                SoulKind.BLUE -> Items.SOUL_LANTERN
            }
            return stackOf(item, toNbt(), displayName, listOf(
                text("can be used to enact a ") + text("pure summon").bold(),
                text("will become a random item in the loot pool if left in a chest between rounds!"),
            ))
        }

        fun toPotionStack(): ItemStack {
            return stackOf(
                Items.POTION, nbtCompoundOf("Potion" to identifier("healing")),
                text("Blood of", player.name).red(),
                text("used to be the soul of someone who was lost..."),
                text("tastes really good!"),
            )
        }
    }

    companion object {
        fun containsSoulNbt(stack: ItemStack): Boolean {
            val nbt = stack.nbt
            return nbt != null && nbt.contains("MsdSoul")
        }
    }

    private fun createSoulFor(player: GamePlayer, team: GameTeam = player.team): Soul {
        val kind = when (team) {
            GameTeam.YELLOW -> SoulKind.YELLOW
            GameTeam.BLUE -> SoulKind.BLUE
            else -> if (Random.nextBoolean()) SoulKind.YELLOW else SoulKind.BLUE
        }
        return Soul(kind, player.uuid)
    }
    private fun createSoulFrom(stack: ItemStack): Soul? {
        return if (containsSoulNbt(stack)) Soul(stack.nbt!!) else null
    }

    override fun update() {
        for ((player, playerEntity) in playerEntitiesNormal) {
            val inventory = playerEntity.inventory.asList()
            for ((i, stack) in inventory.withIndex()) {
                val soul = createSoulFrom(stack) ?: continue
                if (soul.uuid == player.uuid || soul.player.team === GameTeam.NONE) {
                    inventory[i] = soul.toPotionStack().also { it.count = stack.count }
                }
            }
        }
    }

    fun handleDeath(player: GamePlayer, playerEntity: ServerPlayerEntity) {
        if (player.team === GameTeam.YELLOW || player.team === GameTeam.BLUE) {
            if (!Rules.soulsDroppingEnabled) return
            playerEntity.dropItem(createSoulFor(player).toSoulStack(), true, false)
        }
    }

    fun handleRoundEnd() {
        for ((player, playerEntity) in playerEntitiesNormal) {
            if (
                (player.team === GameTeam.YELLOW && Rules.soulsGiveYellowOwnSoulEnabled) ||
                (player.team === GameTeam.BLUE && Rules.soulsGiveBlueOwnSoulEnabled)
            ) {
                if (Rules.soulsGiveHerobrinesSoulEnabled) {
                    playerEntity.give(createSoulFor(context.playerHerobrine, player.team).toSoulStack())
                } else {
                    playerEntity.give(createSoulFor(player).toSoulStack())
                }
            }
        }
    }

    /*
    fun handleSoulConsume(player: GamePlayer, playerEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        if (player.team === GameTeam.PLAYER_YELLOW && Rules.soulsConsumingEnabled) {
            playerEntity.addEffect(StatusEffects.JUMP_BOOST, Rules.soulsConsumingEffectDuration, Rules.soulsConsumingEffectJumpStrength)
            playerEntity.addEffect(StatusEffects.SPEED, Rules.soulsConsumingEffectDuration, Rules.soulsConsumingEffectSpeedStrength)
            playerEntity.particles(ParticleTypes.FLASH)
            playerEntity.play(SoundEvents.ENTITY_GENERIC_EAT)
            stack.decrement(1)
            return ActionResult.SUCCESS
        }
        return ActionResult.FAIL
    }
    */

    fun executeSoulGive(console: Console, players: List<GamePlayer>, soulPlayer: GamePlayer, soulTeam: GameTeam = soulPlayer.team) {
        val soul = createSoulFor(soulPlayer, soulTeam)
        val stack = soul.toSoulStack()
        players.forEach { it.entity?.give(stack.copy()) }
        console.sendInfo("Gave", soul, "to ${players.size} player(s)")
    }

    fun executeSoulClear(console: Console, players: List<GamePlayer>) {
        for (player in players) {
            val stacks = player.inventory?.asList() ?: continue
            for (i in stacks.indices) if (containsSoulNbt(stacks[i])) stacks[i] = stackOf()
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
            text("[START]").green().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel start exact ${aggressor.name} ${victim.name}")) },
            text("[CANCEL]").red().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel cancel exact ${aggressor.name} ${victim.name}")) },
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

        context.automationService.handleDuelPrepare()

        aggressor.team = GameTeam.DUELIST
        aggressor.isAlive = true
        victim.team = GameTeam.DUELIST
        victim.isAlive = true

        Async.background().withTimeout(0.1).go {
            while (true) {
                aggressor.teleport(properties.positionDuel1)
                victim.teleport(properties.positionDuel2)
                delay()
            }
        }

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
            aggressorEntity.give(createSoulFor(duel.victim).toSoulStack())
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

    private inner class DuelScreenHandlerFactory : DynamicScreenHandlerFactory<DuelScreenHandler>() {
        override val name get() = text("baby fight")
        override fun construct(sync: Int, playerInventory: PlayerInventory) = DuelScreenHandler(sync, playerInventory)
    }

    private inner class DuelScreenHandler(sync: Int, playerInventory: PlayerInventory)
        : DynamicScreenHandler(ScreenHandlerType.GRINDSTONE, sync, playerInventory)
    {
        override val inventory: Inventory

        init {
            inventory = Inventories.create(3)
            inventory.setStack(1, stackOf(
                Items.NETHERITE_SWORD,
                nbtCompoundOf("MsdIllegal" to true),
            ))

            addSlot(object : InputSlot(0, 49, 19) {
                override fun canInsert(stack: ItemStack) = containsSoulNbt(stack)
                override fun canTakeItems(playerEntity: PlayerEntity) = true
            })
            addSlot(object : InputSlot(1, 49, 40) {
                override fun canInsert(stack: ItemStack) = false
                override fun canTakeItems(playerEntity: PlayerEntity) = false
            })
            addSlot(OutputSlot(2, 129, 34))
            addPlayerInventorySlots()
        }

        override fun handleTakeResult(stack: ItemStack) {
            val soul = createSoulFrom(stack) ?: return
            val player = context.getPlayer(playerEntity)

            stack.count = 0
            inventory.removeStack(0, 1)

            val duel = Duel(player, soul.player)
            duels.add(duel)

            Game.CONSOLE_OPERATORS.sendInfo("Duel submitted:", duel.message())
        }

        override fun handleUpdateResult() {
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

        override fun handleClosed() {
            playerEntity.give(inventory.removeStack(0))
        }
    }

}
