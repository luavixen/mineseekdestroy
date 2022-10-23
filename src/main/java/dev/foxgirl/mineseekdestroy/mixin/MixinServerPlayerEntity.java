package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity {

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookDamage(DamageSource damageSource, float damageAmount, CallbackInfoReturnable<Boolean> info) {
        if (!ExtraEvents.PLAYER_DAMAGED.invoker().handle((ServerPlayerEntity) (Object) this, damageSource, damageAmount)) {
            info.setReturnValue(false);
        }
    }

}
