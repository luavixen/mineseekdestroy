package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.GameContext;
import dev.foxgirl.mineseekdestroy.util.Broadcast;
import dev.foxgirl.mineseekdestroy.util.TextsKt;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FrozenGameState extends IdlingGameState {

    @Override
    public @NotNull String getName() {
        return "frozen";
    }

    private record FrozenPlayerState(Vec3d position, float health) {}
    private final Map<UUID, FrozenPlayerState> frozenPlayers = new HashMap<>(32);

    private int ticks = 0;

    public void setFrozenPlayer(@NotNull UUID uuid, @NotNull Vec3d position, float health) {
        frozenPlayers.put(uuid, new FrozenPlayerState(position, health));
    }

    @Override
    protected @Nullable GameState onUpdate(@NotNull GameContext context) {
        for (var playerPair : context.getPlayerEntitiesNormal()) {
            var playerEntity = playerPair.getSecond();
            if (playerEntity.isDead() || playerEntity.isRemoved()) continue;
            var frozenPlayerState = frozenPlayers.get(playerEntity.getUuid());
            if (frozenPlayerState == null) {
                frozenPlayers.put(playerEntity.getUuid(), new FrozenPlayerState(playerEntity.getPos(), playerEntity.getHealth()));
            } else {
                if (playerEntity.squaredDistanceTo(frozenPlayerState.position) > 0.25D) {
                    playerEntity.teleport(
                        frozenPlayerState.position.x,
                        frozenPlayerState.position.y,
                        frozenPlayerState.position.z
                    );
                }
                playerEntity.setHealth(frozenPlayerState.health);
            }
        }
        if (ticks++ % 10 == 0) {
            Broadcast.send(new TitleFadeS2CPacket(20, 80, 20));
            Broadcast.send(new TitleS2CPacket(TextsKt.text("GAME FROZEN")));
            Broadcast.send(new SubtitleS2CPacket(TextsKt.text("Waiting for game to restart...")));
            Broadcast.send(new OverlayMessageS2CPacket(TextsKt.text()));
        }
        return null;
    }

}
