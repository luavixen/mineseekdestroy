package dev.foxgirl.mineseekdestroy.command

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.*
import net.minecraft.command.EntitySelector
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.Position

internal fun setup() {

    Command.build("game") {
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
    }

    Command.build("begin") {
        it.params(argLiteral("round")) {
            it.actionWithContext { args, context ->
                Game.getGame().setState(StartingGameState())
            }
        }
        it.params(argLiteral("duel")) {
            it.actionWithContext { args, context ->
                Game.getGame().setState(DuelingGameState())
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
                    val players = args.players().onEach { it.team = team }
                    args.sendInfo("Updated team for ${players.size} player(s) to", team.nameColored)
                }
            }
        }
        register("none", GameTeam.NONE)
        register("operator", GameTeam.OPERATOR)
        register("black", GameTeam.PLAYER_BLACK)
        register("yellow", GameTeam.PLAYER_YELLOW)
        register("blue", GameTeam.PLAYER_BLUE)
    }

    Command.build("stat") {
        it.params(argLiteral("setalive"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players().onEach { it.isAlive = true }
                args.sendInfo("Updated alive status for ${players.size} player(s) to living")
            }
        }
        it.params(argLiteral("setdead"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players().onEach { it.isAlive = false }
                args.sendInfo("Updated alive status for ${players.size} player(s) to dead")
            }
        }
        it.params(argLiteral("setkills"), argInt("kills"), argPlayers()) {
            it.actionWithContext { args, context ->
                val kills: Int = args["kills"]
                val players = args.players().onEach { it.kills = kills }
                args.sendInfo("Updated kill count for ${players.size} player(s)")
            }
        }
        it.params(argLiteral("takekill"), argPlayers()) {
            it.actionWithContext { args, context ->
                val players = args.players().onEach { it.kills-- }
                args.sendInfo("Updated kill count for ${players.size} player(s)")
            }
        }
    }

    Command.build("tp") {
        fun register(literal: String, position: Position) {
            it.params(argLiteral(literal)) {
                it.params(argLiteral("force")) {
                    it.params(argPlayers()) {
                        it.actionWithContext { args, context ->
                            val players = args.players().onEach { it.teleport(position) }
                            args.sendInfo("Teleported ${players.size} player(s) to '${literal}' forcefully")
                        }
                    }
                    it.actionWithContext { args, context ->
                        val players = context.players.onEach { it.teleport(position) }
                        args.sendInfo("Teleported ${players.size} player(s) to '${literal}' forcefully")
                    }
                }
                it.params(argPlayers()) {
                    it.actionWithContext { args, context ->
                        val players = args.players().onEach { if (it.isPlaying) it.teleport(position) }
                        args.sendInfo("Teleported ${players.size} player(s) to '${literal}'")
                    }
                }
                it.actionWithContext { args, context ->
                    val players = context.players.onEach { if (it.isPlaying) it.teleport(position) }
                    args.sendInfo("Teleported ${players.size} player(s) to '${literal}'")
                }
            }
        }
        register("blimp", Game.POSITION_BLIMP)
        register("arena", Game.POSITION_ARENA)
        register("duel1", Game.POSITION_DUEL1)
        register("duel2", Game.POSITION_DUEL2)
    }

    Command.build("inv") {
        it.params(argLiteral("clear")) {
            it.params(argPlayers()) {
                it.actionWithContext { args, context ->
                    context.inventoryService.executeClear(args, args.players())
                }
            }
            it.actionWithContext { args, context ->
                context.inventoryService.executeClear(args)
            }
        }
        it.params(argLiteral("fill")) {
            it.params(argPlayers()) {
                it.actionWithContext { args, context ->
                    context.inventoryService.executeFill(args, args.players())
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

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.players(name: String = "players"): List<GamePlayer> {
    val context = Game.getGame().context!!
    return context.getPlayers(this.get<EntitySelector>(name).getPlayers(this.context.source))
}
