package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinFarmlandBlock modifies farmland to disable the trampling mechanic.
 */
@Mixin(FarmlandBlock.class)
public abstract class MixinFarmlandBlock extends Block {

    protected MixinFarmlandBlock(Settings settings) {
        super(settings);
    }

    @Inject(method = "onLandedUpon", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookOnLandedUpon(World world, BlockState blockState, BlockPos blockPos, Entity entity, float fallDistance, CallbackInfo info) {
        super.onLandedUpon(world, blockState, blockPos, entity, fallDistance);
        info.cancel();
    }

}
