package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void mineseekdestroy$hookSetHealth(float health, CallbackInfo info) {
        if (health <= 0) {
            Game.LOGGER.info("Invalid setHealth call for {} with value {}", this, health);
        }
    }

}
