package dev.foxgirl.mineseekdestroy.service;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntityPositionS2CPacket;
import dev.foxgirl.mineseekdestroy.util.Console;
import dev.foxgirl.mineseekdestroy.util.Reflector;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

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

    private int broadcastTicks = 0;

    private void broadcast() {
        var context = getContext();
        var players = ImmutableList.copyOf(context.playerManager.getPlayerList());
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

    @Override
    public void update() {
        boolean immediate = false;
        synchronized (lock) {
            if (activePrevious != activeCurrent) {
                activePrevious = activeCurrent;
                immediate = true;
            }
        }
        if (immediate || broadcastTicks >= 40) {
            broadcastTicks = ThreadLocalRandom.current().nextInt(6);
            broadcast();
        } else {
            broadcastTicks++;
        }
    }

    private boolean isVisibleTo(GameTeam targetTeam, GameTeam packetTeam, Entity packetEntity) {
        return isActive()
            ? isVisibleToActive(targetTeam, packetTeam, packetEntity)
            : isVisibleToInactive(targetTeam, packetTeam, packetEntity);
    }
    private static boolean isVisibleToActive(GameTeam targetTeam, GameTeam packetTeam, Entity packetEntity) {
        return switch (targetTeam) {
            case NONE, SKIP, GHOST, OPERATOR -> true;
            case DUELIST -> packetTeam.isOperator();
            default -> packetTeam.isOperator() || packetTeam == targetTeam;
        };
    }
    private static boolean isVisibleToInactive(GameTeam targetTeam, GameTeam packetTeam, Entity packetEntity) {
        return switch (targetTeam) {
            case NONE, SKIP, GHOST, OPERATOR -> true;
            default ->
                !packetTeam.isSpectator() ||
                Game.getGameProperties().getRegionBlimp().contains(packetEntity) ||
                Game.getGameProperties().getRegionBlimpBalloons().contains(packetEntity);
        };
    }

    public static EntityPositionS2CPacket createInvisiblePositionPacket(int id) {
        var position = Game.getGameProperties().getPositionHell();
        var packet = Reflector.create(EntityPositionS2CPacket.class);
        var access = (MixinEntityPositionS2CPacket) packet;
        access.mineseekdestroy$setId(id);
        access.mineseekdestroy$setX(position.getX());
        access.mineseekdestroy$setY(position.getY());
        access.mineseekdestroy$setZ(position.getZ());
        return packet;
    }

    public @Nullable EntityPositionS2CPacket handlePositionPacket(@NotNull EntityPositionS2CPacket packet, @NotNull ServerPlayerEntity targetEntity) {
        Objects.requireNonNull(packet, "Argument 'packet'");
        Objects.requireNonNull(targetEntity, "Argument 'targetEntity'");

        var context = getContext();

        var packetId = packet.getId();
        if (packetId == targetEntity.getId()) return null;

        var packetEntity = targetEntity.getWorld().getEntityById(packetId);
        if (packetEntity == null || !packetEntity.isAlive()) return null;

        var packetPlayer = context.getPlayer(packetEntity);
        if (packetPlayer == null) return null;

        var targetPlayer = context.getPlayer(targetEntity);

        var packetTeam = packetPlayer.getTeam();
        var targetTeam = targetPlayer.getTeam();

        if (context.disguiseService.isDisguised(packetPlayer)) {
            return createInvisiblePositionPacket(packetId);
        }

        if (getState().isWaiting()) return null;

        if (getGame().isOperator(targetEntity) || !targetEntity.isAlive()) return null;

        if (isVisibleTo(targetTeam, packetTeam, packetEntity)) return null;

        return createInvisiblePositionPacket(packetId);
    }

}
