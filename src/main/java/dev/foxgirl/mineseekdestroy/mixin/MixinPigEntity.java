package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(PigEntity.class)
public abstract class MixinPigEntity extends AnimalEntity {

    protected MixinPigEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void mineseekdestroy$hookDropInventory(CallbackInfo info) {
        info.cancel();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (amount >= 1.0F && !source.isFromFalling()) {
            removeAllPassengers();
        }
        return super.damage(source, amount);
    }

    @Unique
    private Vec3d mineseekdestroy$posPrev = Vec3d.ZERO;
    @Unique
    private Vec3d mineseekdestroy$posDiff = Vec3d.ZERO;

    @Unique
    private final ArrayList<Pair<ServerPlayerEntity, Integer>> mineseekdestroy$collisions = new ArrayList<>();

    @Unique
    private void mineseekdestroy$handleCollision(ServerPlayerEntity player, Entity rider) {
        for (var collision : mineseekdestroy$collisions) {
            if (collision.getLeft() == player) return;
        }
        mineseekdestroy$collisions.add(new Pair<>(player, age));

        var damageAmount = (float) Game.getGame().getRuleDouble(Game.RULE_CARS_DAMAGE);
        var damageSource = rider instanceof PlayerEntity ? DamageSource.player((PlayerEntity) rider) : DamageSource.OUT_OF_WORLD;
        player.damage(damageSource, damageAmount);

        var pushStrength = Game.getGame().getRuleDouble(Game.RULE_CARS_KNOCKBACK);
        var pushDirection = getPos().subtract(player.getPos());
        player.takeKnockback(pushStrength, pushDirection.x, pushDirection.z);
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.velocityDirty = false;
    }

    @Override
    public void tick() {
        super.tick();

        mineseekdestroy$posDiff = mineseekdestroy$posPrev.subtract(getPos());
        mineseekdestroy$posPrev = getPos();

        if (mineseekdestroy$collisions.size() > 0) {
            mineseekdestroy$collisions.removeIf((collision) -> age - collision.getRight() > 20);
        }
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        super.onPlayerCollision(player);

        if (!(player instanceof ServerPlayerEntity)) return;

        var rider = getPrimaryPassenger();
        if (rider == null || rider == player) return;

        if (mineseekdestroy$posDiff.length() < 0.1) return;

        mineseekdestroy$handleCollision((ServerPlayerEntity) player, rider);
    }

}
