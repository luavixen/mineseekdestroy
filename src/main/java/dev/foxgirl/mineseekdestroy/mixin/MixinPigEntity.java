package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(PigEntity.class)
public abstract class MixinPigEntity extends AnimalEntity {

    private MixinPigEntity(EntityType<? extends AnimalEntity> entityType, World world) {
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
    @SuppressWarnings("unchecked")
    private static final List<RegistryKey<DamageType>> mineseekdestroy$cooldownDamageTypes = ImmutableList.copyOf(new RegistryKey[] {
        DamageTypes.GENERIC,
        DamageTypes.MAGIC,
        DamageTypes.INDIRECT_MAGIC,
        DamageTypes.MOB_ATTACK,
        DamageTypes.MOB_ATTACK_NO_AGGRO,
        DamageTypes.MOB_PROJECTILE,
        DamageTypes.ARROW,
        DamageTypes.TRIDENT,
        DamageTypes.LIGHTNING_BOLT,
        DamageTypes.FIREWORKS,
        DamageTypes.FIREBALL,
        DamageTypes.UNATTRIBUTED_FIREBALL,
        DamageTypes.PLAYER_ATTACK,
        DamageTypes.PLAYER_EXPLOSION,
        DamageTypes.EXPLOSION,
    });

    @Unique
    private int mineseekdestroy$cooldownAge = 0;

    @Unique
    public boolean mineseekdestroy$cooldownIsReady() {
        return age > mineseekdestroy$cooldownAge;
    }

    @Unique
    public void mineseekdestroy$cooldownActivate() {
        removeAllPassengers();
        mineseekdestroy$cooldownAge = age + (int) (Game.getGame().getRuleDouble(Game.RULE_CARS_COOLDOWN_DURATION) * 20.0);
    }

    @Unique
    private void mineseekdestroy$handleDamage(DamageSource source, float amount) {
        if (amount >= 1.0F && mineseekdestroy$cooldownDamageTypes.stream().anyMatch(source::isOf)) {
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
        if (!mineseekdestroy$cooldownIsReady()) return true;

        var context = Game.getGame().getContext();
        if (context != null) {
            var rider = getFirstPassenger();
            return rider instanceof ServerPlayerEntity player && context.getPlayer(player).isSpectator();
        }

        return false;
    }

    @Unique
    private void mineseekdestroy$handleCollision(PlayerEntity player) {
        var rider = getControllingPassenger();
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
        var damageSource = rider instanceof PlayerEntity ? getDamageSources().playerAttack((PlayerEntity) rider) : getDamageSources().outOfWorld();
        player.damage(damageSource, damageAmount);

        var pushStrength = Game.getGame().getRuleDouble(Game.RULE_CARS_KNOCKBACK);
        var pushDirection = getPos().subtract(player.getPos());
        player.takeKnockback(pushStrength, pushDirection.x, pushDirection.z);
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.velocityDirty = false;
    }

}
