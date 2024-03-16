package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
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
            var playerEntity = (ServerPlayerEntity) (Object) this;
            var player = context.getPlayer(playerEntity);
            context.itemService.handleDropInventory(player, playerEntity);
            context.specialGiftService.handleDropInventory(player, playerEntity);
        }
    }

    @Unique
    private float mineseekdestroy$previousAbsorptionAmount;

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void mineseekdestroy$hookApplyDamage1(DamageSource source, float amount, CallbackInfo info) {
        mineseekdestroy$previousAbsorptionAmount = getAbsorptionAmount();
    }

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void mineseekdestroy$hookApplyDamage2(DamageSource source, float amount, CallbackInfo info) {
        var context = Game.getGame().getContext();
        if (context != null) {
            var self = (ServerPlayerEntity) (Object) this;
            var amountActual = Math.min(amount, getHealth() + mineseekdestroy$previousAbsorptionAmount);
            context.damageService.handleDamage(self, source, amountActual);
            context.syphonService.handleDamageTaken(self, source, amountActual);
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
                context.getPlayer((ServerPlayerEntity) (Object) this).getTeam() == GameTeam.BLUE
            ) return true;
        }
        return value;
    }

    @Shadow @Final
    private PlayerInventory inventory;
    @Shadow @Final
    private PlayerAbilities abilities;

    @Unique
    private boolean mineseekdestroy$isYellowArrow(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().contains("MsdYellowArrow");
    }
    @Unique
    private boolean mineseekdestroy$canUseArrow(ItemStack stack, int yellowArrowFlag) {
        if (mineseekdestroy$isYellowArrow(stack)) {
            return yellowArrowFlag != 2;
        }
        return true;
    }

    @Overwrite
    public ItemStack getProjectileType(ItemStack weaponStack) {
        if (weaponStack.getItem() instanceof RangedWeaponItem weaponItem) {
            // 0 -> NONE, 1 -> YELLOW, 2 -> BLUE
            int yellowArrowFlag = 0;
            var context = Game.getGame().getContext();
            if (context != null) {
                var player = context.getPlayer((ServerPlayerEntity) (Object) this);
                if (player.getTeam() == GameTeam.YELLOW) {
                    yellowArrowFlag = 1;
                } else {
                    yellowArrowFlag = 2;
                }
            }

            if (yellowArrowFlag == 1) {
                for (int i = 0, size = inventory.size(); i < size; i++) {
                    ItemStack arrowStack = inventory.getStack(i);
                    if (mineseekdestroy$isYellowArrow(arrowStack)) {
                        return arrowStack;
                    }
                }
            }

            ItemStack arrowHeldStack = RangedWeaponItem.getHeldProjectile(this, weaponItem.getHeldProjectiles());
            if (!arrowHeldStack.isEmpty()) {
                if (mineseekdestroy$canUseArrow(arrowHeldStack, yellowArrowFlag)) {
                    return arrowHeldStack;
                }
            }

            Predicate<ItemStack> predicate = weaponItem.getProjectiles();
            for (int i = 0, size = inventory.size(); i < size; i++) {
                ItemStack arrowStack = inventory.getStack(i);
                if (predicate.test(arrowStack)) {
                    if (mineseekdestroy$canUseArrow(arrowStack, yellowArrowFlag)) {
                        return arrowStack;
                    }
                }
            }

            if (abilities.creativeMode) return new ItemStack(Items.ARROW);
        }

        return ItemStack.EMPTY;
    }

}
