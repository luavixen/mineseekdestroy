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
        context.countdownService.handleRoundStart();

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        Set<GameTeam> teamsCurrentlyAlive = EnumSet.noneOf(GameTeam.class);

        for (var player : context.getPlayersNormal()) {
            if (player.isAlive() && !player.isUndead()) {
                var team = player.getTeam();
                if (team.isCanon()) teamsCurrentlyAlive.add(team);
            }
        }

        switch (teamsCurrentlyAlive.size()) {
            case 1 -> {
                var team = teamsCurrentlyAlive.iterator().next();
                context.automationService.handleRoundEnd(team == GameTeam.YELLOW ? GameTeam.BLUE : GameTeam.YELLOW);
                context.game.sendInfo("Round over!", team.getDisplayName(), "wins!");
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
        context.countdownService.handleRoundEnd();

        return new FinalizingGameState();
    }

}
