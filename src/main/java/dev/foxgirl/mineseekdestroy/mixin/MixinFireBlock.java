package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import kotlin.Pair;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if (block == Blocks.TNT) return mineseekdestroy$CHANCE_ALWAYS;

        if (context.specialSummonsService.isScaldingEarth()) {
            // TODO: Remove this change/check after the champions game
            return game.getProperties().getInflammableBlocks().contains(block) && !game.getRuleBoolean(Game.RULE_CHAOS_ENABLED)
                ? mineseekdestroy$CHANCE_NONE
                : mineseekdestroy$CHANCE_WOOD;
        }

        return mineseekdestroy$CHANCE_NONE;
    }

    @Overwrite
    private int getSpreadChance(BlockState state) {
        return mineseekdestroy$flameChance(state).component1();
    }

    @Overwrite
    private int getBurnChance(BlockState state) {
        return mineseekdestroy$flameChance(state).component2();
    }

    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookScheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo info) {
        var properties = Game.getGameProperties();
        if (
            properties.getRegionPlayable().excludes(pos) ||
            properties.getRegionBlimp().contains(pos) ||
            properties.getRegionBlimpBalloons().contains(pos)
        ) {
            world.removeBlock(pos, false);
            info.cancel();
        }
    }

}
