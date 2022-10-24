package dev.foxgirl.mineseekdestroy.service;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntityPositionS2CPacket;
import dev.foxgirl.mineseekdestroy.util.Fuck;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public final class InvisibilityService extends Service {

    private final Object lock = new Object();

    private boolean activePrevious;
    private boolean activeCurrent;

    public boolean isActive() {
        synchronized (lock) {
            return activeCurrent;
        }
    }
    public void setActive(boolean active) {
        synchronized (lock) {
            activeCurrent = active;
        }
    }

    public void handleUpdate() {
        synchronized (lock) {
            if (activePrevious == activeCurrent) return;
            activePrevious = activeCurrent;
        }
        var context = getContext();
        var players = new ArrayList<>(context.playerManager.getPlayerList());
        for (var target : players) {
            for (var other : players) {
                if (target.getId() == other.getId()) continue;
                if (target.getWorld() != other.getWorld()) continue;
                if (!target.isAlive() || !other.isAlive()) continue;
                if (target.squaredDistanceTo(other) > 65536) continue;
                target.networkHandler.sendPacket(new EntityPositionS2CPacket(other));
            }
        }
    }

    public @Nullable EntityPositionS2CPacket handlePositionPacket(@NotNull EntityPositionS2CPacket packet, @NotNull ServerPlayerEntity targetEntity) {
        Objects.requireNonNull(packet, "Argument 'packet'");
        Objects.requireNonNull(targetEntity, "Argument 'targetEntity'");

        var context = getContext();

        var packetId = packet.getId();
        if (packetId == targetEntity.getId()) return null;

        var packetEntity = targetEntity.getWorld().getEntityById(packetId);
        if (packetEntity == null) return null;

        var packetPlayer = context.getPlayer(packetEntity);
        if (packetPlayer == null) return null;

        var targetPlayer = context.getPlayer(targetEntity);

        if (targetPlayer.getTeam() == packetPlayer.getTeam()) return null;
        if (!targetPlayer.isPlaying() || !packetPlayer.isPlaying()) return null;

        return createInvisiblePacket(packetId);
    }

    private static EntityPositionS2CPacket createInvisiblePacket(int id) {
        var packet = Fuck.create(EntityPositionS2CPacket.class);
        var access = (MixinEntityPositionS2CPacket) packet;
        access.mineseekdestroy$setId(id);
        access.mineseekdestroy$setX(Game.POSITION_HELL.getX());
        access.mineseekdestroy$setY(Game.POSITION_HELL.getY());
        access.mineseekdestroy$setZ(Game.POSITION_HELL.getZ());
        return packet;
    }

}
