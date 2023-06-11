package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinProjectileEntity modifies egg and snowball projectiles to give them
 * extra knockback.
 */
@Mixin(ProjectileEntity.class)
public abstract class MixinProjectileEntity {

    @Inject(method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookOnCollision(HitResult hitResult, CallbackInfo info) {
        if (hitResult.getType() != HitResult.Type.ENTITY) return;

        var state = Game.getGame().getState();
        if (state instanceof WaitingGameState) return;

        var context = Game.getGame().getContext();
        if (context != null) {
            var player = context.getPlayer(((EntityHitResult) hitResult).getEntity());
            if (player != null && (player.isSpectator() || player.isGhost())) {
                info.cancel();
            }
        }
    }

    @Inject(method = "onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at = @At("RETURN"))
    private void mineseekdestroy$hookOnEntityHit(EntityHitResult hitResult, CallbackInfo info) {
        var self = (ProjectileEntity) (Object) this;

        double strength;
        if (self instanceof SnowballEntity) {
            strength = Game.getGame().getRuleDouble(Game.RULE_KNOCKBACK_SNOWBALL);
        } else if (self instanceof EggEntity) {
            strength = Game.getGame().getRuleDouble(Game.RULE_KNOCKBACK_EGG);
        } else {
            return;
        }

        var target = hitResult.getEntity();

        float pushYaw = self.getYaw() * 0.017453292F;
        float pushX = -MathHelper.sin(pushYaw);
        float pushZ = -MathHelper.cos(pushYaw);

        if (strength <= 0) {
            pushX = 0 - pushX;
            pushZ = 0 - pushZ;
            strength = Math.abs(strength);
        }

        if (target instanceof LivingEntity targetLiving) {
            targetLiving.takeKnockback(strength, pushX, pushZ);
        } else {
            target.addVelocity(pushX * strength, 0.1D, pushZ * strength);
        }

        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(targetPlayer));
            targetPlayer.velocityDirty = false;
        }
    }

}
