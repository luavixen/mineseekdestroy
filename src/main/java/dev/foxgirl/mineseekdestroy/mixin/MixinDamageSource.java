package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public abstract class MixinDamageSource {

    @Shadow @Final
    private RegistryEntry<DamageType> type;
    @Shadow @Final @Nullable
    private Entity attacker;
    @Shadow @Final @Nullable
    private Entity source;

    @Inject(method = "getDeathMessage", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookGetDeathMessage(LivingEntity entity, CallbackInfoReturnable<Text> info) {
        var key = type.getKey().get();

        boolean isHeartbreak = key == Game.DAMAGE_TYPE_HEARTBREAK;
        boolean isAbyss = key == Game.DAMAGE_TYPE_ABYSS;
        boolean isBitten = key == Game.DAMAGE_TYPE_BITTEN;

        if (isHeartbreak || isAbyss || isBitten) {
            Text nameEntity = entity.getDisplayName();
            Text nameAttacker;
            if (source != null) {
                nameAttacker = source.getDisplayName();
            } else if (attacker != null) {
                nameAttacker = attacker.getDisplayName();
            } else {
                nameAttacker = isHeartbreak ? Text.of("their buddy") : Text.of("the gods");
            }
            if (isHeartbreak) {
                info.setReturnValue(Text.empty().append(nameEntity).append(" was torn by the loss of ").append(nameAttacker));
            }
            if (isAbyss) {
                info.setReturnValue(Text.empty().append(nameEntity).append(" was dragged into the abyss by ").append(nameAttacker));
            }
            if (isBitten) {
                info.setReturnValue(Text.empty().append(nameEntity).append(" was bitten by the dog"));
            }
        }
    }

}
