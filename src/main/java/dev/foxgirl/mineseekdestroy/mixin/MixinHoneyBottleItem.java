package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.HoneyBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoneyBottleItem.class)
public abstract class MixinHoneyBottleItem {

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookFinishUsing(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> info) {
        if (user instanceof ServerPlayerEntity playerEntity) {
            playerEntity.getHungerManager().setFoodLevel(20);
            playerEntity.getHungerManager().setSaturationLevel(5.0F);
            playerEntity.getHungerManager().setExhaustion(0.0F);
        }
    }

}
