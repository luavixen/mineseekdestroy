package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.event.AliveChangeEvent;
import dev.foxgirl.mineseekdestroy.event.Bus;
import dev.foxgirl.mineseekdestroy.event.GamePlayerSerializer;
import dev.foxgirl.mineseekdestroy.event.TeamChangeEvent;
import dev.foxgirl.mineseekdestroy.state.RunningGameState;
import kotlinx.serialization.Serializable;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Serializable(with = GamePlayerSerializer.class)
public final class GamePlayer {

    private final GameContext context;

    private final UUID uuid;
    private final String name;

    private final int hash;

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof GamePlayer player) {
            return context == player.context && uuid.equals(player.uuid);
        }
        return false;
    }

    private boolean currentAlive = true;

    private GameTeam currentTeam = GameTeam.NONE;
    private GameTeam previousTeam = GameTeam.NONE;

    private int statsKills = 0;
    private int statsDeaths = 0;

    private Inventory inventoryMirror = null;

    GamePlayer(@NotNull GameContext context, @NotNull ServerPlayerEntity player) {
        Objects.requireNonNull(context, "Argument 'context'");
        Objects.requireNonNull(player, "Argument 'player'");

        this.context = context;

        uuid = Objects.requireNonNull(player.getUuid(), "Expression 'player.getUuid()'");
        name = Objects.requireNonNull(player.getEntityName(), "Expression 'player.getEntityName()'");

        hash = uuid.hashCode();
    }

    public @NotNull UUID getUUID() {
        return uuid;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull GameTeam getTeam() {
        return currentTeam;
    }

    public @NotNull GameTeam getPreviousTeam() {
        return previousTeam;
    }

    public void setTeam(@NotNull GameTeam team) {
        Objects.requireNonNull(team, "Argument 'team'");
        if (currentTeam != team) {
            previousTeam = currentTeam;
            currentTeam = team;
            Bus.publish(new TeamChangeEvent(this, previousTeam, currentTeam));
        }
    }

    public void setAlive(boolean newAlive) {
        var oldAlive = currentAlive;
        if (oldAlive != newAlive) {
            currentAlive = newAlive;
            Bus.publish(new AliveChangeEvent(this, oldAlive, newAlive));
        }
    }

    public int getKills() {
        return statsKills;
    }

    public void setKills(int kills) {
        statsKills = kills;
    }

    public int getDeaths() {
        return statsDeaths;
    }

    public void setDeaths(int deaths) {
        statsDeaths = deaths;
    }

    public void countKill() {
        statsKills++;
    }

    public void countDeath() {
        statsDeaths++;
    }

    public void clearStats() {
        statsKills = 0;
        statsDeaths = 0;
    }

    public @Nullable ServerPlayerEntity getEntity() {
        return context.playerManager.getPlayer(uuid);
    }

    public @Nullable PlayerInventory getInventory() {
        var player = getEntity();
        return player != null ? player.getInventory() : null;
    }

    public @Nullable Inventory getInventoryMirror() {
        return inventoryMirror;
    }

    public void setInventoryMirror(@Nullable Inventory mirror) {
        inventoryMirror = mirror;
    }

    public boolean isAlive() {
        return currentAlive;
    }

    public boolean isLiving() {
        if (currentAlive) {
            var player = getEntity();
            return player != null && player.isAlive() && player.networkHandler.isConnectionOpen();
        }
        return false;
    }

    public boolean isPlaying() {
        return currentTeam.isPlaying();
    }

    public boolean isOperator() {
        return currentTeam.isOperator();
    }

    public boolean isSpectator() {
        return currentTeam.isSpectator();
    }

    public boolean isGhost() {
        return currentTeam.isGhost();
    }

    public boolean isPlayingOrGhost() {
        return currentTeam.isPlayingOrGhost();
    }

    public boolean isOnScoreboard() {
        return currentTeam.isOnScoreboard();
    }

    public void teleport(@NotNull Position position) {
        var player = getEntity();
        if (player != null) {
            player.teleport(
                context.world,
                position.getX(),
                position.getY(),
                position.getZ(),
                player.getYaw(),
                player.getPitch()
            );
        }
    }

    private @NotNull Scoreboard getScoreboard() {
        return context.scoreboard;
    }

    private @NotNull ScoreboardObjective getScoreboardObjective() {
        return context.scoreboardKills;
    }

    public @Nullable Team getScoreboardTeam() {
        if (currentAlive) {
            var player = getEntity();
            if (player != null && player.isAlive() && player.networkHandler.isConnectionOpen()) {
                return (
                    player.getWorld().getTime() - player.lastDamageTime < 10L
                        ? currentTeam.getDamagedTeam(getScoreboard())
                        : currentTeam.getAliveTeam(getScoreboard())
                );
            }
        }
        return currentTeam.getDeadTeam(getScoreboard());
    }

    public @NotNull Text getDisplayName() {
        return Team.decorateName(currentTeam.getAliveTeam(getScoreboard()), Text.literal(getName()));
    }

    public void update() {
        var scoreboard = getScoreboard();
        var scoreboardObjective = getScoreboardObjective();

        var playerName = getName();

        var teamExpected = getScoreboardTeam();
        var teamActual = scoreboard.getPlayerTeam(playerName);

        if (teamExpected == null) {
            if (teamActual != null) {
                scoreboard.clearPlayerTeam(playerName);
            }
        } else {
            if (!teamExpected.isEqual(teamActual)) {
                scoreboard.addPlayerToTeam(playerName, teamExpected);
            }
        }

        if (isOnScoreboard() && !(isGhost() && Game.getGame().getState() instanceof RunningGameState)) {
            var playerKills = getKills();
            var playerScore = scoreboard.getPlayerScore(playerName, scoreboardObjective);

            if (playerScore.getScore() != playerKills) {
                playerScore.setScore(playerKills);
            }
        } else {
            if (scoreboard.playerHasObjective(playerName, scoreboardObjective)) {
                scoreboard.resetPlayerScore(playerName, scoreboardObjective);
            }
        }
    }

}
