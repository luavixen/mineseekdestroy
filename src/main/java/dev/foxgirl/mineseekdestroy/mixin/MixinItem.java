package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinItem {

    @Unique
    private static final Integer mineseekdestroy$defaultMaxCount = Item.DEFAULT_MAX_COUNT;

    @Inject(method = "getMaxCount()I", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookGetMaxCount(CallbackInfoReturnable<Integer> info) {
        if (Game.STACKABLE_ITEMS.contains(this)) {
            info.setReturnValue(mineseekdestroy$defaultMaxCount);
        }
    }

}
