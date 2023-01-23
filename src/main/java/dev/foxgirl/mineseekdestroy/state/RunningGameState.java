package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.util.Scheduler;
import net.minecraft.entity.damage.DamageSource;
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
            var pos = Game.getGameProperties().getPositionBlimp();
            var region = Game.getGameProperties().getRegionBlimp();
            // Teleport the player normally
            newPlayerEntity.teleport(
                context.world,
                pos.getX(), pos.getY(), pos.getZ(),
                newPlayerEntity.getYaw(), newPlayerEntity.getPitch()
            );
            // Update the player's position
            newPlayerEntity.updatePosition(pos.getX(), pos.getY(), pos.getZ());
            // Wait a bit and then double-check that the player was *actually* teleported
            Scheduler.now((task) -> {
                if (!region.contains(newPlayerEntity)) {
                    newPlayerEntity.updatePosition(pos.getX(), pos.getY(), pos.getZ());
                }
            });
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
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            return player.isPlaying() && player.isAlive();
        }
        return true;
    }

    @Override
    public boolean onItemDropped(@Nullable GameContext context, ServerPlayerEntity playerEntity, ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        if (Game.getGame().isOperator(playerEntity)) {
            return true;
        }
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            return player.isPlaying();
        }
        return true;
    }

    @Override
    public boolean onItemAcquired(@Nullable GameContext context, ServerPlayerEntity playerEntity, PlayerInventory inventory, ItemStack stack, int slot) {
        if (Game.getGame().isOperator(playerEntity)) {
            return true;
        }
        if (context != null) {
            var player = context.getPlayer(playerEntity);
            return player.isPlaying();
        }
        return true;
    }

}
