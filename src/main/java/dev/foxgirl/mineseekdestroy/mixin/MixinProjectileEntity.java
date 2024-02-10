package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.util.EntitiesKt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinProjectileEntity modifies egg and snowball projectiles to give them
 * extra knockback.
 */
@Mixin(ProjectileEntity.class)
public abstract class MixinProjectileEntity {

    @Inject(method = "canHit(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookCanHitEntity(Entity entity, CallbackInfoReturnable<Boolean> info) {
        var state = Game.getGame().getState();
        if (state.isWaiting()) return;

        var context = Game.getGame().getContext();
        if (context != null) {
            var player = context.getPlayer(entity);
            if (player != null && (player.isSpectator() || player.isGhost())) {
                info.setReturnValue(false);
            }
        }
    }

    @Inject(method = "onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at = @At("RETURN"))
    private void mineseekdestroy$hookOnEntityHit(EntityHitResult hitResult, CallbackInfo info) {
        if ((Object) this instanceof SnowballEntity) {
            mineseekdestroy$handleKnockbackEntity(hitResult, Game.getGame().getRuleDouble(Game.RULE_KNOCKBACK_SNOWBALL));
        }
        if ((Object) this instanceof EggEntity) {
            mineseekdestroy$handleKnockbackEntity(hitResult, Game.getGame().getRuleDouble(Game.RULE_KNOCKBACK_EGG));
        }
    }

    @Unique
    private void mineseekdestroy$handleKnockbackEntity(EntityHitResult hitResult, double strength) {
        var self = (ProjectileEntity) (Object) this;

        var target = hitResult.getEntity();

        float pushYaw = self.getYaw() * 0.017453292F;
        float pushX = -MathHelper.sin(pushYaw);
        float pushZ = -MathHelper.cos(pushYaw);

        if (strength <= 0) {
            pushX = 0 - pushX;
            pushZ = 0 - pushZ;
            strength = Math.abs(strength);
        }

        EntitiesKt.applyKnockback(target, strength, pushX, pushZ, true);
    }

}
