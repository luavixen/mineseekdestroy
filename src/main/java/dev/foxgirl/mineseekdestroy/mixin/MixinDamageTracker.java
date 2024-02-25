package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageTracker.class)
public abstract class MixinDamageTracker {

    @Shadow @Final
    private LivingEntity entity;

    @Inject(method = "onDamage", at = @At("TAIL"))
    private void mineseekdestroy$hookOnDamage(DamageSource source, float amount, CallbackInfo info) {
        var context = Game.getGame().getContext();
        if (context != null) {
            context.damageService.handleDamage(entity, source, amount);
        }
    }

}
