package dev.foxgirl.mineseekdestroy.command

import com.mojang.brigadier.builder.ArgumentBuilder
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.fabric.FabricAdapter
import dev.foxgirl.mineseekdestroy.*
import dev.foxgirl.mineseekdestroy.service.DamageService
import dev.foxgirl.mineseekdestroy.service.PagesService
import dev.foxgirl.mineseekdestroy.service.SummonsService
import dev.foxgirl.mineseekdestroy.state.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.await
import net.minecraft.block.Blocks
import net.minecraft.command.EntitySelector
import net.minecraft.item.Items
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Position
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import java.util.*

internal fun setup() {

    Command.build("game") {
        it.params(argLiteral("end")) {
            it.actionWithContext { args, context ->
                context.barrierService.executeArenaOpen(args)
                context.barrierService.executeBlimpOpen(args)
                game.destroy()
                args.sendInfo("Stopped game")
            }
        }
        it.params(argLiteral("start")) {
            fun register(properties: () -> GameProperties) {
                val flags = mapOf<String, () -> Unit>(
                    "noauto" to { Rules.automationEnabled = false },
                    "noghosts" to { Rules.automationGhostsEnabled = false },
                    "nosummons" to { Rules.summonsEnabled = false },
                    "nocountdown" to { Rules.countdownEnabled = false },
                    "nocountdownautostart" to { Rules.countdownAutostartEnabled = false },
                    "hiddenarmor" to { Rules.hiddenArmorEnabled = true },
                    "chaos" to { Rules.chaosEnabled = true },
                    "gifts" to { Rules.giftsEnabled = true },
                )
                fun registerFlags(it: ArgumentBuilder<ServerCommandSource, *>, count: Int, i: Int = 0) {
                    it.action { args ->
                        if (game.context == null) {
                            game.initialize(properties())
                            val flagsSelected = mutableSetOf<String>()
                            for (j in 0 until i) {
                                flagsSelected.add(args["flag$j"])
                            }
                            if (flagsSelected.isNotEmpty()) {
                                flagsSelected.forEach { flags[it]?.invoke() ?: args.sendError("Invalid flag '$it'") }
                                args.sendInfo("Started new game with flags", flagsSelected)
                            } else {
                                args.sendInfo("Started new game with defaults")
                            }
                        } else {
                            args.sendError("Cannot start new game, already running")
                        }
                    }
                    if (i < count) {
                        it.params(argString("flag$i")) { registerFlags(it, count, i + 1) }
                    }
                }
                it.params(argLiteral(properties().name)) { registerFlags(it, flags.size) }
            }
            GameProperties.instances.forEach { register { it } }
        }
        it.params(argLiteral("prepare")) {
            it.actionWithContext { args, context ->
                args.sendInfo("Resetting and preparing game state")
                context.stormService.executeStormStop(args)
                context.barrierService.executeArenaOpen(args)
                context.barrierService.executeBlimpClose(args)
                context.lootService.executeClear(args)
                context.lootService.executeFill(args)
                context.smokerService.executeClear(args)
                context.inventoryService.executeClear(args)
                context.inventoryService.executeFill(args)
                context.players.forEach { if (!it.isOperator) it.teleport(properties.positionBlimp) }
            }
        }
        it.params(argLiteral("unfreeze")) {
            fun register(name: String, state: () -> GameState) {
                it.params(argLiteral(name)) {
                    it.actionWithContext { args, context ->
                        if (game.state is FrozenGameState) {
                            game.state = state()
                            args.sendInfo("Unfroze game!")
                        } else {
                            args.sendError("Cannot unfreeze game, not frozen")
                        }
                    }
                }
            }
            register("waiting") { WaitingGameState() }
            register("dueling") { DuelingGameState() }
            register("playing") { PlayingGameState() }
        }
    }

    Command.build("begin") {
        it.params(argLiteral("round")) {
            it.actionWithContext { args, context ->
                game.state = StartingGameState()
                context.snapshotService.executeSnapshotSave(args)
            }
        }
        it.params(argLiteral("duel")) {
            it.params(argPlayers("aggressor"), argPlayers("victim")) {
                it.actionWithContext { args, context ->
                    context.automationService.handleDuelPrepare()

                    val player1 = args.player(context, "aggressor")
                    val player2 = args.player(context, "victim")

                    player1.team = GameTeam.DUELIST
                    player2.team = GameTeam.DUELIST
                    player1.isAlive = true
                    player2.isAlive = true

                    player1.teleport(properties.positionDuel1)
                    player2.teleport(properties.positionDuel2)

                    game.state = DuelingGameState()
                    context.snapshotService.executeSnapshotSave(args)
                }
            }
            it.actionWithContext { args, context ->
                game.state = DuelingGameState()
                context.snapshotService.executeSnapshotSave(args)
            }
        }
    }

    Command.build("stop") {
        it.action { args ->
            game.state = WaitingGameState()
            args.sendInfo("Reset current game state")
        }
    }

    Command.build("debug") {
        it.params(argLiteral("players")) {
            it.params(argLiteral("list")) {
                it.actionWithContext { args, context ->
                    val players = context.players
                    args.sendInfo("Players (${players.size}):")
                    players.forEach {
                        args.sendInfo(
                            "  -", text(it.displayName).styleParent { it.withColor(Formatting.WHITE) },
                            "team:", it.team,
                            "alive:", it.isAlive,
                            "connected:", it.entity?.networkHandler?.isConnectionOpen ?: false,
                        )
                    }
                }
            }
            it.params(argLiteral("dump")) {
                fun dump(console: Console, player: GamePlayer) {
                    console.sendInfo("Player", text(player.displayName).styleParent { it.withColor(Formatting.WHITE) }, player.uuid)
                    console.sendInfo("  - team:", player.team)
                    console.sendInfo("  - isPlaying:", player.isPlaying)
                    console.sendInfo("  - isCannon:", player.isCannon)
                    console.sendInfo("  - souls:", player.souls)
                    console.sendInfo("  - kills:", player.kills)
                    console.sendInfo("  - deaths:", player.deaths)
                    console.sendInfo("  - givenDamage:", player.givenDamage)
                    console.sendInfo("  - takenDamage:", player.takenDamage)
                    console.sendInfo("  - isAlive:", player.isAlive)
                    console.sendInfo("  - isLiving:", player.isLiving)
                    console.sendInfo("  - entity:", player.entity.toString().asText().formatted(Formatting.WHITE))
                }
                it.params(argPlayer()) {
                    it.actionWithContext { args, context ->
                        dump(args, args.player(context))
                    }
                }
                it.params(argLiteral("specific"), argString("player")) {
                    it.actionWithContext { args, context ->
                        val player = context.getPlayer(args.get<String>("player"))
                        if (player != null) {
                            dump(args, player)
                        } else {
                            args.sendError("Player not found")
                        }
                    }
                }
            }
        }
        it.params(argLiteral("state")) {
            fun register(literal: String, state: () -> GameState) {
                it.params(argLiteral(literal)) {
                    it.action { args ->
                        game.state = state()
                        args.sendInfo("Set game state to '${literal}'")
                    }
                }
            }
            register("waiting") { WaitingGameState() }
            register("frozen") { FrozenGameState() }
            register("arena") { ArenaGameState() }
            register("finalizing") { FinalizingGameState() }
            register("starting") { StartingGameState() }
            register("playing") { PlayingGameState() }
            register("dueling") { DuelingGameState() }
        }
        it.params(argLiteral("givetools")) {
            it.action { args ->
                val entity = args.context.source.entity
                if (entity is ServerPlayerEntity) {
                    for (i in 1..4) {
                        entity.give(stackOf(
                            Items.SPONGE, nbtCompoundOf("MsdTool${i}".intern() to true),
                            text("Tool ${i}"),
                        ))
                    }
                    args.sendInfo("Added tools to inventory")
                } else {
                    args.sendError("Cannot give tools, command source is not a player")
                }
            }
        }
        it.params(argLiteral("giveitems")) {
            it.action { args ->
                val entity = args.context.source.entity
                if (entity is ServerPlayerEntity) {
                    GameItems.properties.forEach { entity.give(it().copy()) }
                    args.sendInfo("Added all game items to inventory")
                } else {
                    args.sendError("Cannot give special items, command source is not a player")
                }
            }
        }
        it.params(argLiteral("givebooks")) {
            it.action { args ->
                val entity = args.context.source.entity
                if (entity is ServerPlayerEntity) {
                    for (stack in listOf(
                        GameItems.bookDeep,
                        GameItems.bookOccult,
                        GameItems.bookCosmos,
                        GameItems.bookBarter,
                        GameItems.bookFlame,
                    )) {
                        entity.give(stack.copy())
                    }
                    args.sendInfo("Added all books to inventory")
                } else {
                    args.sendError("Cannot give books, command source is not a player")
                }
            }
        }
        it.params(argLiteral("cleanloot")) {
            it.actionWithContext { args, context ->
                context.lootService.executeDebugClean(args)
            }
        }
        it.params(argLiteral("danger")) {
            it.params(argLiteral("loopforever")) {
                it.action { args ->
                    var i = 0
                    while (true) i++
                }
            }
        }
        it.params(argLiteral("removeunexposed")) {
            it.action { args ->
                val entity = args.context.source.entity
                if (entity !is ServerPlayerEntity) {
                    args.sendError("Cannot remove unexposed blocks, command source is not a player")
                    return@action
                }
                args.sendError("Unimplemented")
                val sessionManager = WorldEdit.getInstance().getSessionManager().get(FabricAdapter.adaptPlayer(entity))
                val region = sessionManager.selection.let {
                    Region(
                        it.minimumPoint.let { BlockPos(it.x, it.y, it.z) },
                        it.maximumPoint.let { BlockPos(it.x, it.y, it.z) },
                    )
                }
                val world = FabricAdapter.adapt(sessionManager.selectionWorld) as ServerWorld
                Async.go {
                    args.sendInfo("Starting task for ${region.blockCount} block(s)")
                    val positions = HashSet<BlockPos>(region.blockCount.toInt())
                    fun isOpaqueFullCube(pos: BlockPos) = world.getBlockState(pos).isOpaqueFullCube(world, pos)
                    Editor
                        .queue(world, region)
                        .edit { state, x, y, z ->
                            val pos = BlockPos(x, y, z)
                            if (
                                state.isOpaque &&
                                isOpaqueFullCube(pos.down()) &&
                                isOpaqueFullCube(pos.up()) &&
                                isOpaqueFullCube(pos.north()) &&
                                isOpaqueFullCube(pos.south()) &&
                                isOpaqueFullCube(pos.west()) &&
                                isOpaqueFullCube(pos.east())
                            ) {
                                positions.add(pos)
                            }
                            null
                        }
                        .await()
                    Editor
                        .queue(world, region)
                        .edit { _, x, y, z ->
                            if (positions.contains(BlockPos(x, y, z))) Blocks.AIR.defaultState else null
                        }
                        .await()
                    args.sendInfo("Completed task for ${region.blockCount} block(s)")
                }
            }
        }
    }

    Command.build("snapshot") {
        it.params(argLiteral("save")) {
            it.actionWithContext { args, context ->
                context.snapshotService.executeSnapshotSave(args)
            }
        }
        it.params(argLiteral("restore")) {
            it.actionWithContext { args, context ->
                context.snapshotService.executeSnapshotRestore(args)
            }
        }
        it.params(argLiteral("loadbackup")) {
            it.params(argGreedyString("name")) {
                it.actionWithContext { args, context ->
                    context.snapshotService.executeSnapshotLoadBackup(args, args["name"])
                }
            }
            it.actionWithContext { args, context ->
                context.snapshotService.executeSnapshotLoadBackup(args, null)
            }
        }
    }

    Command.build("automation") {
        it.params(argLiteral("enable")) {
            it.actionWithContext { args, context ->
                Rules.automationEnabled = true
                args.sendInfo("Automation enabled")
            }
        }
        it.params(argLiteral("disable")) {
            it.actionWithContext { args, context ->
                Rules.automationEnabled = false
                args.sendInfo("Automation disabled")
            }
        }
        it.params(argLiteral("ipad")) {
            it.actionWithContext { args, context ->
                context.automationService.executeOpenIpad(args)
            }
        }
    }

    Command.build("countdown") {
        it.params(argLiteral("set")) {
            it.params(argLiteral("time"), argDouble("seconds")) {
                it.actionWithContext { args, context -> context.countdownService.executeSetTime(args, args["seconds"]) }
            }
            it.params(argLiteral("damage"), argDouble("hearts")) {
                it.actionWithContext { args, context -> context.countdownService.executeSetDamage(args, args["hearts"]) }
            }
            it.params(argLiteral("enabled")) {
                it.params(argLiteral("yes")) { it.actionWithContext { args, context -> context.countdownService.executeSetEnabled(args, true) } }
                it.params(argLiteral("no")) { it.actionWithContext { args, context -> context.countdownService.executeSetEnabled(args, false) } }
            }
            it.params(argLiteral("autostart")) {
                it.params(argLiteral("yes")) { it.actionWithContext { args, context -> context.countdownService.executeSetAutostart(args, true) } }
                it.params(argLiteral("no")) { it.actionWithContext { args, context -> context.countdownService.executeSetAutostart(args, false) } }
            }
            it.params(argLiteral("progression")) {
                it.params(argLiteral("yes")) { it.actionWithContext { args, context -> context.countdownService.executeSetProgression(args, true) } }
                it.params(argLiteral("no")) { it.actionWithContext { args, context -> context.countdownService.executeSetProgression(args, false) } }
            }
        }
        it.params(argLiteral("start")) {
            it.params(argInt("iteration")) {
                it.actionWithContext { args, context -> context.countdownService.executeStart(args, args["iteration"]) }
            }
            it.actionWithContext { args, context -> context.countdownService.executeStart(args) }
        }
        it.params(argLiteral("stop")) {
            it.actionWithContext { args, context -> context.countdownService.executeStop(args) }
        }
    }

    Command.build("team") {
        it.params(argLiteral("remove"), argString("player")) {
            it.actionWithContext { args, context ->
                val player = context.getPlayer(args.get<String>("player"))
                if (player != null) {
                    player.team = GameTeam.NONE
                    args.sendInfo("Specified player removed")
                } else {
                    args.sendError("Specified player not found")
                }
            }
        }
        fun register(literal: String, team: GameTeam) {
            it.params(argLiteral(literal)) {
                it.params(argPlayers()) {
                    it.actionWithContext { args, context ->
                        val players = args.players(context).onEach { it.team = team }
                        args.sendInfo("Updated team for ${players.size} player(s) to", team.displayName)
                    }
                }
            }
        }
        register("none", GameTeam.NONE)
        register("skip", GameTeam.SKIP)
        register("ghost", GameTeam.GHOST)
        register("operator", GameTeam.OPERATOR)
        register("duel", GameTeam.DUELIST)
        register("warden", GameTeam.WARDEN)
        register("black", GameTeam.BLACK)
        register("yellow", GameTeam.YELLOW)
        register("blue", GameTeam.BLUE)
    }

    Command.build("score") {
        it.params(argLiteral("setalive"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players(context).onEach { it.isAlive = true }
                args.sendInfo("Updated alive status for ${players.size} player(s) to living")
            }
        }
        it.params(argLiteral("setdead"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players(context).onEach { it.isAlive = false }
                args.sendInfo("Updated alive status for ${players.size} player(s) to dead")
            }
        }
        it.params(argLiteral("setkills"), argInt("kills"), argPlayers()) {
            it.actionWithContext { args, context ->
                val kills: Int = args["kills"]
                val players = args.players(context).onEach { it.kills = kills }
                args.sendInfo("Updated kill count for ${players.size} player(s)")
            }
        }
        it.params(argLiteral("clearkills"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players(context).onEach { it.kills = 0 }
                args.sendInfo("Updated kill count for ${players.size} player(s)")
            }
        }
        it.params(argLiteral("givekill"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players(context).onEach { it.kills++ }
                args.sendInfo("Updated kill count for ${players.size} player(s)")
            }
        }
        it.params(argLiteral("takekill"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players(context).onEach { it.kills-- }
                args.sendInfo("Updated kill count for ${players.size} player(s)")
            }
        }
    }

    Command.build("damage") {
        it.params(argLiteral("dump")) {
            it.actionWithContext { args, context ->
                args.sendInfo("Damage Records (${context.damageService.damageRecords.size}):")
                context.damageService.damageRecords.forEach { record -> args.sendInfo("  ", record) }
            }
        }
        it.params(argLiteral("clear")) {
            fun clearDamage(context: GameContext, console: Console, predicate: (DamageService.DamageRecord) -> Boolean) {
                var count = 0
                context.damageService.updateDamageRecords { records -> records.removeIf { record -> predicate(record).also { if (it) count++ } } }
                console.sendInfo("Cleared $count damage record(s)")
            }
            it.params(argLiteral("given"), argPlayers()) {
                it.actionWithContext { args, context ->
                    val players = args.players(context).map(GamePlayer::getUUID).toSet()
                    clearDamage(context, args) { record -> record.attackerUUID in players }
                }
            }
            it.params(argLiteral("taken")) {
                it.actionWithContext { args, context ->
                    val players = args.players(context).map(GamePlayer::getUUID).toSet()
                    clearDamage(context, args) { record -> record.victimUUID in players }
                }
            }
            it.actionWithContext { args, context ->
                context.damageService.updateDamageRecords { records -> records.clear() }
                args.sendInfo("Cleared all damage records")
            }
        }
        it.params(argLiteral("add"), argDouble("hearts", -5000.0, 5000.0), argPlayers()) {
            it.actionWithContext { args, context ->
                val damage = args.get<Double>("hearts").toFloat() * 2.0F
                val players = args.players(context)
                players.forEach { player -> context.damageService.addRecord(player, damage) }
                args.sendInfo("Added new damage record(s) for ${players.size} player(s)")
            }
        }
    }

    Command.build("soul") {
        it.params(argLiteral("give"), argPlayers(), argPlayer()) {
            it.params(argLiteral("yellow")) {
                it.actionWithContext { args, context ->
                    context.soulService.executeSoulGive(args, args.players(context), args.player(context), GameTeam.YELLOW)
                }
            }
            it.params(argLiteral("blue")) {
                it.actionWithContext { args, context ->
                    context.soulService.executeSoulGive(args, args.players(context), args.player(context), GameTeam.BLUE)
                }
            }
            it.actionWithContext { args, context ->
                context.soulService.executeSoulGive(args, args.players(context), args.player(context))
            }
        }
        it.params(argLiteral("clear")) {
            it.params(argPlayers()) {
                it.actionWithContext { args, context ->
                    context.soulService.executeSoulClear(args, args.players(context))
                }
            }
            it.actionWithContext { args, context ->
                context.soulService.executeSoulClear(args, context.players)
            }
        }
        it.params(argLiteral("list")) {
            it.params(argPlayers()) {
                it.actionWithContext { args, context ->
                    context.soulService.executeSoulList(args, args.players(context))
                }
            }
            it.actionWithContext { args, context ->
                context.soulService.executeSoulList(args, context.players)
            }
        }
    }

    Command.build("duel") {
        it.params(argLiteral("open")) {
            it.actionWithContext { args, context ->
                context.soulService.executeDuelOpen(args)
            }
        }
        it.params(argLiteral("close")) {
            it.actionWithContext { args, context ->
                context.soulService.executeDuelClose(args)
            }
        }
        it.params(argLiteral("start")) {
            fun <T : Command.Arguments<ServerCommandSource>> start(args: T, context: GameContext, force: Boolean, aggressor: (T) -> GamePlayer?, victim: (T) -> GamePlayer?) {
                val playerAggressor = aggressor(args)
                if (playerAggressor == null) { args.sendError("Invalid aggressor"); return }
                val playerVictim = victim(args)
                if (playerVictim == null) { args.sendError("Invalid victim"); return }
                context.soulService.executeDuelStart(args, playerAggressor, playerVictim, force)
            }
            fun startEntity(args: Command.Arguments<ServerCommandSource>, context: GameContext, force: Boolean) = start(args, context, force, { args.player(context, "aggressor") }, { args.player(context, "victim") })
            fun startExact(args: Command.Arguments<ServerCommandSource>, context: GameContext, force: Boolean) = start(args, context, force, { context.getPlayer(args.get<String>("aggressor")) }, { context.getPlayer(args.get<String>("victim")) })
            it.params(argLiteral("exact")) {
                it.params(argString("aggressor"), argString("victim")) {
                    it.params(argLiteral("force")) {
                        it.actionWithContext { args, context -> startExact(args, context, true) }
                    }
                    it.actionWithContext { args, context -> startExact(args, context, false) }
                }
            }
            it.params(argPlayer("aggressor"), argPlayer("victim")) {
                it.params(argLiteral("force")) {
                    it.actionWithContext { args, context -> startEntity(args, context, true) }
                }
                it.actionWithContext { args, context -> startEntity(args, context, false) }
            }
        }
        it.params(argLiteral("cancel")) {
            it.params(argLiteral("exact")) {
                it.params(argString("aggressor"), argString("victim")) {
                    it.actionWithContext { args, context ->
                        val aggressor = context.getPlayer(args.get<String>("aggressor"))
                        if (aggressor == null) { args.sendError("Invalid aggressor"); return@actionWithContext }
                        val victim = context.getPlayer(args.get<String>("victim"))
                        if (victim == null) { args.sendError("Invalid victim"); return@actionWithContext }
                        context.soulService.executeDuelCancel(args, aggressor, victim)
                    }
                }
            }
            it.params(argPlayer("aggressor"), argPlayer("victim")) {
                it.actionWithContext { args, context ->
                    context.soulService.executeDuelCancel(args, args.player(context, "aggressor"), args.player(context, "victim"))
                }
            }
        }
        it.params(argLiteral("list")) {
            it.actionWithContext { args, context ->
                context.soulService.executeDuelList(args)
            }
        }
    }

    Command.build("pages") {
        fun <T : ArgumentBuilder<ServerCommandSource, *>> T.theologies(callback: (ArgumentBuilder<ServerCommandSource, *>, SummonsService.Theology) -> Unit): T {
            for (theology in SummonsService.Theology.entries) {
                params(argLiteral(theology.name.lowercase())) { callback(it, theology) }
            }
            return this
        }
        fun <T : ArgumentBuilder<ServerCommandSource, *>> T.actions(callback: (ArgumentBuilder<ServerCommandSource, *>, PagesService.Action) -> Unit): T {
            for (action in PagesService.Action.entries) {
                params(argLiteral(action.name.lowercase())) { callback(it, action) }
            }
            return this
        }
        it.params(argLiteral("give")) {
            it.params(argLiteral("book")) {
                it.theologies { it, theology ->
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context ->
                            context.pagesService.executeBookGive(args, args.players(context), PagesService.BookType(theology))
                        }
                    }
                }
            }
            it.params(argLiteral("page")) {
                it.theologies { it, theology ->
                    it.actions { it, action ->
                        it.params(argPlayers()) {
                            it.actionWithContext { args, context ->
                                context.pagesService.executePageGive(args, args.players(context), PagesService.PageType(theology, action))
                            }
                        }
                    }
                }
            }
        }
    }

    Command.build("ghosts") {
        it.params(argLiteral("setdeaths"), argPlayers(), argInt("value", 0, 4)) {
            it.actionWithContext { args, context ->
                context.ghostService.executeSetBlackDeaths(args, args.players(context), args["value"])
            }
        }
        it.params(argLiteral("cleardeaths"), argPlayers()) {
            it.actionWithContext { args, context ->
                context.ghostService.executeClearBlackDeaths(args, args.players(context))
            }
        }
    }

    Command.build("tp") {
        fun register(literal: String, position: () -> Position, region: () -> Region) {
            fun teleport(console: Console, players: List<GamePlayer>, all: Boolean, filter: (GamePlayer) -> Boolean = { true }) {
                if (all && game.state.isRunning) {
                    console.sendError("Cannot teleport all players while the game is running, use @a to force")
                    return
                }
                players.filter(filter).let {
                    it.forEach { player -> player.teleport(position()) }
                    console.sendInfo("Teleported ${it.size} player(s) to '${literal}'")
                }
            }

            fun isPlaying(player: GamePlayer) = !player.isOperator && player.isPlayingOrGhost
            fun isRegion(player: GamePlayer) = !player.isOperator && player.entity.let { if (it != null) !region().contains(it) else false }

            it.params(argLiteral(literal)) {
                it.params(argLiteral("area")) {
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context -> teleport(args, args.players(context), false, ::isRegion) }
                    }
                    it.actionWithContext { args, context -> teleport(args, context.players, true, ::isRegion) }
                }
                it.params(argLiteral("force")) {
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context -> teleport(args, args.players(context), false) }
                    }
                    it.actionWithContext { args, context -> teleport(args, context.players, true) }
                }
                it.params(argPlayers()) {
                    it.actionWithContext { args, context -> teleport(args, args.players(context), false, ::isPlaying) }
                }
                it.actionWithContext { args, context -> teleport(args, context.players, true, ::isPlaying) }
            }
        }
        register("spawn", { properties.positionSpawn.toCenterPos() }, { properties.regionAll })
        register("blimp", { properties.positionBlimp }, { properties.regionBlimp })
        register("arena", { properties.positionArena }, { properties.regionPlayable })
        register("duel1", { properties.positionDuel1 }, { properties.regionPlayable })
        register("duel2", { properties.positionDuel2 }, { properties.regionPlayable })
    }

    Command.build("inv") {
        it.params(argLiteral("clear")) {
            it.params(argPlayers()) {
                it.actionWithContext { args, context ->
                    context.inventoryService.executeClear(args, args.players(context))
                }
            }
            it.actionWithContext { args, context ->
                context.inventoryService.executeClear(args)
            }
        }
        it.params(argLiteral("fill")) {
            it.params(argPlayers()) {
                it.actionWithContext { args, context ->
                    context.inventoryService.executeFill(args, args.players(context))
                }
            }
            it.actionWithContext { args, context ->
                context.inventoryService.executeFill(args)
            }
        }
    }

    Command.build("loot") {
        it.params(argLiteral("clear")) {
            it.actionWithContext { args, context ->
                context.lootService.executeClear(args)
            }
        }
        it.params(argLiteral("fill")) {
            it.actionWithContext { args, context ->
                context.lootService.executeFill(args)
            }
        }
    }

    Command.build("smoker") {
        it.params(argLiteral("clear")) {
            it.actionWithContext { args, context ->
                context.smokerService.executeClear(args)
            }
        }
        it.params(argLiteral("fill")) {
            it.actionWithContext { args, context ->
                context.smokerService.executeFill(args)
            }
        }
    }

    Command.build("invis") {
        it.params(argLiteral("on")) {
            it.actionWithContext { args, context ->
                context.invisibilityService.executeSetEnabled(args)
            }
        }
        it.params(argLiteral("off")) {
            it.actionWithContext { args, context ->
                context.invisibilityService.executeSetDisabled(args)
            }
        }
    }

    Command.build("barrier") {
        it.params(argLiteral("arena")) {
            it.params(argLiteral("open")) {
                it.actionWithContext { args, context ->
                    context.barrierService.executeArenaOpen(args)
                }
            }
            it.params(argLiteral("close")) {
                it.actionWithContext { args, context ->
                    context.barrierService.executeArenaClose(args)
                }
            }
        }
        it.params(argLiteral("blimp")) {
            it.params(argLiteral("open")) {
                it.actionWithContext { args, context ->
                    context.barrierService.executeBlimpOpen(args)
                }
            }
            it.params(argLiteral("close")) {
                it.actionWithContext { args, context ->
                    context.barrierService.executeBlimpClose(args)
                }
            }
        }
    }

    Command.build("storm") {
        it.params(argLiteral("start")) {
            it.params(argDouble("seconds")) {
                it.actionWithContext { args, context ->
                    Rules.borderCloseDuration = args["seconds"]
                    context.stormService.executeStormStart(args)
                }
            }
            it.actionWithContext { args, context ->
                context.stormService.executeStormStart(args)
            }
        }
        it.params(argLiteral("stop")) {
            it.actionWithContext { args, context ->
                context.stormService.executeStormStop(args)
            }
        }
        it.params(argLiteral("clear")) {
            it.actionWithContext { args, context ->
                context.stormService.executeStormClear(args)
            }
        }
    }

    Command.build("gimmick") {
        it.params(argLiteral("buddy")) {
            it.params(argLiteral("enable")) {
                it.actionWithContext { args, context ->
                    Rules.buddyEnabled = true
                    game.setRuleBoolean(Game.RULE_BUDDY_ENABLED, true)
                    args.sendInfo("Buddy system enabled")
                }
            }
            it.params(argLiteral("disable")) {
                it.actionWithContext { args, context ->
                    Rules.buddyEnabled = false
                    args.sendInfo("Buddy system disabled")
                }
            }
            it.params(argLiteral("list")) {
                it.params(argLiteral("announce")) {
                    it.actionWithContext { args, context ->
                        context.specialBuddyService.executeBuddyList(Game.CONSOLE_PLAYERS, "You'll want to carry this person to finals:")
                    }
                }
                it.actionWithContext { args, context ->
                    context.specialBuddyService.executeBuddyList(args)
                }
            }
            it.params(argLiteral("add"), argPlayer("follower"), argPlayer("target")) {
                it.actionWithContext { args, context ->
                    context.specialBuddyService.executeBuddyAdd(
                        args,
                        args.player(context, "target"),
                        args.player(context, "follower"),
                    )
                }
            }
            it.params(argLiteral("remove"), argPlayer("follower"), argPlayer("target")) {
                it.actionWithContext { args, context ->
                    context.specialBuddyService.executeBuddyRemove(
                        args,
                        args.player(context, "target"),
                        args.player(context, "follower"),
                    )
                }
            }
        }
        it.params(argLiteral("cars")) {
            it.params(argLiteral("spawn")) {
                it.actionWithContext { args, context ->
                    val position = args.context.source.position
                    if (position == null) {
                        args.sendError("Cannot spawn car from console")
                    } else {
                        context.specialCarService.executeSpawnCar(args, position)
                    }
                }
            }
            it.params(argLiteral("kill")) {
                it.actionWithContext { args, context ->
                    context.specialCarService.executeKillCars(args)
                }
            }
            it.params(argLiteral("show")) {
                it.actionWithContext { args, context ->
                    context.specialCarService.executeShowCars(args)
                }
            }
            it.params(argLiteral("hide")) {
                it.actionWithContext { args, context ->
                    context.specialCarService.executeHideCars(args)
                }
            }
        }
        it.params(argLiteral("ghouls")) {
            it.params(argLiteral("enable")) {
                it.actionWithContext { args, context ->
                    if (properties != GameProperties.Macander) {
                        args.sendError("Cannot manage ghouls for this arena")
                    } else {
                        Rules.ghoulsEnabled = true
                        args.sendInfo("Ghouls enabled")
                    }
                }
            }
            it.params(argLiteral("disable")) {
                it.actionWithContext { args, context ->
                    if (properties != GameProperties.Macander) {
                        args.sendError("Cannot manage ghouls for this arena")
                    } else {
                        Rules.ghoulsEnabled = false
                        args.sendInfo("Ghouls disabled")
                    }
                }
            }
        }
        it.params(argLiteral("burning")) {
            it.params(argLiteral("start")) {
                it.actionWithContext { args, context ->
                    context.barrierService.executeArenaClose(args)
                    game.setRuleBoolean(GameRules.DO_FIRE_TICK, true)
                    args.sendInfo("Burning started")
                }
            }
        }
    }

    Command.build("summon") {
        it.params(argLiteral("perform"), argPlayer("player"), argString("theology1"), argString("theology2")) {
            it.actionWithContext { args, context ->
                fun theologyOf(string: String) =
                    try {
                        SummonsService.Theology.valueOf(string.trim().uppercase(Locale.ROOT))
                    } catch (cause : IllegalArgumentException) {
                        args.sendError("Invalid theology '${string}'")
                        null
                    }

                val kind = SummonsService.Prayer(
                    theologyOf(args["theology1"]) ?: return@actionWithContext,
                    theologyOf(args["theology2"]) ?: return@actionWithContext,
                )
                context.summonsService.executeSummon(args, kind, args.player(context))
            }
        }
        it.params(argLiteral("cleartimeout")) {
            it.actionWithContext { args, context ->
                context.summonsService.executeClearTimeout(args)
            }
        }
        it.params(argLiteral("debug")) {
            it.params(argLiteral("print")) {
                it.actionWithContext { args, context ->
                    context.summonsService.executeDebugPrint(args)
                }
            }
            it.params(argLiteral("reset")) {
                it.actionWithContext { args, context ->
                    context.summonsService.executeDebugReset(args)
                }
            }
            it.params(argLiteral("showtext")) {
                it.actionWithContext { args, context ->
                    context.summonsService.executeDebugShowText(args)
                }
            }
        }
    }

    Command.build("gm") {
        fun register(literal: String, mode: GameMode) {
            it.params(argLiteral(literal)) {
                it.params(argPlayers()) {
                    it.actionWithContext { args, context ->
                        val players = args.players(context).onEach { it.entity?.changeGameMode(mode) }
                        args.sendInfo("Set game mode for ${players.size} players to ${literal}")
                    }
                }
                it.actionWithContext { args, context ->
                    val players = context.players.filter { it.isOperator }.onEach { it.entity?.changeGameMode(mode) }
                    args.sendInfo("Set game mode for ${players.size} players to ${literal}")
                }
            }
        }
        register("survival", GameMode.SURVIVAL)
        register("creative", GameMode.CREATIVE)
        register("spectator", GameMode.SPECTATOR)
        register("adventure", GameMode.ADVENTURE)
    }

}

private val game get() = Game.getGame()
private val properties get() = Game.getGameProperties()

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.player(context: GameContext, name: String = "player") =
    context.getPlayer(this.get<EntitySelector>(name).getPlayer(this.context.source))
private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.players(context: GameContext, name: String = "players") =
    context.getPlayers(this.get<EntitySelector>(name).getPlayers(this.context.source))
