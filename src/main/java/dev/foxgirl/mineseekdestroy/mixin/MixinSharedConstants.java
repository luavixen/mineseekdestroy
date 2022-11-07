package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SharedConstants.class)
public abstract class MixinSharedConstants {

    @Inject(method = "isValidChar(C)Z", at = @At("HEAD"), cancellable = true)
    private static void mineseekdestroy$hookIsValidChar(char value, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(true);
    }

}
