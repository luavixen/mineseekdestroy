package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class MixinServerCommonNetworkHandler {

    @ModifyVariable(
        method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
        at = @At("HEAD"), ordinal = 0
    )
    private Packet<?> mineseekdestroy$hookSend(Packet<?> packet) {
        if ((Object) this instanceof ServerPlayNetworkHandler networkHandler) {
            var context = Game.getGame().getContext();
            if (context != null) {
                if (packet instanceof EntityPositionS2CPacket) {
                    var replacement = context.invisibilityService.handlePositionPacket((EntityPositionS2CPacket) packet, networkHandler.getPlayer());
                    if (replacement != null) return replacement;
                }
                if (packet instanceof EntityTrackerUpdateS2CPacket) {
                    var replacement = context.glowService.handleTrackerUpdatePacket((EntityTrackerUpdateS2CPacket) packet, networkHandler.getPlayer());
                    if (replacement != null) return replacement;
                }
            }
        }
        return packet;
    }

}
