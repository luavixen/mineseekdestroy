package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {

    @Redirect(
        method = "interactBlock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;")
    )
    private ActionResult mineseekdestroy$hookInteractBlock$0(BlockState blockState, World world, PlayerEntity player, Hand hand, BlockHitResult blockHit) {
        var result = ExtraEvents.BLOCK_USED.invoker().handle(player, world, hand, blockHit, blockState);
        if (result != ActionResult.PASS) return result;
        return blockState.onUse(world, player, hand, blockHit);
    }

    @Redirect(
        method = "interactBlock",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;")
    )
    private ActionResult mineseekdestroy$hookInteractBlock$1(ItemStack stack, ItemUsageContext usageContext) {
        var result = ExtraEvents.BLOCK_USED_WITH.invoker().handle(
            usageContext.player,
            usageContext.world,
            usageContext.hand,
            usageContext.hit,
            stack
        );
        if (result != ActionResult.PASS) return result;
        return stack.useOnBlock(usageContext);
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookInteractItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> info) {
        var result = ExtraEvents.ITEM_USED.invoker().handle(player, world, hand, stack);
        if (result != ActionResult.PASS) info.setReturnValue(result);
    }

}
