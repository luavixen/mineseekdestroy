package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import kotlin.Pair;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FireBlock.class)
public abstract class MixinFireBlock extends AbstractFireBlock {

    private MixinFireBlock(Settings settings, float damage) {
        super(settings, damage);
    }

    @Unique
    private static final Pair<Integer, Integer> mineseekdestroy$CHANCE_NONE = new Pair<>(0, 0);
    @Unique
    private static final Pair<Integer, Integer> mineseekdestroy$CHANCE_WOOD = new Pair<>(5, 20);
    @Unique
    private static final Pair<Integer, Integer> mineseekdestroy$CHANCE_ALWAYS = new Pair<>(1000, 1000);

    @Unique
    private static Pair<Integer, Integer> mineseekdestroy$flameChance(BlockState state) {
        var game = Game.getGame();

        var context = game.getContext();
        if (context == null) return mineseekdestroy$CHANCE_NONE;

        var block = state.getBlock();

        if (context.specialSummonsService.isScaldingEarth()) {
            return game.getProperties().getInflammableBlocks().contains(block)
                ? mineseekdestroy$CHANCE_NONE
                : mineseekdestroy$CHANCE_WOOD;
        } else {
            return block != Blocks.TNT
                ? mineseekdestroy$CHANCE_NONE
                : mineseekdestroy$CHANCE_ALWAYS;
        }
    }

    @Overwrite
    private int getSpreadChance(BlockState state) {
        return mineseekdestroy$flameChance(state).component1();
    }

    @Overwrite
    private int getBurnChance(BlockState state) {
        return mineseekdestroy$flameChance(state).component2();
    }

}
