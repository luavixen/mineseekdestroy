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
        var context = Game.getGame().getContext();
        if (context != null && context.specialBuddyService.getDamageTypeKey() == type.getKey().get()) {
            Text name;
            if (source != null) {
                name = source.getDisplayName();
            } else if (attacker != null) {
                name = attacker.getDisplayName();
            } else {
                name = Text.of("their buddy");
            }
            info.setReturnValue(Text.empty().append(entity.getDisplayName()).append(" was torn by the loss of ").append(name));
        }
    }

}
