package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPositionS2CPacket.class)
public interface MixinEntityPositionS2CPacket {

    @Mutable @Accessor("id")
    void mineseekdestroy$setId(int id);

    @Mutable @Accessor("x")
    void mineseekdestroy$setX(double x);

    @Mutable @Accessor("y")
    void mineseekdestroy$setY(double y);

    @Mutable @Accessor("z")
    void mineseekdestroy$setZ(double z);

}
