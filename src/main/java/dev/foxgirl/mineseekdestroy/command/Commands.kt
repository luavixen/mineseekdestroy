package dev.foxgirl.mineseekdestroy.command

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameContext
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.*
import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.command.EntitySelector
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.Position

internal fun setup() {

    Command.build("game") {
        it.params(argLiteral("stop")) {
            it.action { args ->
                val game = Game.getGame()
                if (game.context != null) {
                    game.destroy()
                    args.sendInfo("Stopped game")
                } else {
                    args.sendError("Cannot stop game, no game running")
                }
            }
        }
        it.params(argLiteral("start")) {
            it.action { args ->
                val game = Game.getGame()
                if (game.context == null) {
                    game.initialize()
                    args.sendInfo("Started new game")
                } else {
                    args.sendError("Cannot start new game, already running")
                }
            }
        }
        it.params(argLiteral("prepare")) {
            it.actionWithContext { args, context ->
                args.sendInfo("Resetting and preparing game state")
                context.barrierService.executeArenaOpen(args)
                context.barrierService.executeBlimpClose(args)
                context.lootService.executeClear(args)
                context.inventoryService.executeClear(args)
                context.players.forEach { if (!it.isOperator) it.teleport(Game.POSITION_BLIMP) }
            }
        }
    }

    Command.build("begin") {
        it.params(argLiteral("round")) {
            it.actionWithContext { args, context ->
                context.game.state = StartingGameState()
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
                    player1.teleport(Game.POSITION_DUEL1)
                    player2.teleport(Game.POSITION_DUEL2)

                    context.game.state = DuelingGameState()
                }
            }
            it.actionWithContext { args, context ->
                context.game.state = DuelingGameState()
            }
        }
    }

    Command.build("end") {
        it.action { args ->
            Game.getGame().setState(WaitingGameState())
            args.sendInfo("Reset current game state")
        }
    }

    Command.build("state") {
        fun register(literal: String, state: () -> GameState) {
            it.params(argLiteral(literal)) {
                it.action { args ->
                    Game.getGame().setState(state())
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

    Command.build("team") {
        fun register(literal: String, team: GameTeam) {
            it.params(argLiteral(literal), argPlayers()) {
                it.actionWithContext { args, context ->
                    val players = args.players(context).onEach { it.team = team }
                    args.sendInfo("Updated team for ${players.size} player(s) to", team.nameColored)
                }
            }
        }
        register("none", GameTeam.NONE)
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
        fun register(literal: String, position: Position, region: Region) {
            fun teleport(console: Console, players: List<GamePlayer>, filter: (GamePlayer) -> Boolean = { true }) {
                players.filter(filter).let {
                    it.forEach { player -> player.teleport(position) }
                    console.sendInfo("Teleported ${it.size} player(s) to '${literal}'")
                }
            }

            fun isPlaying(player: GamePlayer) = player.isPlaying
            fun isInRegion(player: GamePlayer) = player.entity?.let(region::contains) ?: false

            it.params(argLiteral(literal)) {
                it.params(argLiteral("area")) {
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context -> teleport(args, args.players(context), ::isInRegion) }
                    }
                    it.actionWithContext { args, context -> teleport(args, context.players, ::isInRegion) }
                }
                it.params(argLiteral("force")) {
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context -> teleport(args, args.players(context)) }
                    }
                    it.actionWithContext { args, context -> teleport(args, context.players) }
                }
                it.params(argPlayers()) {
                    it.actionWithContext { args, context -> teleport(args, args.players(context), ::isPlaying) }
                }
                it.actionWithContext { args, context -> teleport(args, context.players, ::isPlaying) }
            }
        }
        register("blimp", Game.POSITION_BLIMP, Game.REGION_BLIMP)
        register("arena", Game.POSITION_ARENA, Game.REGION_PLAYABLE)
        register("duel1", Game.POSITION_DUEL1, Game.REGION_PLAYABLE)
        register("duel2", Game.POSITION_DUEL2, Game.REGION_PLAYABLE)
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

}

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.player(context: GameContext, name: String = "player")
    = context.getPlayer(this.get<EntitySelector>(name).getPlayer(this.context.source))

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.players(context: GameContext, name: String = "players")
    = context.getPlayers(this.get<EntitySelector>(name).getPlayers(this.context.source))

