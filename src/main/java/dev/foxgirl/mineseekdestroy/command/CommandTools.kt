package dev.foxgirl.mineseekdestroy.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameContext
import dev.foxgirl.mineseekdestroy.util.Console
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Builds and adds a sequence of [ArgumentBuilder]s as parameters to this
 * builder.
 *
 * @param builders Sequence of builders to add as parameters.
 * @param callback
 *   Function to invoke with the last builder in the sequence before building,
 *   will be invoked with this builder if [builders] is empty.
 */
fun <S, T : ArgumentBuilder<S, *>> T.params(
    vararg builders: ArgumentBuilder<S, *>,
    callback: (ArgumentBuilder<S, *>) -> Unit
): T {
    val list = mutableListOf<ArgumentBuilder<S, *>>()
    list.add(this)
    list.addAll(builders)
    list.reverse()

    val iter = list.iterator()
    var prev = iter.next().also(callback)

    while (iter.hasNext()) {
        prev = iter.next().also { it.then(prev.build()) }
    }

    return this
}

/**
 * Registers an action on this [ArgumentBuilder] that calls [callback].
 *
 * Note that if an action was already registered through one of the .action
 * extensions or the [ArgumentBuilder.executes] method then the existing
 * registration will be overwritten.
 *
 * @param callback Function to invoke when this command is executed.
 */
fun <S : ServerCommandSource, T : ArgumentBuilder<S, *>> T.actionUnchecked(callback: (Command.Arguments<S>) -> Unit): T {
    executes { context -> Command.Arguments(context).let { callback(it); it.result } }
    return this
}

/**
 * Registers an action on this [ArgumentBuilder] that, when the command is
 * executed:
 *  - Asserts that the command's source is an operator.
 *  - Calls [callback] with the command's arguments, handling and reporting
 *    exceptions.
 *
 * Note that if an action was already registered through one of the .action
 * extensions or the [ArgumentBuilder.executes] method then the existing
 * registration will be overwritten.
 *
 * @param callback Function to invoke when this command is executed.
 */
fun <S : ServerCommandSource, T : ArgumentBuilder<S, *>> T.action(callback: (Command.Arguments<S>) -> Unit): T {
    actionUnchecked { args ->
        val entity = args.context.source.entity
        if (entity is PlayerEntity && !Game.getGame().isOperator(entity)) {
            args.sendError("You do not have permission to run this command")
        } else {
            try {
                callback(args)
            } catch (err : CommandSyntaxException) {
                throw err
            } catch (err : Exception) {
                args.sendError(err)
                err.printStackTrace()
                throw RuntimeException("Unhandled exception running command '${args.context.input}'", err)
            }
        }
    }
    return this
}

/**
 * Registers an action on this [ArgumentBuilder] that, when the command is
 * executed:
 *  - Asserts that the command's source is an operator.
 *  - Asserts that a game is running and retrieves the [GameContext].
 *  - Calls [callback] with the command's arguments and game context, handling
 *    and reporting exceptions.
 *
 * Note that if an action was already registered through one of the .action
 * extensions or the [ArgumentBuilder.executes] method then the existing
 * registration will be overwritten.
 *
 * @param callback Function to invoke when this command is executed.
 */
fun <S : ServerCommandSource, T : ArgumentBuilder<S, *>> T.actionWithContext(callback: (Command.Arguments<S>, GameContext) -> Unit): T {
    action { args ->
        val context = Game.getGame().getContext()
        if (context == null) {
            args.sendError("No game is running, cannot run this command")
        } else {
            callback(args, context)
        }
    }
    return this
}

/**
 * Provides various utilities for creating/executing commands and implements
 * the [CommandRegistrationCallback] interface. This object should be
 * registered on the [CommandRegistrationCallback.EVENT] event during mod
 * initialization.
 */
object Command : CommandRegistrationCallback {
    /**
     * Represents the arguments passed to a command's executor. Wrapper around
     * [CommandContext] that provides getters for accessing context argument
     * values.
     *
     * @param S Command source type.
     * @property context Wrapped command context.
     * @constructor Creates a new [Arguments] wrapper around [context].
     */
    class Arguments<S : ServerCommandSource>(val context: CommandContext<S>) : Console {
        /**
         * Result value to return on command execution completion.
         *
         * From the [Fabric wiki](https://fabricmc.net/wiki/tutorial:commands):
         * In Minecraft, the result can correspond to the power of a redstone
         * comparator feeding from a command block or the value that will be
         * passed the chain command block the command block is facing.
         * Typically negative values mean a command has failed and will do
         * nothing. A result of 0 means the command has passed. Positive values
         * mean the command was successful and did something.
         */
        var result = 0

        /**
         * Gets the parsed value of one of the command arguments.
         *
         * @param T Type to cast the parsed value to.
         * @param name Name of the command argument.
         * @return The parsed value of the command argument.
         * @throws IllegalArgumentException
         *   If there are no command arguments with the name [name].
         * @throws ClassCastException
         *   If the parsed value cannot be cast to [T].
         */
        operator fun <T> get(name: String): T {
            @Suppress("UNCHECKED_CAST")
            return context.getArgument(name, Any::class.java) as T
        }

