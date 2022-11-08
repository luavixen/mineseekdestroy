package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.GameContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinalizingGameState extends RunningGameState {

    private int ticks = 0;

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        if (ticks < 20) {
            ticks++;
        } else {
            return new WaitingGameState();
        }

        return null;
    }

}
