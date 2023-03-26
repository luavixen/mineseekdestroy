package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.SaddledComponent;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SaddledComponent.class)
public abstract class MixinSaddledComponent {

    @Redirect(
        method = "boost(Lnet/minecraft/util/math/random/Random;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/data/DataTracker;set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V")
    )
    private void mineseekdestroy$hookBoost(DataTracker dataTracker, TrackedData<Integer> key, Object value) {
        dataTracker.set(key, Integer.valueOf((int) (Game.getGame().getRuleDouble(Game.RULE_CARS_BOOST_DURATION) * 20.0)));
    }

}
