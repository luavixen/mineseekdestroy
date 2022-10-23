package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
    private ActionResult mineseekdestroy$hookInteractBlock$1(ItemStack itemStack, ItemUsageContext itemUsageContext) {
        var result = ExtraEvents.BLOCK_PLACED.invoker().handle(
            itemUsageContext.getPlayer(),
            itemUsageContext.getWorld(),
            itemUsageContext.getHand(),
            new BlockHitResult(
                itemUsageContext.getHitPos(),
                itemUsageContext.getSide(),
                itemUsageContext.getBlockPos(),
                itemUsageContext.hitsInsideBlock()
            ),
            itemStack
        );
        if (result != ActionResult.PASS) return result;
        return itemStack.useOnBlock(itemUsageContext);
    }

}
