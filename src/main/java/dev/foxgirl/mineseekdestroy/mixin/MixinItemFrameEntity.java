package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class MixinItemFrameEntity {

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (!source.isSourceCreativePlayer()) {
            info.setReturnValue(false);
        }
    }

}
