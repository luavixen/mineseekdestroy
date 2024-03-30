package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.util.Broadcast;
import dev.foxgirl.mineseekdestroy.util.Rules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkirmishingGameState extends DuelingGameState {

    @Override
    public @NotNull String getName() {
        return "skirmishing";
    }

    @Override
    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        context.game.sendInfo("Skirmish started! FIGHT!");

        context.invisibilityService.executeSetDisabled(Game.CONSOLE_OPERATORS);
        context.barrierService.executeBlimpClose(Game.CONSOLE_OPERATORS);

        context.automationService.handleSkirmishBegin();
        context.countdownService.handleRoundStart();

        Broadcast.sendSoundPing();

        Rules.setSkirmishEnabled(false);

        return null;
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        var players = findPlayers(context);

        if (players.size() < 2) {
            if (players.size() == 1) {
                context.game.sendInfo("Skirmish over!", players.get(0).getDisplayName(), "wins!");
                context.automationService.handleSkirmishEnd(players.get(0));
            } else {
                context.game.sendInfo("Skirmish over! Nobody wins!");
                context.automationService.handleSkirmishEnd(null);
            }

            context.lootService.handleRoundEnd();
            context.summonsService.handleRoundEnd();
            context.soulService.handleRoundEnd();
            context.countdownService.handleRoundEnd();

            return new FinalizingGameState();
        }

        return null;
    }

}
