package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayingGameState extends RunningGameState {

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
        int aliveYellow = 0;
        int aliveBlue = 0;

        for (var player : context.getPlayersIn()) {
            switch (player.getTeam()) {
                case PLAYER_YELLOW -> aliveYellow++;
                case PLAYER_BLUE -> aliveBlue++;
            }
        }

        if (aliveYellow == 0 && aliveBlue == 0) {
            context.game.sendInfo("Round over! Both teams died at the exact same time, nobody wins!");
        } else if (aliveYellow == 0) {
            context.game.sendInfo("Round over!", GameTeam.PLAYER_BLUE.getNameColored(), "wins!");
            context.automationService.handleRoundEnd(GameTeam.PLAYER_YELLOW);
        } else if (aliveBlue == 0) {
            context.game.sendInfo("Round over!", GameTeam.PLAYER_YELLOW.getNameColored(), "wins!");
            context.automationService.handleRoundEnd(GameTeam.PLAYER_BLUE);
        } else {
            return null;
        }

        context.specialSummonsService.handleRoundEnd();

        return new FinalizingGameState();
    }

}
