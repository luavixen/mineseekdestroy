package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DuelingGameState extends RunningGameState {

    @Override
    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        var players = findPlayers(context);

        if (players.size() >= 2) {
            var player1 = players.get(0).getDisplayName();
            var player2 = players.get(1).getDisplayName();
            var text = Text.empty().append(player1).append(" VS ").append(player2).append("!");
            Game.getGame().sendInfo("Duel started!", text);
        } else {
            Game.getGame().sendInfo("Duel started!");
        }

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        var players = findPlayers(context);

        if (players.size() == 0) {
            Game.getGame().sendInfo("Duel over! Both players died at the exact same time, nobody wins!");
            return new FinalizingGameState();
        }
        if (players.size() == 1) {
            Game.getGame().sendInfo("Duel over!", players.get(0).getDisplayName(), "wins!");
            return new FinalizingGameState();
        }

        return null;
    }

    private List<ServerPlayerEntity> findPlayers(GameContext context) {
        var list = new ArrayList<ServerPlayerEntity>();
        for (var player : context.getPlayers()) {
            if (!player.isPlaying() || !player.isAlive()) continue;
            var entity = player.getEntity();
            if (entity == null || !entity.isAlive()) continue;
            if (entity.getWorld() != context.world) continue;
            if (!Game.REGION_PLAYABLE.contains(entity.getPos())) continue;
            list.add(entity);
        }
        return list;
    }

}
