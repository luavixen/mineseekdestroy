package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinalizingGameState extends RunningGameState {

    @Override
    public @NotNull String getName() {
        return "finalizing";
    }

    private final int ticksFinalizing = (int) (Rules.getFinalizingDuration() * 20.0);

    private int ticks = 0;

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        if (ticks < ticksFinalizing) {
            ticks++;
        } else {
            var properties = Game.getGameProperties();
            for (var playerPair : context.getPlayerEntitiesNormal()) {
                var player = playerPair.getFirst();
                var playerEntity = playerPair.getSecond();
                if (
                    properties.getRegionBlimp().excludes(playerEntity) &&
                    properties.getRegionBlimpBalloons().excludes(playerEntity)
                ) {
                    player.teleport(properties.getPositionBlimp());
                }
            }
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
