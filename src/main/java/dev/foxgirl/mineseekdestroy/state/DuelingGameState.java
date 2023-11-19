package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GamePlayer;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.util.Broadcast;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DuelingGameState extends RunningGameState {

    @Override
    public @NotNull String getName() {
        return "dueling";
    }

    @Override
    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        var players = findPlayers(context);

        if (players.size() >= 2) {
            var player1 = players.get(0).getDisplayName();
            var player2 = players.get(1).getDisplayName();
            var text = Text.empty().append(player1).append(" VS ").append(player2).append("!");
            context.game.sendInfo("Duel started!", text);
        } else {
            context.game.sendInfo("Duel started!");
        }

        Broadcast.sendSoundPing();

        context.barrierService.executeArenaClose(Game.CONSOLE_OPERATORS);

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        var players = findPlayers(context);

        if (players.size() < 2) {
            if (players.size() == 1) {
                context.game.sendInfo("Duel over!", players.get(0).getDisplayName(), "wins!");
            } else {
                context.game.sendInfo("Duel over! Both players died at the exact same time, nobody wins!");
            }

            context.automationService.handleDuelEnd(players);
            context.barrierService.executeArenaOpen(Game.CONSOLE_OPERATORS);

            return new FinalizingGameState();
        }

        return null;
    }

    private List<GamePlayer> findPlayers(GameContext context) {
        var list = new ArrayList<GamePlayer>(2);
        for (var player : context.getPlayers()) {
            if (player.getTeam() != GameTeam.DUELIST || !player.isAlive()) continue;
            list.add(player);
        }
        return list;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            return player.getTeam() == GameTeam.DUELIST;
        }
        return true;
    }

}
