package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {

    private MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo info) {
        var context = Game.getGame().getContext();
        if (context != null && context.getPlayer((ServerPlayerEntity) (Object) this).isSpectator()) {
            info.cancel();
        }
    }

    @Inject(method = "dropInventory", at = @At("TAIL"))
    private void mineseekdestroy$hookDropInventory(CallbackInfo info) {
        var context = Game.getGame().getContext();
        if (context != null) {
            context.itemService.handleDropInventory((ServerPlayerEntity) (Object) this);
        }
    }

    @ModifyVariable(
        method = "attack(Lnet/minecraft/entity/Entity;)V",
        at = @At("STORE"), ordinal = 2
    )
    private boolean mineseekdestroy$hookAttack(boolean value) {
        if (Rules.getBlueMeleeCrits()) {
            var context = Game.getGame().getContext();
            if (
                context != null &&
                context.getPlayer((ServerPlayerEntity) (Object) this).getTeam() == GameTeam.PLAYER_BLUE
            ) return true;
        }
        return value;
    }

    @Shadow @Final
    private PlayerInventory inventory;
    @Shadow @Final
    private PlayerAbilities abilities;

    @Overwrite
    public ItemStack getProjectileType(ItemStack weaponStack) {
        if (weaponStack.getItem() instanceof RangedWeaponItem weaponItem) {
            ItemStack arrowHeldStack = RangedWeaponItem.getHeldProjectile(this, weaponItem.getHeldProjectiles());
            if (!arrowHeldStack.isEmpty()) return arrowHeldStack;

            int yellowArrowFlag = 0;
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer((ServerPlayerEntity) (Object) this);
                if (player.getTeam() == GameTeam.PLAYER_YELLOW) {
                    yellowArrowFlag = 1;
                } else {
                    yellowArrowFlag = 2;
                }
            }

            if (yellowArrowFlag == 1) {
                for (int i = 0, size = inventory.size(); i < size; i++) {
                    ItemStack arrowStack = inventory.getStack(i);
                    if (
                        arrowStack.hasNbt() &&
                        arrowStack.getNbt().contains("MsdYellowArrow")
                    ) {
                        return arrowStack;
                    }
                }
            }

            Predicate<ItemStack> predicate = weaponItem.getProjectiles();
            for (int i = 0, size = inventory.size(); i < size; i++) {
                ItemStack arrowStack = inventory.getStack(i);
                if (predicate.test(arrowStack)) {
                    if (yellowArrowFlag != 2 || !(
                        arrowStack.hasNbt() &&
                        arrowStack.getNbt().contains("MsdYellowArrow")
                    )) {
                        return arrowStack;
                    }
                }
            }

            if (abilities.creativeMode) return new ItemStack(Items.ARROW);
        }

        return ItemStack.EMPTY;
    }

}
