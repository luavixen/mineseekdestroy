package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class MixinFireBlock extends AbstractFireBlock {

    private MixinFireBlock(Settings settings, float damage) {
        super(settings, damage);
    }

    private static boolean mineseekdestroy$flameActive(BlockState state) {
        var game = Game.getGame();

        var context = game.getContext();
        if (context != null && context.specialSummonsService.isScaldingEarth()) {
            var ignored = game.getProperties().getInflammableBlocks();
            if (ignored.contains(state.getBlock())) return false;
            return true;
        }

        return false;
    }

    @Inject(method = "getSpreadChance", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookGetSpreadChance(BlockState state, CallbackInfoReturnable<Integer> info) {
        if (mineseekdestroy$flameActive(state)) {
            info.setReturnValue(5);
        }
    }

    @Inject(method = "getBurnChance", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookGetBurnChance(BlockState state, CallbackInfoReturnable<Integer> info) {
        if (mineseekdestroy$flameActive(state)) {
            info.setReturnValue(20);
        }
    }

}
