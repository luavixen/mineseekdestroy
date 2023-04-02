package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo info) {
        var context = Game.getGame().getContext();
        if (context != null && context.getPlayer((ServerPlayerEntity) (Object) this).isSpectator()) {
            info.cancel();
        }
    }

    @Inject(method = "dropInventory", at = @At("TAIL"))
    private void mineseekdestroy$hookDropInventory(CallbackInfo info) {
        var context = Game.getGame().getContext();
        if (context != null) {
            context.itemService.handleDropInventory((ServerPlayerEntity) (Object) this);
        }
    }

}