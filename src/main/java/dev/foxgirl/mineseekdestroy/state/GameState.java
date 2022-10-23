package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
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

    public GameState update(@NotNull GameContext context) {
        context.armorService.handleUpdate();
        return null;
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
        if (Game.PLACABLE_BLOCKS.contains(state.getBlock())) {
            return true;
        }
        return false;
    }

    public ActionResult onBlockUsed(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHit, BlockState blockState) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (Game.INTERACTABLE_BLOCKS.contains(blockState.getBlock())) {
            var blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(blockHit.getBlockPos()) : null;
            if (blockEntity instanceof LootableContainerBlockEntity) {
                if (context != null) {
                    return context.lootService.handleContainerOpen(blockEntity);
                } else {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        }
        return ActionResult.FAIL;
    }

    public ActionResult onBlockPlaced(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHit, ItemStack blockItemStack) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (
            blockItemStack.getItem() instanceof BlockItem blockItem &&
                Game.PLACABLE_BLOCKS.contains(blockItem.getBlock()) &&
                Game.REGION_PLAYABLE.contains(blockHit.getBlockPos())
        ) {
            if (context != null) {
                var player = context.getPlayer((ServerPlayerEntity) playerEntity);
                if (player.isPlaying() && player.isAlive()) {
                    return ActionResult.PASS;
                }
            }
            return ActionResult.FAIL;
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseEntity(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        return Game.getGame().isOperator(playerEntity) ? ActionResult.PASS : ActionResult.FAIL;
    }

    public ActionResult onAttackBlock(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockPos pos, Direction direction) {
        return ActionResult.PASS;
    }

    public ActionResult onAttackEntity(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (entity instanceof PlayerEntity) {
            return ActionResult.PASS;
        }
        return ActionResult.FAIL;
    }

}
