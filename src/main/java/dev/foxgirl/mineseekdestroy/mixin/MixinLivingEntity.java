package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void mineseekdestroy$hookSetHealth(float health, CallbackInfo info) {
        if (health <= 0) {
            Game.LOGGER.info("Invalid setHealth call for {} with value {}", this, health);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (source.isIndirect() || !(source.getAttacker() instanceof PlayerEntity)) {
            return;
        }
        var context = Game.getGame().getContext();
        if (context != null) {
            var self = (LivingEntity) (Object) this;
            var player = context.disguiseService.getDisguisedPlayer(self);
            if (player != null) {
                Game.LOGGER.info("Attempting to forward {} damage to disguised player {}", amount, player.getNameQuoted());
                var playerEntity = player.getEntity();
                if (playerEntity != null) {
                    if (playerEntity.damage(source, amount)) {
                        self.getWorld().sendEntityDamage(self, source);
                        info.setReturnValue(true);
                    }
                }
                info.setReturnValue(false);
            }
        }
    }

    @ModifyVariable(method = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V", at = @At("HEAD"), ordinal = 1)
    private double mineseekdestroy$hookTakeKnockback$1(double strength) {
        if ((Object) this instanceof ServerPlayerEntity playerEntity) {
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer(playerEntity);
                if (player.getTeam().isGhost()) {
                    return strength * Game.getGame().getRuleDouble(Game.RULE_GHOSTS_KNOCKBACK_MULTIPLIER);
                }
            }
        }
        return strength;
    }

    @Inject(method = "Lnet/minecraft/entity/LivingEntity;takeKnockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookTakeKnockback$2(double strength, double x, double z, CallbackInfo info) {
        if ((Object) this instanceof ServerPlayerEntity playerEntity) {
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer(playerEntity);
                if (player.getTeam() == GameTeam.YELLOW) info.cancel();
            }
        }
    }

}
