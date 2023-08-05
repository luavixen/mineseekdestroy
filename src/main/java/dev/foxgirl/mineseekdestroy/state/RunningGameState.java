package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Position;
import org.jetbrains.annotations.Nullable;

public abstract class RunningGameState extends GameState {

    @Override
    public void onRespawn(@Nullable GameContext context, ServerPlayerEntity oldPlayerEntity, ServerPlayerEntity newPlayerEntity, boolean alive) {
        if (context == null) {
            super.onRespawn(context, oldPlayerEntity, newPlayerEntity, alive);
            return;
        }

        Position position;

        var player = context.getPlayer(newPlayerEntity);
        if (player.isPlayingOrGhost() && !player.isAlive()) {
            position = Game.getGameProperties().getPositionBlimp();
        } else {
            position = Game.getGameProperties().getPositionSpawn().toCenterPos();
        }

        newPlayerEntity.teleport(
            context.world,
            position.getX(), position.getY(), position.getZ(),
            newPlayerEntity.getYaw(), newPlayerEntity.getPitch()
        );
        newPlayerEntity.updatePosition(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean allowDeath(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context == null) return true;

        var player = context.getPlayer(playerEntity);
        if (player.isPlayingOrGhost() && player.isAlive()) {
            player.setAlive(false);
            player.countDeath();

            ServerPlayerEntity attackerEntity;
            if (damageSource.getAttacker() instanceof ServerPlayerEntity attackerEntity1) {
                attackerEntity = attackerEntity1;
            } else if (damageSource.getSource() instanceof ServerPlayerEntity attackerEntity2) {
                attackerEntity = attackerEntity2;
            } else if (playerEntity.getPrimeAdversary() instanceof ServerPlayerEntity attackerEntity3) {
                attackerEntity = attackerEntity3;
            } else {
                attackerEntity = null;
            }

            GamePlayer attacker = attackerEntity == null ? null : context.getPlayer(attackerEntity);

            if (attacker != null) {
                if (player.getTeam() == GameTeam.PLAYER_BLACK) {
                    attacker.countKill();
                    attacker.countKill();
                } else {
                    attacker.countKill();
                }
                if (player.isGhost()) {
                    context.ghostService.handleDeath(player, playerEntity, attacker, attackerEntity);
                }
                Game.LOGGER.info(player.getName() + " was killed by " + attacker.getName());
            } else {
                Game.LOGGER.info(player.getName() + " was killed with no valid attacker");
            }

            context.specialBuddyService.handleDeath(player);
        }

        return true;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context == null) return false;

        var player = context.getPlayer(playerEntity);
        if (player.isPlayingOrGhost() && player.isAlive()) {
            var vehicle = playerEntity.getVehicle();
            if (vehicle instanceof PigEntity) {
                if (damageSource.isOf(DamageTypes.FALL)) {
                    return false;
                } else {
                    context.specialCarService.cooldownActivate((PigEntity) vehicle);
                }
            }
            if (
                player.isGhost() &&
                context.ghostService.shouldIgnoreDamage(damageSource.getTypeRegistryEntry().getKey().orElse(null))
            ) {
                return false;
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
            if (!player.isPlayingOrGhost()) return false;
        }

        return super.onItemDropped(context, playerEntity, stack, throwRandomly, retainOwnership);
    }

    @Override
    public boolean onItemAcquired(@Nullable GameContext context, ServerPlayerEntity playerEntity, PlayerInventory inventory, ItemStack stack, int slot) {
        if (Game.getGame().isOperator(playerEntity)) {
            return true;
        }
        if (Game.ILLEGAL_ITEMS.contains(stack.getItem())) {
            return false;
        }

        GameItems.replace(stack);

        if (context != null) {
            var player = context.getPlayer(playerEntity);
            if (player.isGhost()) {
                return stack.getItem() == Items.SLIME_BLOCK;
            } else if (!player.isPlaying()) {
                return false;
            }
        }

        return true;
    }

}
