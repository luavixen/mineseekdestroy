package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class MixinProjectileEntity {

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
        float pushY = -MathHelper.cos(pushYaw);

        if (strength <= 0) {
            pushX = 0 - pushX;
            pushY = 0 - pushY;
            strength = Math.abs(strength);
        }

        if (target instanceof LivingEntity targetLiving) {
            targetLiving.takeKnockback(strength, pushX, pushY);
        } else {
            target.addVelocity(pushX * strength, 0.1D, pushY * strength);
        }

        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(targetPlayer));
            targetPlayer.velocityDirty = false;
        }
    }

}
