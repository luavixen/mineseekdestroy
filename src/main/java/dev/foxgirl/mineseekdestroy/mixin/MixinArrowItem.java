package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArrowItem.class)
public abstract class MixinArrowItem {

    @Inject(method = "createArrow", at = @At("RETURN"))
    private void mineseekdestroy$hookCreateArrow(World world, ItemStack stack, LivingEntity shooter, CallbackInfoReturnable<PersistentProjectileEntity> info) {
        if (Rules.getBlueArrowCrits()) {
            var context = Game.getGame().getContext();
            if (context == null) return;
            var player = context.getPlayer(shooter);
            if (player != null && player.getTeam() == GameTeam.PLAYER_BLUE) {
                info.getReturnValue().setCritical(true);
            }
        }
    }

}
