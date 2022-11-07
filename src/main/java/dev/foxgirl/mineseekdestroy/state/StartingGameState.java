package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.GameContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StartingGameState extends GameState {

    @Override
    protected GameState onSetup(@NotNull GameContext context) {
        // TODO: Implement starting setup behaviour (slow fall effect, initialize countdown)
        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        // TODO: Implement starting update behaviour (update countdown, switch to playing when countdown ends)
        return null;
    }
}
