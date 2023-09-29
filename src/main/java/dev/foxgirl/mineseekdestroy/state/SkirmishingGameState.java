package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkirmishingGameState extends IdlingGameState {

    @Override
    public @NotNull String getName() {
        return "skirmishing";
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        return true;
    }

}
