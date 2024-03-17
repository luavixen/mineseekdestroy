package dev.foxgirl.mineseekdestroy.service;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GamePlayer;
import dev.foxgirl.mineseekdestroy.GameProperties;
import dev.foxgirl.mineseekdestroy.state.GameState;
import dev.foxgirl.mineseekdestroy.util.Console;
import kotlin.Pair;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public abstract class Service {

    private GameContext context;

    public final void initialize(@NotNull GameContext context) {
        Objects.requireNonNull(context, "Argument 'context'");
        if (this.context == null) {
            this.context = context;
        } else {
            throw new IllegalStateException("Service already initialized");
        }
        setup();
    }

    protected void setup() {
    }

    public void update() {
    }

    protected final @NotNull GameContext getContext() {
        var context = this.context;
        if (context == null) {
            throw new IllegalStateException("Cannot access context before service initialization");
        }
        return context;
    }

    protected final @NotNull MinecraftServer getServer() {
        return getContext().server;
    }

    protected final @NotNull ServerWorld getWorld() {
        return getContext().world;
    }

    protected final @NotNull List<@NotNull GamePlayer> getPlayers() {
        return getContext().getPlayers();
    }
    protected final @NotNull List<@NotNull GamePlayer> getPlayersNormal() {
        return getContext().getPlayersNormal();
    }
    protected final @NotNull List<@NotNull GamePlayer> getPlayersIn() {
        return getContext().getPlayersIn();
    }
    protected final @NotNull List<@NotNull GamePlayer> getPlayersOut() {
        return getContext().getPlayersOut();
    }
    protected final @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntities() {
        return getContext().getPlayerEntities();
    }
    protected final @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntitiesNormal() {
        return getContext().getPlayerEntitiesNormal();
    }
    protected final @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntitiesIn() {
        return getContext().getPlayerEntitiesIn();
    }
    protected final @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntitiesOut() {
        return getContext().getPlayerEntitiesOut();
    }

    protected final @NotNull Game getGame() {
        return Game.getGame();
    }

    protected final @NotNull GameProperties getProperties() {
        return getGame().getProperties();
    }

    protected final @NotNull GameState getState() {
        return getGame().getState();
    }

    protected final void setState(@NotNull GameState state) {
        getGame().setState(state);
    }

    protected final @NotNull Logger getLogger() {
        return Game.LOGGER;
    }

    protected final @NotNull Console getConsolePlayers() {
        return Game.CONSOLE_PLAYERS;
    }
    protected final @NotNull Console getConsoleOperators() {
        return Game.CONSOLE_OPERATORS;
    }

}
