package dev.foxgirl.mineseekdestroy.service;

import com.google.common.collect.ImmutableList;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntity;
import dev.foxgirl.mineseekdestroy.mixin.MixinEntityTrackerUpdateS2CPacket;
import dev.foxgirl.mineseekdestroy.util.Fuck;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class GlowService extends Service {

    private static final TrackedData<Byte> FLAGS = MixinEntity.mineseekdestroy$getFLAGS();

    private static EntityTrackerUpdateS2CPacket updateFlagsPacket(Entity entity, byte value) {
        var packet = Fuck.create(EntityTrackerUpdateS2CPacket.class);
        var access = (MixinEntityTrackerUpdateS2CPacket) packet;
        access.mineseekdestroy$setId(entity.getId());
        access.mineseekdestroy$setTrackedValues(ImmutableList.of(new DataTracker.Entry<>(FLAGS, value)));
        return packet;
    }

    private static EntityTrackerUpdateS2CPacket updateFlagsFakePacket(Entity entity, boolean glowing) {
        var value = entity.getDataTracker().get(FLAGS);
        if (glowing) {
            value = (byte) (value | 0x40);
        } else {
            value = (byte) (value & 0xBF);
        }
        return updateFlagsPacket(entity, value);
    }

    private static EntityTrackerUpdateS2CPacket updateFlagsRealPacket(Entity entity) {
        return updateFlagsPacket(entity, entity.getDataTracker().get(FLAGS));
    }

    private int broadcastTicks = 0;

    private void broadcast() {
        var players = getPlayers();

        var packetsFake = new ArrayList<EntityTrackerUpdateS2CPacket>(players.size());
        var packetsReal = new ArrayList<EntityTrackerUpdateS2CPacket>(players.size());

        for (var player : players) {
            var entity = player.getEntity();
            if (entity == null) continue;
            boolean glowing = player.isPlaying() && player.isAlive();
            packetsFake.add(updateFlagsFakePacket(entity, glowing));
            packetsReal.add(updateFlagsRealPacket(entity));
        }

        for (var player : players) {
            var entity = player.getEntity();
            if (entity == null) continue;
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

}
