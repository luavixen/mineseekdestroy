package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.util.Scheduler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.item.Items;
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
        var position = Game.getGameProperties().getPositionSpawn().toCenterPos();
        newPlayerEntity.teleport(
            context != null ? context.world : newPlayerEntity.getWorld(),
            position.getX(), position.getY(), position.getZ(),
            newPlayerEntity.getYaw(), newPlayerEntity.getPitch()
        );
        newPlayerEntity.updatePosition(position.getX(), position.getY(), position.getZ());
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
            var properties = Game.getGameProperties();
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            return player.isPlaying() && player.isAlive()
                && Game.PLACABLE_BLOCKS.contains(state.getBlock())
                && properties.getRegionPlayable().contains(pos)
                && properties.getRegionBlimp().excludes(pos);
        }
        return false;
    }

    public ActionResult onUseBlock(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHit, BlockState blockState) {
        if (Game.getGame().isOperator(playerEntity)) {
            if (context != null && context.world == world) {
                if (blockState.getBlock() == Blocks.FLETCHING_TABLE) {
                    var player = context.getPlayer((ServerPlayerEntity) playerEntity);
                    var result = context.specialSummonsService.handleAltarOpen(player, blockHit.getBlockPos());
                    if (result != ActionResult.PASS) return result;
                }
            }
            return ActionResult.PASS;
        }
        if (context != null && context.world == world) {
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
                    var result = context.lootService.handleContainerOpen(blockEntity);
                    if (result != ActionResult.PASS) return result;
                }
                if (blockState.getBlock() == Blocks.FLETCHING_TABLE) {
                    var result = context.specialSummonsService.handleAltarOpen(player, blockHit.getBlockPos());
                    if (result != ActionResult.PASS) return result;
                }
                return ActionResult.PASS;
            }
            if (
                blockState.getBlock() == Blocks.LEVER &&
                properties.getRegionBlimp().contains(blockHit.getBlockPos())
            ) {
                return ActionResult.PASS;
            }
        } else {
            if (
                blockState.getBlock() == Blocks.LEVER &&
                world.getBlockState(blockHit.getBlockPos().down()).getBlock() == Blocks.OBSERVER
            ) {
                return ActionResult.PASS;
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onUseBlockWith(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockHitResult blockHit, ItemStack stack) {
        if (Game.getGame().isOperator(playerEntity)) {
            return ActionResult.PASS;
        }
        if (context != null && context.world == world) {
            var properties = Game.getGameProperties();
            var player = context.getPlayer((ServerPlayerEntity) playerEntity);
            if (player.isPlaying() && player.isAlive()) {
                var item = stack.getItem();
                if (
                    item instanceof BlockItem blockItem &&
                    Game.PLACABLE_BLOCKS.contains(blockItem.getBlock()) &&
                    properties.getRegionPlayable().contains(blockHit.getBlockPos()) &&
                    properties.getRegionBlimp().excludes(blockHit.getBlockPos())
                ) {
                    if (item == Items.TARGET && stack.hasCustomName()) {
                        context.specialFamilyGuyService.handleFamilyGuyBlockPlaced(player, blockHit);
                    }
                    return ActionResult.PASS;
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
                if (entity instanceof PigEntity && context.specialCarService.cooldownIsReady((PigEntity) entity)) {
                    return ActionResult.PASS;
                }
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult onAttackBlock(@Nullable GameContext context, PlayerEntity playerEntity, World world, Hand hand, BlockPos pos, Direction direction) {
        if (context != null && context.world == world) {
            if (world.getBlockState(pos).getBlock() == Blocks.QUARTZ_SLAB) {
                var result = context.specialPianoService.handleInteract(context.getPlayer((ServerPlayerEntity) playerEntity), pos);
                if (result != ActionResult.PASS) return result;
            }
        }
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
        if (Game.getGame().isOperator(playerEntity)) return true;

        if (Game.UNDROPPABLE_ITEMS.contains(stack.getItem())) {
            Scheduler.delay(1.0, (schedule) -> playerEntity.giveItemStack(stack));
            return false;
        }

        return !Game.ILLEGAL_ITEMS.contains(stack.getItem());
    }

    public boolean onItemAcquired(@Nullable GameContext context, ServerPlayerEntity playerEntity, PlayerInventory inventory, ItemStack stack, int slot) {
        return Game.getGame().isOperator(playerEntity) || !Game.ILLEGAL_ITEMS.contains(stack.getItem());
    }

}
