package dev.foxgirl.mineseekdestroy.service;

import com.google.common.collect.ImmutableList;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntity;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntityTrackerUpdateS2CPacket;
import dev.foxgirl.mineseekdestroy.util.Fuck;
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

    private static EntityTrackerUpdateS2CPacket createPacket(int id, List<DataTracker.Entry<?>> values) {
        var packet = Fuck.create(EntityTrackerUpdateS2CPacket.class);
        var access = (MixinEntityTrackerUpdateS2CPacket) packet;
        access.mineseekdestroy$setId(id);
        access.mineseekdestroy$setTrackedValues(values);
        return packet;
    }

    private static EntityTrackerUpdateS2CPacket createFlagsPacket(Entity entity, byte value) {
        return createPacket(entity.getId(), ImmutableList.of(new DataTracker.Entry<>(FLAGS, value)));
    }

    private static EntityTrackerUpdateS2CPacket createFakeFlagsPacket(Entity entity, boolean glowing) {
        var value = entity.getDataTracker().get(FLAGS);
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
        var players = getPlayers();

        var packetsFake = new ArrayList<EntityTrackerUpdateS2CPacket>(players.size());
        var packetsReal = new ArrayList<EntityTrackerUpdateS2CPacket>(players.size());

        for (var player : players) {
            var entity = player.getEntity();
            if (entity == null) continue;
            if (entity.getWorld() != world || !entity.isAlive()) continue;
            boolean glowing = player.isPlaying() && player.isAlive();
            packetsFake.add(createFakeFlagsPacket(entity, glowing));
            packetsReal.add(createRealFlagsPacket(entity));
        }

        for (var player : players) {
            var entity = player.getEntity();
            if (entity == null) continue;
            if (entity.getWorld() != world) continue;
            var networkHandler = entity.networkHandler;
            var packets = (player.isPlaying() && player.isAlive()) ? packetsReal : packetsFake;
            for (var packet : packets) {
                networkHandler.sendPacket(packet);
            }
        }
    }

    public void handleUpdate() {
        if (broadcastTicks >= 40) {
            broadcastTicks = ThreadLocalRandom.current().nextInt(6);
            broadcast();
        } else {
            broadcastTicks++;
        }
    }

    @SuppressWarnings("unchecked")
    public @Nullable EntityTrackerUpdateS2CPacket handleTrackerUpdatePacket(@NotNull EntityTrackerUpdateS2CPacket packet, @NotNull ServerPlayerEntity targetEntity) {
        Objects.requireNonNull(packet, "Argument 'packet'");
        Objects.requireNonNull(targetEntity, "Argument 'targetEntity'");

        var valuesOld = packet.getTrackedValues();
        if (valuesOld == null) return null;

        var flags = (DataTracker.Entry<Byte>) null;
        int flagsIndex = 0;
        for (int i = 0, size = valuesOld.size(); i < size; i++) {
            var value = valuesOld.get(i);
            if (value.getData() == FLAGS) {
                flags = (DataTracker.Entry<Byte>) value;
                flagsIndex = i;
            }
        }
        if (flags == null) return null;

        var context = getContext();

        var targetPlayer = context.getPlayer(targetEntity);
        if (targetPlayer.isPlaying() && targetPlayer.isAlive()) return null;

        var packetId = packet.id();
        if (packetId == targetEntity.getId()) return null;

        var packetEntity = targetEntity.getWorld().getEntityById(packetId);
        if (packetEntity == null || !packetEntity.isAlive()) return null;

        var packetPlayer = context.getPlayer(packetEntity);
        if (packetPlayer == null) return null;

        var value = flags.get();
        if (packetPlayer.isPlaying() && packetPlayer.isAlive()) {
            value = (byte) (value | 0x40);
        } else {
            value = (byte) (value & 0xBF);
        }

        var valuesNew = new ArrayList<>(valuesOld);

        valuesNew.remove(flagsIndex);
        valuesNew.add(new DataTracker.Entry<>(FLAGS, value));

        return createPacket(packetId, valuesNew);
    }

}
