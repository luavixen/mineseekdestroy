package dev.foxgirl.mineseekdestroy.state;

import dev.foxgirl.mineseekdestroy.GameContext;
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
        return null;
    }

}
