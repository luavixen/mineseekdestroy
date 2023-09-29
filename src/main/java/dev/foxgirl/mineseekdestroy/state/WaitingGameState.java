package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.entity.effect.StatusEffects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaitingGameState extends IdlingGameState {

    @Override
    public @NotNull String getName() {
        return "waiting";
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        for (var playerPair : context.getPlayerEntitiesNormal()) {
            var playerEntity = playerPair.getSecond();
            if (playerEntity.isDead() || playerEntity.isRemoved()) continue;
            playerEntity.setHealth(playerEntity.getMaxHealth());
            var hungerManager = playerEntity.getHungerManager();
            hungerManager.setFoodLevel(20);
            hungerManager.setSaturationLevel(5.0F);
            hungerManager.setExhaustion(0.0F);
            playerEntity.removeStatusEffect(StatusEffects.WITHER);
            playerEntity.removeStatusEffect(StatusEffects.POISON);
            playerEntity.removeStatusEffect(StatusEffects.SLOWNESS);
            playerEntity.removeStatusEffect(StatusEffects.HUNGER);
            playerEntity.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        return null;
    }

}
