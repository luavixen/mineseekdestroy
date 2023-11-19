package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProtectionEnchantment.class)
public abstract class MixinProtectionEnchantment {

    @Inject(method = "transformExplosionKnockback", at = @At("HEAD"), cancellable = true)
    private static void mineseekdestroy$hookTransformExplosionKnockback(LivingEntity entity, double velocity, CallbackInfoReturnable<Double> info) {
        if (entity instanceof ServerPlayerEntity playerEntity) {
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer(playerEntity);
                if (player.getTeam() == GameTeam.YELLOW) info.setReturnValue(0.0);
            }
        }
    }

}
