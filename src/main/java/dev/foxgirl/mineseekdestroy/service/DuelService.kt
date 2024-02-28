package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.DuelingGameState
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.terminate
import net.minecraft.block.Blocks
import net.minecraft.block.RespawnAnchorBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.ActionResult

class DuelService : Service() {

    private data class Duel(
        val attacker: GamePlayer,
        val victim: GamePlayer,
    ) {
        val id = "${attacker.uuid}/${victim.uuid}"

        val attackerDamage = attacker.givenDamage
        val victimDamage = victim.givenDamage

        val differenceScore get() = DamageService.damageToScore(attackerDamage - victimDamage)
        val attackerScore get() = DamageService.damageToScore(attackerDamage)
        val victimScore get() = DamageService.damageToScore(victimDamage)

        val displayName: Text get() = text(attacker, "VS", victim)

        fun messageTitle(): Text = text(text(attacker), "(${attackerScore})", "VS", text(victim), "(${victimScore})")
        fun messageButtons(): Text = text(
            text("[START]").green().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel start exact ${attacker.name} ${victim.name}")) },
            text("[CANCEL]").red().style { it.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/msd duel cancel exact ${attacker.name} ${victim.name}")) },
        )

        fun message(): List<Text> = listOf(
            text("  -", messageTitle()),
            text("      ↳", messageButtons()),
        )
    }

    private var duels = mutableSetOf<Duel>()
    private var duelsEnabled = false

    private fun requestDuel(playerEntity: ServerPlayerEntity, duel: Duel) {
        if (duelsEnabled) {
            if (duels.add(duel)) {
                Game.CONSOLE_OPERATORS.sendInfo("Duel submitted:", duel.messageTitle())
                Game.CONSOLE_OPERATORS.sendInfo("        ↳", duel.messageButtons())
                playerEntity.sendInfo("Duel", duel, "requested, waiting for approval!")
            } else {
                playerEntity.sendError("Duel", duel, "has already been requested!")
            }
        } else {
            playerEntity.sendError("Can't request this duel, submission is closed!")
        }
    }

    fun handleAnchorOpen(player: GamePlayer): ActionResult {
        if (duelsEnabled && (player.isPlayingOrGhost || player.isOperator)) {
            player.entity?.openHandledScreen(DuelScreenHandlerFactory())
            return ActionResult.SUCCESS
        }
        return ActionResult.FAIL
    }

    private fun setAnchorState(active: Boolean) {
        Editor.queue(world, properties.regionBlimp) { queue ->
            if (active) {
                queue.edit { state, _, _, _ ->
                    if (state.block === Blocks.RESPAWN_ANCHOR) state.with(RespawnAnchorBlock.CHARGES, RespawnAnchorBlock.MAX_CHARGES) else null
                }
            } else {
                queue.edit { state, _, _, _ ->
                    if (state.block === Blocks.RESPAWN_ANCHOR) state.with(RespawnAnchorBlock.CHARGES, RespawnAnchorBlock.NO_CHARGES) else null
                }
            }
        }
            .terminate()
    }

    fun executeDuelOpen(console: Console) {
        if (duelsEnabled) {
            console.sendError("Duel submission already open")
            return
        }
        duelsEnabled = true
        setAnchorState(true)
        console.sendInfo("Duel submission opened")
    }
    fun executeDuelClose(console: Console) {
        if (!duelsEnabled) {
            console.sendError("Duel submission already closed")
            return
        }
        duelsEnabled = false
        setAnchorState(false)
        console.sendInfo("Duel submission closed")
    }

    fun executeDuelStart(console: Console, attacker: GamePlayer, victim: GamePlayer, force: Boolean) {
        val duel = Duel(attacker, victim)

        if (!duels.remove(duel) && !force) {
            console.sendError("Duel", duel, "is not available, cannot start")
            return
        }

        context.snapshotService.executeSnapshotSave(console)

        context.damageService.addRecord(attacker, 0.0F - victim.givenDamage.coerceAtLeast(0.0F))
        context.automationService.handleDuelPrepare()

        attacker.team = GameTeam.DUELIST
        attacker.isAlive = true
        victim.team = GameTeam.DUELIST
        victim.isAlive = true

        Async.background().withTimeout(0.1).go {
            while (true) {
                attacker.teleport(properties.positionDuel1)
                victim.teleport(properties.positionDuel2)
                delay()
            }
        }

        state = DuelingGameState()

        console.sendInfo("Duel", duel, "starting")
    }

    fun executeDuelCancel(console: Console, aggressor: GamePlayer, victim: GamePlayer) {
        val duel = Duel(aggressor, victim)

        if (!duels.remove(duel)) {
            console.sendError("Duel", duel, "is not available, cannot cancel")
            return
        }

        console.sendInfo("Duel", duel, "cancelled")
    }

    fun executeDuelCreate(console: Console, attacker: GamePlayer, victim: GamePlayer) {
        val duel = Duel(attacker, victim)

        if (!duels.add(duel)) {
            console.sendError("Duel", duel, "already exists")
            return
        }

        console.sendInfo("Duel", duel, "created")
    }

    fun executeDuelList(console: Console) {
        console.sendInfo("Duels available:")
        for (duel in duels) {
            duel.message().forEach(console::sendInfo)
        }
    }

    private companion object {
        val duelNotPossibleSlot = 13
        val duelSelectionSlot = 16
        val duelBorderSlots = listOf(6, 7, 8, 15, 17, 24, 25, 26).toIntArray()
        val duelPlayerSlots = listOf(0..5, 9..14, 18..23).flatten().toIntArray()
    }

    private inner class DuelScreenHandlerFactory : DynamicScreenHandlerFactory<DuelScreenHandler>() {
        override val name get() = text("baby fight")
        override fun construct(sync: Int, playerInventory: PlayerInventory) = DuelScreenHandler(sync, playerInventory)
    }

    private inner class DuelScreenHandler(sync: Int, playerInventory: PlayerInventory) : DynamicChestScreenHandler(3, sync, playerInventory) {
        private val duels: Map<String, Duel>

        init {
            val player = context.getPlayer(playerEntity)

            val duels = playersNormal
                .asSequence()
                .filter { it != player && it.isPlayingOrGhost }
                .filter { it.givenDamage < player.givenDamage }
                .sortedBy { it.givenDamage }
                .map { Duel(player, it) }
                .associateBy { duel -> duel.id }

            this.duels = duels

            if (duels.isEmpty()) {
                inventory[duelNotPossibleSlot] = stackOf(
                    Items.BARRIER,
                    nbtCompoundOf("MsdIllegal" to true),
                    text("You can't duel anybody!").red(),
                )

                addChestInventorySlots { index, _, _, x, y -> StaticSlot(index, x, y) }
                addPlayerInventorySlots()
            } else {
                val duelHeads = duels.values.map { duel ->
                    stackOf(
                        Items.PLAYER_HEAD,
                        nbtCompoundOf(
                            "SkullOwner" to duel.victim.name,
                            "MsdIllegal" to true,
                            "MsdDuel" to duel.id,
                        ),
                        text("Duel ").append(duel.victim.displayName).append("?"),
                        text("currently on", duel.victim.team).lightPurple(),
                        text("DD left after duel:", text(duel.differenceScore.toString()).green()).lightPurple(),
                    )
                }

                val duelHeadsIterator = duelHeads.iterator()

                for (index in duelPlayerSlots) {
                    if (!duelHeadsIterator.hasNext()) break
                    inventory[index] = duelHeadsIterator.next()
                }

                for (index in duelBorderSlots) {
                    inventory[index] = stackOf(
                        Items.LIME_STAINED_GLASS_PANE,
                        nbtCompoundOf("MsdIllegal" to true),
                        text("Place a head here to duel!"),
                    )
                }

                addChestInventorySlots { index, _, _, x, y ->
                    when (index) {
                        duelSelectionSlot -> object : InputSlot(index, x, y) {
                            override fun canInsert(stack: ItemStack) = isDuelHead(stack)
                        }
                        in duelPlayerSlots -> object : DynamicSlot(index, x, y) {
                            override fun canInsert(stack: ItemStack) = isDuelHead(stack)
                            override fun canTakeItems(playerEntity: PlayerEntity) = true
                        }
                        in duelBorderSlots -> StaticSlot(index, x, y)
                        else -> null
                    }
                }
                addPlayerInventorySlots()
            }
        }

        private fun isDuelHead(stack: ItemStack) = stack.hasNbt() && "MsdDuel" in stack.nbt!!
        private fun getDuelFrom(stack: ItemStack): Duel? =
            if (isDuelHead(stack)) duels[stack.nbt?.getString("MsdDuel")] else null

        override fun handleTakeResult(slot: OutputSlot, stack: ItemStack) {
        }

        override fun handleUpdateResult() {
            val duel = getDuelFrom(inventory[duelSelectionSlot])
            if (duel != null) {
                requestDuel(playerEntity, duel)
                inventory.setStack(duelSelectionSlot, stackOf())
                playerEntity.closeHandledScreen()
            }
        }

        override fun handleClosed() {
        }
    }

}
