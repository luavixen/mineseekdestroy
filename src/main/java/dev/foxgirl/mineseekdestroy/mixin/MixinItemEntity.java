package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameItems;
import dev.foxgirl.mineseekdestroy.GameTeam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity extends Entity {

    private MixinItemEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow(prefix = "mineseekdestroy$")
    abstract ItemStack mineseekdestroy$getStack();

    @Unique
    private boolean mineseekdestroy$ready = false;

    @Unique
    private boolean mineseekdestroy$setup() {
        mineseekdestroy$ready = true;

        var stack = mineseekdestroy$getStack();

        if (Game.ILLEGAL_ITEMS.contains(stack.getItem())) {
            discard();
            return true;
        } else if (
            (Game.DROPPED_ITEMS.contains(stack.getItem())) ||
            (stack.hasNbt() && stack.getNbt().contains("MsdGlowing"))
        ) {
            var context = Game.getGame().getContext();
            if (context != null) {
                context.scoreboard.addScoreHolderToTeam(getNameForScoreboard(), context.getTeam(GameTeam.OPERATOR));
                if (!isGlowing()) {
                    setGlowing(true);
                }
            }
        } else {
            GameItems.replace(stack);
        }

        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if (Game.DROPPED_ITEMS.contains(mineseekdestroy$getStack().getItem())) {
            var context = Game.getGame().getContext();
            if (context != null) {
                try {
                    context.scoreboard.removeScoreHolderFromTeam(getNameForScoreboard(), context.getTeam(GameTeam.OPERATOR));
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookTick(CallbackInfo info) {
        if (!mineseekdestroy$ready && mineseekdestroy$setup()) info.cancel();
    }

}
