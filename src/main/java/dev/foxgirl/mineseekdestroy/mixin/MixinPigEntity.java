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

import java.util.ArrayList;

@Mixin(PigEntity.class)
public abstract class MixinPigEntity extends AnimalEntity {

    protected MixinPigEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
        mineseekdestroy$handleTick();
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        super.onPlayerCollision(player);
        mineseekdestroy$handleCollision(player);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        var success = super.damage(source, amount);
        if (success) mineseekdestroy$handleDamage(source, amount);
        return success;
    }

    @Unique
    private int mineseekdestroy$cooldownAge = 0;

    @Unique
    public boolean mineseekdestroy$cooldownReady() {
        return age > mineseekdestroy$cooldownAge;
    }

    @Unique
    public void mineseekdestroy$cooldownActivate() {
        removeAllPassengers();
        mineseekdestroy$cooldownAge = age + (int) (Game.getGame().getRuleDouble(Game.RULE_CARS_COOLDOWN_DURATION) * 20.0);
    }

    @Unique
    private void mineseekdestroy$handleDamage(DamageSource source, float amount) {
        if (amount >= 1.0F && !source.isFromFalling()) {
            mineseekdestroy$cooldownActivate();
        }
    }

    @Unique
    private Vec3d mineseekdestroy$posPrev = Vec3d.ZERO;
    @Unique
    private Vec3d mineseekdestroy$posDiff = Vec3d.ZERO;

    @Unique
    private final ArrayList<Pair<ServerPlayerEntity, Integer>> mineseekdestroy$collisions = new ArrayList<>();

    @Unique
    private void mineseekdestroy$handleTick() {
        if (mineseekdestroy$shouldRemovePassengers()) removeAllPassengers();

        mineseekdestroy$posDiff = mineseekdestroy$posPrev.subtract(getPos());
        mineseekdestroy$posPrev = getPos();

        if (mineseekdestroy$collisions.size() > 0) {
            mineseekdestroy$collisions.removeIf((collision) -> age - collision.getRight() > 20);
        }
    }

    @Unique
    private boolean mineseekdestroy$shouldRemovePassengers() {
        if (!hasPassengers()) return false;
        if (!mineseekdestroy$cooldownReady()) return true;

        var context = Game.getGame().getContext();
        if (context != null) {
            var rider = getFirstPassenger();
            return rider instanceof ServerPlayerEntity player && context.getPlayer(player).isSpectator();
        }

        return false;
    }

    @Unique
    private void mineseekdestroy$handleCollision(PlayerEntity player) {
        var rider = getPrimaryPassenger();
        if (rider == null || rider == player) return;

        if (mineseekdestroy$posDiff.length() < 0.1) return;

        mineseekdestroy$performCollision((ServerPlayerEntity) player, rider);
    }

    @Unique
    private void mineseekdestroy$performCollision(ServerPlayerEntity player, Entity rider) {
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

}
