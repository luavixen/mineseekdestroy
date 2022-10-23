package dev.foxgirl.mineseekdestroy.command

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
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
                    game.initialize()
                    args.sendInfo("Stopped game")
                } else {
                    args.sendError("Cannot stop game, no game running")
                }
            }
        }
    }

    Command.build("team") {
        fun register(literal: String, team: GameTeam) {
            it.params(argLiteral(literal), argPlayers()) {
                it.actionWithContext { args, context ->
                    val players = args.players().onEach { it.team = team }
                    args.sendInfo("Update team for ${players.size} player(s) to '${literal}'")
                }
            }
        }
        register("none", GameTeam.NONE)
        register("operator", GameTeam.OPERATOR)
        register("black", GameTeam.PLAYER_BLACK)
        register("yellow", GameTeam.PLAYER_YELLOW)
        register("blue", GameTeam.PLAYER_BLUE)
    }

    Command.build("tp") {
        fun register(literal: String, position: Position) {
            it.params(argLiteral(literal)) {
                it.params(argLiteral("force"), argPlayers()) {
                    it.actionWithContext { args, context ->
                        val players = args.players().onEach { it.teleport(position) }
                        args.sendInfo("Teleported ${players.size} player(s) to '${literal}' forcefully")
                    }
                }
                it.params(argPlayers()) {
                    it.actionWithContext { args, context ->
                        val players = args.players().onEach { if (it.isPlaying) it.teleport(position) }
                        args.sendInfo("Teleported ${players.size} player(s) to '${literal}'")
                    }
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

}

private fun <S : ServerCommandSource, T : Command.Arguments<S>> T.players(name: String = "players"): List<GamePlayer> {
    val selector: EntitySelector = this["players"]
    val source = this.context.source
    val context = Game.getGame().context!!
    return context.getPlayers(selector.getPlayers(source))
}
