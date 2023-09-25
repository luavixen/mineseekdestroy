package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameItems;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinAbstractFurnaceBlockEntity modifies furnaces to always have fuel
 * (Bamboo) and glow when being used.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity {

    @Unique
    private ShulkerEntity mineseekdestroy$shulker;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void mineseekdestroy$hookTick$0(World world, BlockPos blockPos, BlockState blockState, AbstractFurnaceBlockEntity blockEntity, CallbackInfo info) {
        if (!Game.getGame().getRuleBoolean(Game.RULE_ENHANCED_FURNACES)) return;
        var stack = blockEntity.getStack(1);
        if (stack.getCount() != 1 || stack.getItem() != Items.BAMBOO) {
            blockEntity.setStack(1, new ItemStack(Items.BAMBOO));
        }
        GameItems.replace(blockEntity.getStack(2));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void mineseekdestroy$hookTick$1(World world, BlockPos blockPos, BlockState blockState, AbstractFurnaceBlockEntity blockEntity, CallbackInfo info) {
        if (!Game.getGame().getRuleBoolean(Game.RULE_ENHANCED_FURNACES)) return;
        var self = (MixinAbstractFurnaceBlockEntity) (Object) blockEntity;
        if (blockState.get(AbstractFurnaceBlock.LIT)) {
            var entity = self.mineseekdestroy$shulker;
            if (entity == null) {
                entity = self.mineseekdestroy$shulker = EntityType.SHULKER.create(world);
                entity.setPosition(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                entity.setAiDisabled(true);
                entity.setInvulnerable(true);
                world.spawnEntity(entity);
                var context = Game.getGame().getContext();
                if (context != null) {
                    context.scoreboard.addPlayerToTeam(entity.getEntityName(), context.getTeam(GameTeam.OPERATOR));
                }
            }
            entity.setInvisible(true);
            entity.setGlowing(true);
        } else {
            var entity = self.mineseekdestroy$shulker;
            if (entity != null) {
                var context = Game.getGame().getContext();
                if (context != null) {
                    try {
                        context.scoreboard.removePlayerFromTeam(entity.getEntityName(), context.getTeam(GameTeam.OPERATOR));
                    } catch (IllegalStateException ignored) {}
                }
                entity.remove(Entity.RemovalReason.DISCARDED);
                self.mineseekdestroy$shulker = null;
            }
        }
    }

}
