package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface MixinEntity {

    @Accessor("FLAGS")
    static TrackedData<Byte> mineseekdestroy$getFLAGS() {
        throw new AssertionError();
    }

}
