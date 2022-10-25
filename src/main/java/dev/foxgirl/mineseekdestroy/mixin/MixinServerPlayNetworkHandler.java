package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {

    @Shadow
    private ServerPlayerEntity player;

    @ModifyVariable(
        method = "sendPacket(Lnet/minecraft/network/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
        at = @At("HEAD"), ordinal = 0
    )
    private Packet<?> mineseekdestroy$hookSendPacket(Packet<?> packet) {
        if (packet instanceof EntityPositionS2CPacket) {
            var context = Game.getGame().getContext();
            if (context != null) {
                var replacement = context.invisibilityService.handlePositionPacket((EntityPositionS2CPacket) packet, player);
                if (replacement != null) return replacement;
            }
        }
        return packet;
    }

}