        /**
         * Sends command completion feedback to the command source and server
         * operators.
         *
         * @param values
         *   Message to send as feedback, values will be converted to strings
         *   and joined with spaces.
         */
        override fun sendInfo(vararg values: Any?) {
            val message = values.joinToString(" ")
            Game.getGame().sendInfo(message)
            context.source.sendFeedback(Text.literal("[msd] $message").formatted(Formatting.GRAY), true)
        }

        /**
         * Sends command error feedback to the command source and sets the
         * [result] value to `-1`.
         *
         * @param values
         *   Message to send as feedback, values will be converted to strings
         *   and joined with spaces.
         */
        override fun sendError(vararg values: Any?) {
            val message = values.joinToString(" ")
            Game.getGame().sendError(message)
            context.source.sendError(Text.literal("[msd] $message").formatted(Formatting.RED))
            result = -1
        }
    }

    /**
     * Helper utilities for creating new [ArgumentBuilder] instances for
     * building new commands.
     */
    object Helpers {
        /**
         * Creates a new [LiteralArgumentBuilder].
         *
         * @param name Literal text to match.
         * @return Created builder instance.
         */
        fun argLiteral(name: String)
            : LiteralArgumentBuilder<ServerCommandSource>
            = LiteralArgumentBuilder.literal(name)

        /**
         * Creates a new [RequiredArgumentBuilder] for the given [type].
         *
         * @param name Argument name, used by [CommandArguments.get].
         * @param type Argument type.
         * @return Created builder instance.
         */
        fun <T> argParam(name: String, type: ArgumentType<T>)
            : RequiredArgumentBuilder<ServerCommandSource, T>
            = RequiredArgumentBuilder.argument(name, type)

        fun argBool(name: String)
            = argParam(name, BoolArgumentType.bool())

        fun argInt(name: String)
            = argParam(name, IntegerArgumentType.integer())
        fun argInt(name: String, min: Int)
            = argParam(name, IntegerArgumentType.integer(min))
        fun argInt(name: String, min: Int, max: Int)
            = argParam(name, IntegerArgumentType.integer(min, max))

        fun argLong(name: String)
            = argParam(name, LongArgumentType.longArg())
        fun argLong(name: String, min: Long)
            = argParam(name, LongArgumentType.longArg(min))
        fun argLong(name: String, min: Long, max: Long)
            = argParam(name, LongArgumentType.longArg(min, max))

        fun argFloat(name: String)
            = argParam(name, FloatArgumentType.floatArg())
        fun argFloat(name: String, min: Float)
            = argParam(name, FloatArgumentType.floatArg(min))
        fun argFloat(name: String, min: Float, max: Float)
            = argParam(name, FloatArgumentType.floatArg(min, max))

        fun argDouble(name: String)
            = argParam(name, DoubleArgumentType.doubleArg())
        fun argDouble(name: String, min: Double)
            = argParam(name, DoubleArgumentType.doubleArg(min))
        fun argDouble(name: String, min: Double, max: Double)
            = argParam(name, DoubleArgumentType.doubleArg(min, max))

        fun argWord(name: String)
            = argParam(name, StringArgumentType.word())
        fun argString(name: String)
            = argParam(name, StringArgumentType.string())
        fun argGreedyString(name: String)
            = argParam(name, StringArgumentType.greedyString())

        fun argEntity(name: String = "entity")
            = argParam(name, EntityArgumentType.entity())
        fun argEntities(name: String = "entities")
            = argParam(name, EntityArgumentType.entities())
        fun argPlayer(name: String = "player")
            = argParam(name, EntityArgumentType.player())
        fun argPlayers(name: String = "players")
            = argParam(name, EntityArgumentType.players())
    }

    private lateinit var commandDispatcher: CommandDispatcher<ServerCommandSource>
    private lateinit var commandNode: CommandNode<ServerCommandSource>

    override fun register(
        dispatcher: CommandDispatcher<ServerCommandSource>,
        registry: CommandRegistryAccess,
        environment: CommandManager.RegistrationEnvironment
    ) {
        commandDispatcher = dispatcher
        commandNode = dispatcher.register(Helpers.argLiteral("msd"))
        setup()
    }

    /**
     * Registers a new /msd subcommand.
     *
     * @param node Subcommand node to add to the /msd command.
     */
    fun add(node: CommandNode<ServerCommandSource>) {
        commandNode.addChild(node)
    }

    /**
     * Registers a new /msd subcommand.
     *
     * @param builder
     *   Subcommand builder to build and to add to the /msd command.
     */
    fun add(builder: ArgumentBuilder<ServerCommandSource, *>) {
        add(builder.build())
    }

    /**
     * Builds a new /msd subcommand.
     *
     * @param name Subcommand name to be used (eg. /msd <name>).
     * @param block
     *   Function to invoke with new [LiteralArgumentBuilder] with [Helpers] as
     *   the receiver.
     */
    fun build(
        name: String,
        block: Helpers.(LiteralArgumentBuilder<ServerCommandSource>) -> Unit
    ) {
        add(Helpers.argLiteral(name).also { block(Helpers, it) })
    }
}
