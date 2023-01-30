package dev.foxgirl.mineseekdestroy.command

import dev.foxgirl.mineseekdestroy.*
import dev.foxgirl.mineseekdestroy.state.*
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.command.EntitySelector
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.Position
import net.minecraft.world.GameRules

internal fun setup() {

    Command.build("game") {
        it.params(argLiteral("end")) {
            it.action { args ->
                if (game.context != null) {
                    game.destroy()
                    args.sendInfo("Ended game")
                } else {
                    args.sendError("Cannot end game, no game running")
                }
            }
        }
        it.params(argLiteral("start")) {
            fun register(literal: String, properties: () -> GameProperties) {
                it.params(argLiteral(literal)) {
                    it.params(argLiteral("automated")) {
                        it.action { args ->
                            if (game.context == null) {
                                game.initialize(properties())
                                game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, true)
                                args.sendInfo("Started new game with automation")
                            } else {
                                args.sendError("Cannot start new game, already running")
                            }
                        }
                    }
                    it.action { args ->
                        if (game.context == null) {
                            game.initialize(properties())
                            args.sendInfo("Started new game")
                        } else {
                            args.sendError("Cannot start new game, already running")
                        }
                    }
                }
            }
            register("macander") { GameProperties.Macander }
            register("radiator") { GameProperties.Radiator }
            register("realm") { GameProperties.Realm }
        }
        it.params(argLiteral("prepare")) {
            it.actionWithContext { args, context ->
                args.sendInfo("Resetting and preparing game state")
                context.stormService.executeStormStop(args)
                context.barrierService.executeArenaOpen(args)
                context.barrierService.executeBlimpClose(args)
                context.lootService.executeClear(args)
                context.smokerService.executeClear(args)
                context.inventoryService.executeClear(args)
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

    Command.build("state") {
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
                context.automationService.executeOpenIpad()
                args.sendInfo("Opening iPad")
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
                        args.sendInfo("Updated team for ${players.size} player(s) to", team.nameColored)
                    }
                }
            }
        }
        register("none", GameTeam.NONE)
        register("skip", GameTeam.SKIP)
        register("operator", GameTeam.OPERATOR)
        register("black", GameTeam.PLAYER_BLACK)
        register("yellow", GameTeam.PLAYER_YELLOW)
        register("blue", GameTeam.PLAYER_BLUE)
        register("duel", GameTeam.PLAYER_DUEL)
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

            fun isPlaying(player: GamePlayer) = player.isPlaying
            fun isInRegion(player: GamePlayer) = player.entity.let { if (it != null) region().contains(it) else false }

            it.params(argLiteral(literal)) {
                it.params(argLiteral("area")) {
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context -> teleport(args, args.players(context), false, ::isInRegion) }
                    }
                    it.actionWithContext { args, context -> teleport(args, context.players, true, ::isInRegion) }
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
        it.params(argLiteral("ghosts")) {
            it.params(argLiteral("enable")) {
                it.actionWithContext { args, context ->
                    if (properties != GameProperties.Macander) {
                        args.sendError("Cannot manage ghosts for this arena")
                        return@actionWithContext
                    }
                    game.setRuleBoolean(Game.RULE_GHOSTS_ENABLED, true)
                    args.sendInfo("Ghosts enabled")
                }
            }
            it.params(argLiteral("disable")) {
                it.actionWithContext { args, context ->
                    if (properties != GameProperties.Macander) {
                        args.sendError("Cannot manage ghosts for this arena")
                        return@actionWithContext
                    }
                    game.setRuleBoolean(Game.RULE_GHOSTS_ENABLED, false)
                    args.sendInfo("Ghosts disabled")
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

}

private val game get() = Game.getGame()
private val properties get() = Game.getGameProperties()

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.player(context: GameContext, name: String = "player")
    = context.getPlayer(this.get<EntitySelector>(name).getPlayer(this.context.source))

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.players(context: GameContext, name: String = "players")
    = context.getPlayers(this.get<EntitySelector>(name).getPlayers(this.context.source))
