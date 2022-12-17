package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public interface MixinEntityTrackerUpdateS2CPacket {

    @Accessor("id") @Mutable
    void mineseekdestroy$setId(int id);

    @Accessor("trackedValues") @Mutable
    void mineseekdestroy$setTrackedValues(List<DataTracker.SerializedEntry<?>> trackedValues);

}
