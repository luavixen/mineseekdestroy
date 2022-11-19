package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class MixinPlayerInventory {

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookInsertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        var inventory = (PlayerInventory) (Object) this;
        var player = (ServerPlayerEntity) inventory.player;
        if (!ExtraEvents.ITEM_ACQUIRED.invoker().handle(player, inventory, stack, slot)) {
            info.setReturnValue(false);
        }
    }

}
