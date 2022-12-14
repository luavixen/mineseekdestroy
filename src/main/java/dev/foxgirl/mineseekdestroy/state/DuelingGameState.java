package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.entity.damage.DamageSource;
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

        context.barrierService.executeArenaClose(Game.CONSOLE_OPERATORS);

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        var players = findPlayers(context);

        if (players.size() < 2) {
            if (players.size() == 1) {
                Game.getGame().sendInfo("Duel over!", players.get(0).getDisplayName(), "wins!");
            } else {
                Game.getGame().sendInfo("Duel over! Both players died at the exact same time, nobody wins!");
            }

            context.barrierService.executeArenaOpen(Game.CONSOLE_OPERATORS);

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
            if (!Game.getGameProperties().getRegionPlayable().contains(entity)) continue;
            list.add(entity);
        }
        return list;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            return player.getTeam() == GameTeam.PLAYER_DUEL;
        }
        return true;
    }

}
