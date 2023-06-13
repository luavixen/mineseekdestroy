package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.service.ItemService;
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
        if (stack.getItem() == Items.BONE_BLOCK) {
            stack.setNbt(Objects.requireNonNull(Game.stackEggBlock.getNbt()).copy());
        }
    }

}
