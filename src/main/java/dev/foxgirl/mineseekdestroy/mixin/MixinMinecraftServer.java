package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.util.Scheduler;
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
            Scheduler.stop();
        } catch (Exception cause) {
            Game.LOGGER.error("Scheduler failed to stop", cause);
        }
    }

}
