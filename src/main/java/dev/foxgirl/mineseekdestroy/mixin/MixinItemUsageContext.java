package dev.foxgirl.mineseekdestroy.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemUsageContext.class)
public interface MixinItemUsageContext {

    @Accessor("player")
    PlayerEntity mineseekdestroy$getPlayer();

    @Accessor("hand")
    Hand mineseekdestroy$getHand();

    @Accessor("hit")
    BlockHitResult mineseekdestroy$getHit();

    @Accessor("world")
    World mineseekdestroy$getWorld();

}
