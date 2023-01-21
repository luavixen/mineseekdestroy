package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
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

import java.util.Objects;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity {

    @Unique
    private ShulkerEntity mineseekdestroy$shulker;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void mineseekdestroy$hookTickHead(World world, BlockPos blockPos, BlockState blockState, AbstractFurnaceBlockEntity blockEntity, CallbackInfo info) {
        var stack = blockEntity.getStack(1);
        if (stack.getCount() != 1 || stack.getItem() != Items.BAMBOO) {
            blockEntity.setStack(1, new ItemStack(Items.BAMBOO));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void mineseekdestroy$hookTickTail(World world, BlockPos blockPos, BlockState blockState, AbstractFurnaceBlockEntity blockEntity, CallbackInfo info) {
        var self = Objects.requireNonNull((MixinAbstractFurnaceBlockEntity) (Object) blockEntity);
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
                    context.scoreboard.addPlayerToTeam(entity.getEntityName(), context.teamOperator);
                }
            }
            entity.setInvisible(true);
            entity.setGlowing(true);
        } else {
            var entity = self.mineseekdestroy$shulker;
            if (entity != null) {
                var context = Game.getGame().getContext();
                if (context != null) {
                    context.scoreboard.removePlayerFromTeam(entity.getEntityName(), context.teamOperator);
                }
                entity.remove(Entity.RemovalReason.DISCARDED);
                self.mineseekdestroy$shulker = null;
            }
        }
    }

}
