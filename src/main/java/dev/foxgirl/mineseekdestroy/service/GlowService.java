package dev.foxgirl.mineseekdestroy.service;

import dev.foxgirl.mineseekdestroy.GameTeam;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntity;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntityTrackerUpdateS2CPacket;
import dev.foxgirl.mineseekdestroy.util.Reflector;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class GlowService extends Service {

    private static final TrackedData<Byte> FLAGS = MixinEntity.mineseekdestroy$getFLAGS();

    private static EntityTrackerUpdateS2CPacket createPacket(int id, List<DataTracker.SerializedEntry<?>> values) {
        var packet = Reflector.create(EntityTrackerUpdateS2CPacket.class);
        var access = (MixinEntityTrackerUpdateS2CPacket) (Object) packet;
        access.mineseekdestroy$setId(id);
        access.mineseekdestroy$setTrackedValues(values);
        return packet;
    }

    private static EntityTrackerUpdateS2CPacket createFlagsPacket(Entity entity, byte value) {
        return createPacket(entity.getId(), ImmutableList.of(DataTracker.SerializedEntry.of(FLAGS, value)));
    }

    private static EntityTrackerUpdateS2CPacket createFakeFlagsPacket(Entity entity, boolean glowing) {
        var value = (byte) entity.getDataTracker().get(FLAGS);
        if (glowing) {
            value = (byte) (value | 0x40);
        } else {
            value = (byte) (value & 0xBF);
        }
        return createFlagsPacket(entity, value);
    }

    private static EntityTrackerUpdateS2CPacket createRealFlagsPacket(Entity entity) {
        return createFlagsPacket(entity, entity.getDataTracker().get(FLAGS));
    }

    private int broadcastTicks = 0;

    private void broadcast() {
        var world = getWorld();
        var context = getContext();
        var players = getPlayers();

        var packetsFake = new ArrayList<EntityTrackerUpdateS2CPacket>(players.size() + 10);
        var packetsReal = new ArrayList<EntityTrackerUpdateS2CPacket>(players.size() + 10);

        for (var player : players) {
            var entity = player.getEntity();
            if (entity == null) continue;
            if (entity.getWorld() != world || !entity.isAlive()) continue;
            boolean glowing = player.isPlayingOrGhost() && player.isAlive();
            packetsFake.add(createFakeFlagsPacket(entity, glowing));
            packetsReal.add(createRealFlagsPacket(entity));
            var disguiseEntity = context.disguiseService.getDisguise(player);
            if (disguiseEntity != null) {
                packetsFake.add(createFakeFlagsPacket(disguiseEntity, glowing));
                packetsReal.add(createRealFlagsPacket(disguiseEntity));
            }
        }

        for (var player : players) {
            var entity = player.getEntity();
            if (entity == null) continue;
            if (entity.getWorld() != world) continue;
            var networkHandler = entity.networkHandler;
            List<EntityTrackerUpdateS2CPacket> packets;
            if (player.isPlaying() && player.isAlive()) {
                if (context.conduitService.shouldMakeCannonPlayersGlow(player)) {
                    packets = packetsFake;
                } else {
                    packets = packetsReal;
                }
            } else {
                packets = packetsFake;
            }
            for (var packet : packets) {
                networkHandler.sendPacket(packet);
            }
        }
    }

    public void broadcastNow() {
        broadcastTicks = ThreadLocalRandom.current().nextInt(6);
        broadcast();
    }

    @Override
    public void update() {
        if (broadcastTicks >= 40) {
            broadcastNow();
        } else {
            broadcastTicks++;
        }
    }

    @SuppressWarnings("unchecked")
    public @Nullable EntityTrackerUpdateS2CPacket handleTrackerUpdatePacket(@NotNull EntityTrackerUpdateS2CPacket packet, @NotNull ServerPlayerEntity targetEntity) {
        Objects.requireNonNull(packet, "Argument 'packet'");
        Objects.requireNonNull(targetEntity, "Argument 'targetEntity'");

        var valuesOld = packet.trackedValues();
        if (valuesOld == null) return null;

        var flags = (DataTracker.SerializedEntry<Byte>) null;
        int flagsIndex = 0;
        for (int i = 0, size = valuesOld.size(); i < size; i++) {
            var value = valuesOld.get(i);
            if (value.id() == FLAGS.getId()) {
                flags = (DataTracker.SerializedEntry<Byte>) value;
                flagsIndex = i;
            }
        }
        if (flags == null) return null;

        var context = getContext();

        var targetPlayer = context.getPlayer(targetEntity);

        var packetId = packet.id();
        if (packetId == targetEntity.getId()) return null;

        var packetEntity = targetEntity.getWorld().getEntityById(packetId);
        if (packetEntity == null || !packetEntity.isAlive()) return null;

        var packetPlayer = context.getPlayer(packetEntity);
        if (packetPlayer == null) {
            packetPlayer = context.disguiseService.getDisguisedPlayer(packetEntity);
            if (packetPlayer == null) return null;
        }

        if (
            context.conduitService.shouldMakeCannonPlayersGlow(packetPlayer) &&
            targetPlayer.getTeam().isCanon() && targetPlayer.isPlaying() && targetPlayer.isAlive()
        ) {

            // Black conduit exception, make cannon players glow!

        } else {

            if (
                targetPlayer.isPlaying() && targetPlayer.isAlive() &&
                targetPlayer.getTeam() != packetPlayer.getTeam()
            ) return null;

            if (
                targetPlayer.getTeam() == GameTeam.DUELIST &&
                packetPlayer.getTeam() == GameTeam.DUELIST
            ) return null;

        }

        var value = (byte) flags.value();
        if (packetPlayer.isPlayingOrGhost() && packetPlayer.isAlive()) {
            value = (byte) (value | 0x40);
        } else {
            value = (byte) (value & 0xBF);
        }

        var valuesNew = new ArrayList<>(valuesOld);

        valuesNew.remove(flagsIndex);
        valuesNew.add(DataTracker.SerializedEntry.of(FLAGS, value));

        return createPacket(packetId, valuesNew);
    }

}
