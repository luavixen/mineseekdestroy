package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.SaddledComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SaddledComponent.class)
public abstract class MixinSaddledComponent {

    @Redirect(
        method = "boost(Lnet/minecraft/util/math/random/Random;)Z",
        at = @At(value = "FIELD", target = "Lnet/minecraft/entity/SaddledComponent;currentBoostTime:I", opcode = Opcodes.PUTFIELD)
    )
    private void mineseekdestroy$hookBoost(SaddledComponent self, int value) {
        self.currentBoostTime = (int) (Game.getGame().getRuleDouble(Game.RULE_CARS_BOOST_DURATION) * 20.0);
    }

}
