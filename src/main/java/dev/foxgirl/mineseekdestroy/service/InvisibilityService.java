package dev.foxgirl.mineseekdestroy.service;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntityPositionS2CPacket;
import dev.foxgirl.mineseekdestroy.util.Console;
import dev.foxgirl.mineseekdestroy.util.Fuck;
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

    public void executeSetEnabled(@NotNull Console console) {
        Objects.requireNonNull(console, "Argument 'console'");
        if (isActive()) {
            console.sendError("Invisibility is already enabled");
        } else {
            setActive(true);
            console.sendInfo("Invisibility enabled");
        }
    }

    public void executeSetDisabled(@NotNull Console console) {
        Objects.requireNonNull(console, "Argument 'console'");
        if (isActive()) {
            setActive(false);
            console.sendInfo("Invisibility disabled");
        } else {
            console.sendError("Invisibility is already disabled");
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
                if (!other.isAlive()) continue;
                if (target.getId() == other.getId()) continue;
                if (target.getWorld() != other.getWorld()) continue;
                if (target.squaredDistanceTo(other) > 65536) continue;
                target.networkHandler.sendPacket(new EntityPositionS2CPacket(other));
            }
        }
    }

    public @Nullable EntityPositionS2CPacket handlePositionPacket(@NotNull EntityPositionS2CPacket packet, @NotNull ServerPlayerEntity targetEntity) {
        Objects.requireNonNull(packet, "Argument 'packet'");
        Objects.requireNonNull(targetEntity, "Argument 'targetEntity'");

        if (getGame().isOperator(targetEntity) || !targetEntity.isAlive()) return null;

        var context = getContext();

        var packetId = packet.getId();
        if (packetId == targetEntity.getId()) return null;

        var packetEntity = targetEntity.getWorld().getEntityById(packetId);
        if (packetEntity == null || !packetEntity.isAlive()) return null;

        var packetPlayer = context.getPlayer(packetEntity);
        if (packetPlayer == null) return null;

        var targetPlayer = context.getPlayer(targetEntity);

        var targetTeam = targetPlayer.getTeam();
        var packetTeam = packetPlayer.getTeam();

        if (isActive()) {
            switch (targetTeam) {
                case NONE:
                case OPERATOR:
                    return null;
                case PLAYER_BLACK:
                    if (packetTeam == GameTeam.OPERATOR || packetTeam == GameTeam.PLAYER_BLACK) return null;
                case PLAYER_YELLOW:
                    if (packetTeam == GameTeam.OPERATOR || packetTeam == GameTeam.PLAYER_YELLOW) return null;
                case PLAYER_BLUE:
                    if (packetTeam == GameTeam.OPERATOR || packetTeam == GameTeam.PLAYER_BLUE) return null;
            }
        } else {
            switch (targetTeam) {
                case NONE:
                case OPERATOR:
                    return null;
                case PLAYER_BLACK:
                case PLAYER_YELLOW:
                case PLAYER_BLUE:
                    if (packetTeam != GameTeam.NONE) return null;
            }
        }

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
