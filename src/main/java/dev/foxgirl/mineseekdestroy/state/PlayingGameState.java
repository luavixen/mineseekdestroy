package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import org.jetbrains.annotations.NotNull;

public class PlayingGameState extends RunningGameState {

    @Override
    protected GameState onSetup(@NotNull GameContext context) {
        // TODO: Announce round start
        return null;
    }

    @Override
    protected GameState onUpdate(@NotNull GameContext context) {
        int aliveYellow = 0;
        int aliveBlue = 0;

        for (var player : context.getPlayers()) {
            if (!player.isPlaying() || !player.isAlive()) continue;
            switch (player.getTeam()) {
                case PLAYER_YELLOW -> aliveYellow++;
                case PLAYER_BLUE -> aliveBlue++;
            }
        }

        if (aliveYellow == 0 && aliveBlue == 0) {
            Game.getGame().sendInfo("Round over! Both teams died at the exact same time, nobody wins!");
            return new FinalizingGameState();
        }
        if (aliveYellow == 0) {
            Game.getGame().sendInfo("Round over!", GameTeam.PLAYER_BLUE.getNameColored(), "wins!");
            return new FinalizingGameState();
        }
        if (aliveBlue == 0) {
            Game.getGame().sendInfo("Round over!", GameTeam.PLAYER_YELLOW.getNameColored(), "wins!");
            return new FinalizingGameState();
        }

        return null;
    }

}
