package dev.foxgirl.mineseekdestroy.command

import dev.foxgirl.mineseekdestroy.*
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService
import dev.foxgirl.mineseekdestroy.state.*
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.command.EntitySelector
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtByte
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
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
            }
        }
        it.params(argLiteral("start")) {
            fun register(literal: String, properties: () -> GameProperties) {
                it.params(argLiteral(literal)) {
                    it.params(argLiteral("noauto")) {
                        it.action { args ->
                            if (game.context == null) {
                                game.initialize(properties())
                                game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, false)
                                args.sendInfo("Started new game WITHOUT automation")
                            } else {
                                args.sendError("Cannot start new game, already running")
                            }
                        }
                    }
                    it.action { args ->
                        if (game.context == null) {
                            game.initialize(properties())
                            args.sendInfo("Started new game with automation")
                        } else {
                            args.sendError("Cannot start new game, already running")
                        }
                    }
                }
            }
            register("macander") { GameProperties.Macander }
            register("radiator") { GameProperties.Radiator }
            register("realm") { GameProperties.Realm }
            register("lights") { GameProperties.Lights }
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

                    player1.team = GameTeam.PLAYER_DUEL
                    player2.team = GameTeam.PLAYER_DUEL
                    player1.isAlive = true
                    player2.isAlive = true

                    player1.kills = 0
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
            register("finalizing") { FinalizingGameState() }
            register("starting") { StartingGameState() }
            register("playing") { PlayingGameState() }
            register("dueling") { DuelingGameState() }
        }
        it.params(argLiteral("givetools")) {
            it.action { args ->
                val entity = args.context.source.entity
                if (entity is ServerPlayerEntity) {
                    fun stack(i: Int) = ItemStack(Items.SPONGE).also {
                        it.getOrCreateNbt().put("MsdTool${i}", NbtByte.ONE)
                        it.setCustomName(Text.literal("Tool ${i}").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false)))
                    }
                    for (i in 1..4) entity.giveItemStack(stack(i))
                    args.sendInfo("Added tools to inventory")
                } else {
                    args.sendError("Cannot give tools, command source has no entity")
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
                context.barrierService.executeBlimpClose(args)
            }
        }
    }

    Command.build("automation") {
        it.params(argLiteral("enable")) {
            it.actionWithContext { args, context ->
                game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, true)
                args.sendInfo("Automation enabled")
            }
        }
        it.params(argLiteral("disable")) {
            it.actionWithContext { args, context ->
                game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, false)
                args.sendInfo("Automation disabled")
            }
        }
        it.params(argLiteral("ipad")) {
            it.actionWithContext { args, context ->
                context.automationService.executeOpenIpad(args)
            }
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
        register("duel", GameTeam.PLAYER_DUEL)
        register("warden", GameTeam.PLAYER_WARDEN)
        register("black", GameTeam.PLAYER_BLACK)
        register("yellow", GameTeam.PLAYER_YELLOW)
        register("blue", GameTeam.PLAYER_BLUE)
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

    Command.build("tp") {
        fun register(literal: String, position: () -> Position, region: () -> Region) {
            fun teleport(console: Console, players: List<GamePlayer>, all: Boolean, filter: (GamePlayer) -> Boolean = { true }) {
                if (all && game.state is RunningGameState) {
                    console.sendError("Cannot teleport all players while the game is running, use @a to force")
                    return
                }
                players.filter(filter).let {
                    it.forEach { player -> player.teleport(position()) }
                    console.sendInfo("Teleported ${it.size} player(s) to '${literal}'")
                }
            }

            fun isPlaying(player: GamePlayer) = !player.isOperator && player.isPlaying
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
                    game.setRuleDouble(Game.RULE_BORDER_CLOSE_DURATION, args["seconds"])
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
                        game.setRuleBoolean(Game.RULE_GHOULS_ENABLED, true)
                        args.sendInfo("Ghouls enabled")
                    }
                }
            }
            it.params(argLiteral("disable")) {
                it.actionWithContext { args, context ->
                    if (properties != GameProperties.Macander) {
                        args.sendError("Cannot manage ghouls for this arena")
                    } else {
                        game.setRuleBoolean(Game.RULE_GHOULS_ENABLED, false)
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
                        SpecialSummonsService.Theology.valueOf(string.trim().uppercase(Locale.ROOT))
                    } catch (cause : IllegalArgumentException) {
                        args.sendError("Invalid theology '${string}'")
                        null
                    }

                val kind = SpecialSummonsService.TheologyPair(
                    theologyOf(args["theology1"]) ?: return@actionWithContext,
                    theologyOf(args["theology2"]) ?: return@actionWithContext,
                )
                context.specialSummonsService.executeSummon(args, kind, args.player(context))
            }
        }
        it.params(argLiteral("cleartimeout")) {
            it.actionWithContext { args, context ->
                context.specialSummonsService.executeClearTimeout(args)
            }
        }
        it.params(argLiteral("debug")) {
            it.params(argLiteral("print")) {
                it.actionWithContext { args, context ->
                    context.specialSummonsService.executeDebugPrint(args)
                }
            }
            it.params(argLiteral("reset")) {
                it.actionWithContext { args, context ->
                    context.specialSummonsService.executeDebugReset(args)
                }
            }
            it.params(argLiteral("showtext")) {
                it.actionWithContext { args, context ->
                    context.specialSummonsService.executeDebugShowText(args)
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

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.player(context: GameContext, name: String = "player")
    = context.getPlayer(this.get<EntitySelector>(name).getPlayer(this.context.source))

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.players(context: GameContext, name: String = "players")
    = context.getPlayers(this.get<EntitySelector>(name).getPlayers(this.context.source))
