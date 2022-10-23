package dev.foxgirl.mineseekdestroy.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class ExtraEvents {

    private ExtraEvents() {
    }

    @FunctionalInterface
    public interface PlayerDamaged {
        boolean handle(ServerPlayerEntity player, DamageSource damageSource, float damageAmount);
    }

    @FunctionalInterface
    public interface BlockUsed {
        ActionResult handle(PlayerEntity player, World world, Hand hand, BlockHitResult blockHit, BlockState blockState);
    }

    @FunctionalInterface
    public interface BlockPlaced {
        ActionResult handle(PlayerEntity player, World world, Hand hand, BlockHitResult blockHit, ItemStack blockItemStack);
    }

    public static final @NotNull Event<PlayerDamaged> PLAYER_DAMAGED = EventFactory.createArrayBacked(
        PlayerDamaged.class,
        (listeners) -> (player, damageSource, damageAmount) -> {
            for (var listener : listeners) {
                if (!listener.handle(player, damageSource, damageAmount)) {
                    return false;
                }
            }
            return true;
        }
    );

    public static final @NotNull Event<BlockUsed> BLOCK_USED = EventFactory.createArrayBacked(
        BlockUsed.class,
        (listeners) -> (player, world, hand, blockHit, blockState) -> {
            for (var listener : listeners) {
                var result = listener.handle(player, world, hand, blockHit, blockState);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    public static final @NotNull Event<BlockPlaced> BLOCK_PLACED = EventFactory.createArrayBacked(
        BlockPlaced.class,
        (listeners) -> (player, world, hand, blockHit, blockItemStack) -> {
            for (var listener : listeners) {
                var result = listener.handle(player, world, hand, blockHit, blockItemStack);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

}
