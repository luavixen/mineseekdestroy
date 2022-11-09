package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.world.ModifiableWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EggEntity.class)
public abstract class MixinEggEntity {

    @Redirect(
        method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ModifiableWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean mineseekdestroy$hookOnCollision(ModifiableWorld world, Entity entity) {
        return false;
    }

}
