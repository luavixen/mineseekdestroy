package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinalizingGameState extends RunningGameState {

    private final int ticksFinalizing = (int) (Game.getGame().getRuleDouble(Game.RULE_FINALIZING_DURATION) * 20.0);

    private int ticks = 0;

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        if (ticks < ticksFinalizing) {
            ticks++;
        } else {
            return new WaitingGameState();
        }

        return null;
    }

    @Override
    public boolean allowDeath(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        return true;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        return false;
    }

}
