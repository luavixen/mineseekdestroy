package dev.foxgirl.mineseekdestroy.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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
    public interface BlockUsedWith {
        ActionResult handle(PlayerEntity player, World world, Hand hand, BlockHitResult blockHit, ItemStack stack);
    }

    @FunctionalInterface
    public interface ItemUsed {
        ActionResult handle(ServerPlayerEntity player, World world, Hand hand, ItemStack stack);
    }

    @FunctionalInterface
    public interface ItemDropped {
        boolean handle(ServerPlayerEntity player, ItemStack stack, boolean throwRandomly, boolean retainOwnership);
    }

    @FunctionalInterface
    public interface ItemAcquired {
        boolean handle(ServerPlayerEntity player, PlayerInventory inventory, ItemStack stack, int slot);
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

    public static final @NotNull Event<BlockUsedWith> BLOCK_USED_WITH = EventFactory.createArrayBacked(
        BlockUsedWith.class,
        (listeners) -> (player, world, hand, blockHit, stack) -> {
            for (var listener : listeners) {
                var result = listener.handle(player, world, hand, blockHit, stack);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    public static final @NotNull Event<ItemUsed> ITEM_USED = EventFactory.createArrayBacked(
        ItemUsed.class,
        (listeners) -> (player, world, hand, stack) -> {
            for (var listener : listeners) {
                var result = listener.handle(player, world, hand, stack);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    public static final @NotNull Event<ItemDropped> ITEM_DROPPED = EventFactory.createArrayBacked(
        ItemDropped.class,
        (listeners) -> (player, stack, throwRandomly, retainOwnership) -> {
            for (var listener : listeners) {
                if (!listener.handle(player, stack, throwRandomly, retainOwnership)) {
                    return false;
                }
            }
            return true;
        }
    );

    public static final @NotNull Event<ItemAcquired> ITEM_ACQUIRED = EventFactory.createArrayBacked(
        ItemAcquired.class,
        (listeners) -> (player, inventory, stack, slot) -> {
            for (var listener : listeners) {
                if (!listener.handle(player, inventory, stack, slot)) {
                    return false;
                }
            }
            return true;
        }
    );

}
