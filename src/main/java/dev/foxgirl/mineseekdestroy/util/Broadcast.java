package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.GamePlayer;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public class Broadcast {

    private Broadcast() {}

    public interface PacketSupplier {
        @Nullable Packet<?> get(@NotNull GamePlayer player, @NotNull ServerPlayerEntity playerEntity);
    }

    public static void send(PacketSupplier packetSupplier) {
        var context = Game.getGame().getContext();
        if (context == null) return;

        for (var playerEntity : context.playerManager.getPlayerList()) {
            var packet = packetSupplier.get(context.getPlayer(playerEntity), playerEntity);
            if (packet != null) {
                playerEntity.networkHandler.sendPacket(packet);
            }
        }
    }

    public static void send(Packet<?> packet) {
        send((player, playerEntity) -> packet);
    }

    public interface PositionSupplier {
        @Nullable Position get(@NotNull GamePlayer player, @NotNull ServerPlayerEntity playerEntity);
    }

    public static void sendSound(SoundEvent sound, SoundCategory category, float volume, float pitch, PositionSupplier positionSupplier) {
        var entry = Registries.SOUND_EVENT.getEntry(sound);
        var seed = ThreadLocalRandom.current().nextLong();

        send((player, playerEntity) -> {
            var pos = positionSupplier.get(player, playerEntity);
            if (pos == null) return null;

            return new PlaySoundS2CPacket(
                entry, SoundCategory.PLAYERS, pos.getX(), pos.getY(), pos.getZ(),
                volume, pitch, seed
            );
        });
    }

    public static void sendSound(SoundEvent sound, SoundCategory category, float volume, float pitch, World world, Position position) {
        sendSound(sound, category, volume, pitch, (player, playerEntity) -> {
            if (playerEntity.getWorld() != world) return null;
            if (playerEntity.squaredDistanceTo(position.getX(), position.getY(), position.getZ()) > 65536) return null;
            return position;
        });
    }

    public static void sendSound(SoundEvent sound, SoundCategory category, float volume, float pitch) {
        sendSound(sound, category, volume, pitch, (player, playerEntity) -> new Vec3d(playerEntity.getX(), playerEntity.getEyeY(), playerEntity.getZ()));
    }

    public static void sendSoundPing() {
        var volume = (float) Game.getGame().getRuleDouble(Game.RULE_PING_VOLUME);
        var pitch = (float) Game.getGame().getRuleDouble(Game.RULE_PING_PITCH);
        sendSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.NEUTRAL, volume, pitch);
    }

}
