package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionEntity.class)
public abstract class MixinPotionEntity {

    @Inject(method = "isLingering()Z", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookIsLingering(CallbackInfoReturnable<Boolean> info) {
        var self = (PotionEntity) (Object) this;
        if (self.getOwner() instanceof ServerPlayerEntity playerEntity) {
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer(playerEntity);
                if (player.getTeam() == GameTeam.PLAYER_YELLOW) info.setReturnValue(true);
            }
        }
    }

}
