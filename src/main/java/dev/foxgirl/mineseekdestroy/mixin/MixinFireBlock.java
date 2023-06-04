package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FireBlock.class)
public abstract class MixinFireBlock extends AbstractFireBlock {

    private MixinFireBlock(Settings settings, float damage) {
        super(settings, damage);
    }

    private static boolean mineseekdestroy$flameActive(BlockState state) {
        var game = Game.getGame();

        var context = game.getContext();
        if (context == null) return false;

        var block = state.getBlock();

        if (context.specialSummonsService.isScaldingEarth()) {
            return !game.getProperties().getInflammableBlocks().contains(block);
        } else {
            return block == Blocks.TNT;
        }
    }

    @Overwrite
    private int getSpreadChance(BlockState state) {
        return mineseekdestroy$flameActive(state) ? 5 : 0;
    }

    @Overwrite
    private int getBurnChance(BlockState state) {
        return mineseekdestroy$flameActive(state) ? 20 : 0;
    }

}
