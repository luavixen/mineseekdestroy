package dev.foxgirl.mineseekdestroy.mixin;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.util.Broadcast;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EnderPearlEntity.class)
public abstract class MixinEnderPearlEntity extends ThrownItemEntity {

    private MixinEnderPearlEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyVariable(
        method = "onCollision(Lnet/minecraft/util/hit/HitResult;)V",
        at = @At("STORE"), ordinal = 0
    )
    private Entity mineseekdestroy$hookOnCollision(Entity entity) {
        var context = Game.getGame().getContext();
        if (context != null && entity instanceof ServerPlayerEntity playerEntity) {
            var player = context.getPlayer(playerEntity);
            if (player.getTeam() == GameTeam.PLAYER_YELLOW) {
                if (playerEntity.getWorld() != getWorld()) return null;

                var targetEntity = getWorld().getPlayers()
                    .stream()
                    .filter((currentEntity) -> {
                        if (currentEntity == playerEntity) {
                            return false;
                        }
                        if (squaredDistanceTo(currentEntity) > 256.0) {
                            return false;
                        }
                        var current = context.getPlayer((ServerPlayerEntity) currentEntity);
                        return current.isPlaying() && current.isLiving();
                    })
                    .min((a, b) -> Double.compare(squaredDistanceTo(a), squaredDistanceTo(b)))
                    .orElse(null);

                if (targetEntity != null) {
                    Game.LOGGER.info("Grapple pearl teleporting target '{}' to user '{}", targetEntity.getName(), playerEntity.getEntityName());

                    refreshPositionAfterTeleport(playerEntity.getPos());

                    Broadcast.sendParticles(ParticleTypes.POOF, 0.1F, 5, getWorld(), targetEntity.getPos());
                    Broadcast.sendSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F, getWorld(), getPos());

                    return targetEntity;
                } else {
                    Game.LOGGER.info("Grapple pearl from user '{}' missed", playerEntity.getEntityName());

                    Broadcast.sendParticles(ParticleTypes.POOF, 0.1F, 3, getWorld(), getPos());

                    return null;
                }
            } else {
                Game.LOGGER.info("Normal pearl teleporting user '{}'", playerEntity.getEntityName());
                Broadcast.sendSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F, getWorld(), getPos());
                Broadcast.sendParticles(ParticleTypes.POOF, 0.1F, 5, playerEntity.getWorld(), playerEntity.getPos());
            }
        }

        return entity;
    }

}
