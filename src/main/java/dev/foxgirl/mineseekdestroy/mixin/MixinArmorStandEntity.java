package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.service.TemporalGearService;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorStandEntity.class)
public abstract class MixinArmorStandEntity {

    @Unique
    private boolean mineseekdestroy$handleBroken(DamageSource source) {
        var self = (ArmorStandEntity) (Object) this;
        if (TemporalGearService.isTemporalStand(self)) {
            var context = Game.getGame().getContext();
            if (context != null) {
                context.temporalGearService.handleArmorStandBroken(self, source);
            }
            return true;
        }
        return false;
    }

    @Inject(method = "onBreak", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookOnBreak(DamageSource source, CallbackInfo info) {
        if (mineseekdestroy$handleBroken(source)) info.cancel();
    }

    @Inject(method = "breakAndDropItem", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookBreakAndDropItem(DamageSource source, CallbackInfo info) {
        if (mineseekdestroy$handleBroken(source)) info.cancel();
    }

}
