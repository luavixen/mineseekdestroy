package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.GameItems;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * MixinCraftingResultInventory modifies the crafting system so that it renames
 * all bone blocks to "Egg Block".
 */
@Mixin(CraftingResultInventory.class)
public abstract class MixinCraftingResultInventory {

    @Inject(method = "setStack", at = @At("HEAD"))
    private void mineseekdestroy$hookSetStack(int slot, ItemStack stack, CallbackInfo info) {
        if (stack.getItem() == Items.WRITTEN_BOOK) {
            stack.setNbt(Objects.requireNonNull(GameItems.INSTANCE.getBookCobbled().getNbt()).copy());
        } else {
            GameItems.replace(stack);
        }
    }

}
