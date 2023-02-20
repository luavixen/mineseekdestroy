package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.mixin.MixinPigEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GameState {

    protected @Nullable GameState onSetup(@NotNull GameContext context) {
        return null;
    }
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        return null;
    }

    private boolean fresh = true;

    public final @Nullable GameState update(@NotNull GameContext context) {
        if (fresh) {
            fresh = false;
            var state = onSetup(context);
            if (state != null) return state;
        }
        return onUpdate(context);
    }

    public void onRespawn(@Nullable GameContext context, ServerPlayerEntity oldPlayerEntity, ServerPlayerEntity newPlayerEntity, boolean alive) {
    }

    public boolean allowDeath(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        return true;
    }

    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        return false;
    }

    public boolean allowBlockBreak(@Nullable GameContext context, PlayerEntity playerEntity, World world, BlockPos pos, BlockState state, BlockEntity entity) {
        if (Game.getGame().isOperator(playerEntity)) {
            return true;
        }
        if (context != null) {
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            if (player.isPlaying() && Game.PLACABLE_BLOCKS.contains(state.getBlock())) {
                return true;
            }
        }
        return false;
    }

    public ActionResult onUseBlock(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHit, BlockState blockState) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (context != null) {
            var properties = Game.getGameProperties();
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            if (
                player.isPlaying() && player.isAlive() &&
                properties.getInteractableBlocks().contains(blockState.getBlock()) &&
                properties.getRegionPlayable().contains(blockHit.getBlockPos()) &&
                properties.getRegionBlimp().excludes(blockHit.getBlockPos())
            ) {
                var blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(blockHit.getBlockPos()) : null;
                if (blockEntity instanceof LootableContainerBlockEntity) {
                    return context.lootService.handleContainerOpen(blockEntity);
                }
                return ActionResult.PASS;
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseBlockWith(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHit, ItemStack stack) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (context != null) {
            var properties = Game.getGameProperties();
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            if (player.isPlaying() && player.isAlive()) {
                var item = stack.getItem();
                if (item instanceof BlockItem blockItem) {
                    if (
                        Game.PLACABLE_BLOCKS.contains(blockItem.getBlock()) &&
                        properties.getRegionPlayable().contains(blockHit.getBlockPos()) &&
                        properties.getRegionBlimp().excludes(blockHit.getBlockPos())
                    ) return ActionResult.PASS;
                }
                if (Game.USABLE_ITEMS.contains(item)) return ActionResult.PASS;
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseItem(@Nullable GameContext context, ServerPlayerEntity playerEntity, World world, Hand hand, ItemStack stack) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            if (player.isPlaying() && Game.USABLE_ITEMS.contains(stack.getItem())) {
                return ActionResult.PASS;
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseEntity(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (context != null) {
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            if (player.isPlaying() && player.isAlive()) {
                if (entity instanceof PigEntity && ((MixinPigEntity) entity).mineseekdestroy$cooldownReady()) {
                    return ActionResult.PASS;
                }
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onAttackBlock(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockPos pos, Direction direction) {
        return ActionResult.PASS;
    }

    public ActionResult onAttackEntity(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (context != null) {
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            if (player.isPlaying() && player.isAlive()) {
                if (entity instanceof PlayerEntity || entity instanceof MobEntity) {
                    return ActionResult.PASS;
                }
            }
        }
        return ActionResult.FAIL;
    }

    public boolean onItemDropped(@Nullable GameContext context, ServerPlayerEntity playerEntity, ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        return !Game.ILLEGAL_ITEMS.contains(stack.getItem());
    }

    public boolean onItemAcquired(@Nullable GameContext context, ServerPlayerEntity playerEntity, PlayerInventory inventory, ItemStack stack, int slot) {
        return !Game.ILLEGAL_ITEMS.contains(stack.getItem());
    }

}
