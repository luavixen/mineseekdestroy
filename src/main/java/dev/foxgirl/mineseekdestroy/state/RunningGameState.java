package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.mixin.MixinPigEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public abstract class RunningGameState extends GameState {

    @Override
    public void onRespawn(@Nullable GameContext context, ServerPlayerEntity oldPlayerEntity, ServerPlayerEntity newPlayerEntity, boolean alive) {
        if (context == null) return;

        var player = context.getPlayer(newPlayerEntity);
        if (player.isPlaying() && !player.isAlive()) {
            var position = Game.getGameProperties().getPositionBlimp();
            newPlayerEntity.teleport(
                context.world,
                position.getX(), position.getY(), position.getZ(),
                newPlayerEntity.getYaw(), newPlayerEntity.getPitch()
            );
            newPlayerEntity.updatePosition(position.getX(), position.getY(), position.getZ());
        }
    }

    @Override
    public boolean allowDeath(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context == null) return true;

        var player = context.getPlayer(playerEntity);
        if (player.isPlaying() && player.isAlive()) {
            player.setAlive(false);
            player.countDeath();

            if (damageSource.getAttacker() instanceof ServerPlayerEntity playerAttackerEntity) {
                var playerAttacker = context.getPlayer(playerAttackerEntity);
                if (player.getTeam() == GameTeam.PLAYER_BLACK) {
                    playerAttacker.countKill();
                    playerAttacker.countKill();
                } else {
                    playerAttacker.countKill();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context == null) return false;

        var player = context.getPlayer(playerEntity);
        if (player.isPlaying() && player.isAlive()) {
            var vehicle = playerEntity.getVehicle();
            if (vehicle instanceof PigEntity) {
                if (damageSource.isFromFalling()) {
                    return false;
                } else {
                    ((MixinPigEntity) vehicle).mineseekdestroy$cooldownActivate();
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onItemDropped(@Nullable GameContext context, ServerPlayerEntity playerEntity, ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        if (Game.getGame().isOperator(playerEntity)) {
            return true;
        }
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            if (!player.isPlaying()) return false;
        }
        return super.onItemDropped(context, playerEntity, stack, throwRandomly, retainOwnership);
    }

    @Override
    public boolean onItemAcquired(@Nullable GameContext context, ServerPlayerEntity playerEntity, PlayerInventory inventory, ItemStack stack, int slot) {
        if (Game.getGame().isOperator(playerEntity)) {
            return true;
        }
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            if (!player.isPlaying()) return false;
        }
        return super.onItemAcquired(context, playerEntity, inventory, stack, slot);
    }

}
