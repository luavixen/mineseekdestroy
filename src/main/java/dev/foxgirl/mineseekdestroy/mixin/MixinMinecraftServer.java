package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.util.async.Scheduler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void mineseekdestroy$hookShutdown(CallbackInfo info) {
        try {
            Game.getGame().destroy();
        } catch (Exception cause) {
            Game.LOGGER.error("Shutdown, game destroy failed", cause);
        }
        try {
            Scheduler.stop();
        } catch (Exception cause) {
            Game.LOGGER.error("Shutdown, scheduler failed to stop", cause);
        }
    }

}
