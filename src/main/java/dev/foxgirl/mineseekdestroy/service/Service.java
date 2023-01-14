package dev.foxgirl.mineseekdestroy.service;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GamePlayer;
import dev.foxgirl.mineseekdestroy.GameProperties;
import dev.foxgirl.mineseekdestroy.state.GameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
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

    protected final @NotNull World getWorld() {
        return getContext().world;
    }

    protected final @NotNull List<@NotNull GamePlayer> getPlayers() {
        return getContext().getPlayers();
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

}
