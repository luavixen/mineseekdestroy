package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.GameItems;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinCraftingResultInventory modifies the crafting system so that it renames
 * all bone blocks to "Egg Block".
 */
@Mixin(CraftingResultInventory.class)
public abstract class MixinCraftingResultInventory {

    @Inject(method = "setStack", at = @At("HEAD"))
    private void mineseekdestroy$hookSetStack(int slot, ItemStack stack, CallbackInfo info) {
        GameItems.replace(stack);
    }

}
