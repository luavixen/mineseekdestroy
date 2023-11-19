package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void mineseekdestroy$hookSetHealth(float health, CallbackInfo info) {
        if (health <= 0) {
            Game.LOGGER.info("Invalid setHealth call for {} with value {}", this, health);
        }
    }

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookTakeKnockback(double strength, double x, double z, CallbackInfo info) {
        if ((Object) this instanceof ServerPlayerEntity playerEntity) {
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer(playerEntity);
                if (player.getTeam() == GameTeam.YELLOW) info.cancel();
            }
        }
    }

}
