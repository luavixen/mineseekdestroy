package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.*;
import dev.foxgirl.mineseekdestroy.util.MiscKt;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
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
        if (player.isPlayingOrGhost()) {
            if (player.isAlive() || player.isUndead()) {
                position = Game.getGameProperties().getPositionBlimpTop();
                Game.LOGGER.info("Respawning player {} in blimp at upper deck (running!)", player.getNameQuoted());
            } else {
                position = Game.getGameProperties().getPositionBlimp();
                Game.LOGGER.info("Respawning player {} in blimp at lower deck (running!)", player.getNameQuoted());
            }
        } else {
            position = Game.getGameProperties().getPositionSpawn().toCenterPos();
            Game.LOGGER.info("Respawning player {} at spawn (running!)", player.getNameQuoted());
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

        if (damageSource.isOf(DamageTypes.GENERIC_KILL) || damageAmount >= 100000.0F) {
            Game.LOGGER.info("Invalid death for player \"{}\" source: {} amount: {}", playerEntity.getNameForScoreboard(), damageSource.getName(), damageAmount);
            return false;
        }

        var player = context.getPlayer(playerEntity);
        if (player.isPlayingOrGhost() && player.isAlive()) {
            if (player.isCannon()) {
                if (player.isUndead()) {
                    player.setAlive(false);
                } else {
                    player.setUndead(true);
                }
            } else {
                player.setAlive(false);
            }
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
                if (player.getTeam() == GameTeam.BLACK) {
                    attacker.countKill();
                    attacker.countKill();
                } else {
                    attacker.countKill();
                }
                if (player.isGhost()) {
                    context.ghostService.handleGhostDeath(player, playerEntity, attacker, attackerEntity);
                }
                Game.LOGGER.info("Death recorded, {} was killed by {}", player.getNameQuoted(), attacker.getNameQuoted());
            } else {
                Game.LOGGER.info("Death recorded, {} was killed with no valid attacker", player.getNameQuoted());
            }

            context.soulService.handleDeath(player, playerEntity);
            context.specialBuddyService.handleDeath(player);
        } else {
            Game.LOGGER.info("Unexpected death for player {} who is not playing and alive, continuing", player.getNameQuoted());
        }

        return true;
    }

    @Override
    public boolean onTakeDamage(@Nullable GameContext context, ServerPlayerEntity playerEntity, DamageSource damageSource, float damageAmount) {
        if (context == null) return false;

        if (damageSource.isOf(DamageTypes.GENERIC_KILL) || damageAmount >= 100000.0F) {
            Game.LOGGER.info("Invalid damage for player \"{}\" source: {} amount: {}", playerEntity.getNameForScoreboard(), damageSource.getName(), damageAmount);
            return false;
        }

        var player = context.getPlayer(playerEntity);
        if (player.isPlayingOrGhost() && player.isAlive()) {
            if (
                playerEntity.hasStatusEffect(StatusEffects.JUMP_BOOST) &&
                damageSource.isOf(DamageTypes.FALL)
            ) {
                return false;
            }
            if (
                player.getTeam() == GameTeam.BLUE &&
                damageSource.isOf(DamageTypes.FLY_INTO_WALL)
            ) {
                return false;
            }
            if (
                player.isGhost() &&
                context.ghostService.shouldGhostIgnoreDamage(damageSource.getTypeRegistryEntry().getKey().orElse(null))
            ) {
                return false;
            }
            if (
                player.isCannon() &&
                context.conduitService.shouldIgnoreDamage(player, playerEntity, damageSource)
            ) {
                return false;
            }
            var vehicle = playerEntity.getVehicle();
            if (vehicle instanceof PigEntity) {
                if (damageSource.isOf(DamageTypes.FALL)) {
                    return false;
                } else {
                    context.specialCarService.cooldownActivate((PigEntity) vehicle);
                }
            }
            var attacker = MiscKt.getPlayer(damageSource);
            if (attacker != null && attacker.isUndead()) {
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
