package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public class PlayingGameState extends RunningGameState {

    @Override
    public @NotNull String getName() {
        return "playing";
    }

    @Override
    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        context.game.sendInfo("Round started! KILL!");

        context.invisibilityService.executeSetDisabled(Game.CONSOLE_OPERATORS);
        context.barrierService.executeBlimpClose(Game.CONSOLE_OPERATORS);

        context.automationService.handleRoundBegin();

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        Set<GameTeam> teamsCurrentlyAlive = EnumSet.noneOf(GameTeam.class);

        for (var player : context.getPlayersIn()) {
            var team = player.getTeam();
            if (team.isCannon()) teamsCurrentlyAlive.add(team);
        }

        switch (teamsCurrentlyAlive.size()) {
            case 1 -> {
                var team = teamsCurrentlyAlive.iterator().next();
                context.game.sendInfo("Round over!", team.getDisplayName(), "wins!");
                context.automationService.handleRoundEnd(team == GameTeam.PLAYER_YELLOW ? GameTeam.PLAYER_BLUE : GameTeam.PLAYER_YELLOW);
            }
            case 0 -> {
                context.game.sendInfo("Round over! Both teams died at the exact same time, nobody wins!");
            }
            default -> {
                return null;
            }
        }

        context.lootService.handleRoundEnd();
        context.summonsService.handleRoundEnd();
        context.soulService.handleRoundEnd();

        return new FinalizingGameState();
    }

}
