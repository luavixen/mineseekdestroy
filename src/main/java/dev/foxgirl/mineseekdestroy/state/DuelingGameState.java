package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DuelingGameState extends RunningGameState {

    @Override
    protected GameState onSetup(@NotNull GameContext context) {
        // TODO: Announce duel start
        return null;
    }

    @Override
    protected GameState onUpdate(@NotNull GameContext context) {
        var playerEntities = new ArrayList<ServerPlayerEntity>();

        for (var player : context.getPlayers()) {
            if (!player.isPlaying() || !player.isAlive()) continue;
            var entity = player.getEntity();
            if (entity == null || !entity.isAlive()) continue;
            if (entity.getWorld() != context.world) continue;
            if (!Game.REGION_PLAYABLE.contains(entity.getPos())) continue;
            playerEntities.add(entity);
        }

        if (playerEntities.size() < 1) {
            Game.getGame().sendInfo("Duel over! Both players died at the exact same time, nobody wins!");
            return new FinalizingGameState();
        }
        if (playerEntities.size() < 2) {
            Game.getGame().sendInfo("Duel over!", playerEntities.get(0).getDisplayName(), "wins!");
            return new FinalizingGameState();
        }

        return null;
    }

}
