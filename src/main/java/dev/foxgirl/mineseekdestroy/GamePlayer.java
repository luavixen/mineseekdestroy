package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.util.NbtKt;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
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

    private int statsKills = 0;
    private int statsDeaths = 0;

    private Inventory inventoryMirror = null;

    private GamePlayer(@NotNull GameContext context, UUID uuid, String name) {
        Objects.requireNonNull(context, "Argument 'context'");
        this.context = context;
        this.uuid = uuid;
        this.name = name;
        this.hash = uuid.hashCode();
    }

    GamePlayer(@NotNull GameContext context, @NotNull ServerPlayerEntity player) {
        this(
            context,
            Objects.requireNonNull(player.getUuid(), "Expression 'player.getUuid()'"),
            Objects.requireNonNull(player.getEntityName(), "Expression 'player.getEntityName()'")
        );
    }

    GamePlayer(@NotNull GameContext context, @NotNull NbtCompound nbt) {
        this(
            context,
            NbtKt.toUUID(nbt.get("UUID")),
            NbtKt.toActualString(nbt.get("Name"))
        );

        currentAlive = NbtKt.toBoolean(nbt.get("Alive"));
        currentTeam = GameTeam.valueOf(NbtKt.toActualString(nbt.get("Team")));
        statsKills = NbtKt.toInt(nbt.get("Kills"));
        statsKills = NbtKt.toInt(nbt.get("Deaths"));
    }

    public @NotNull NbtCompound toNbt() {
        var nbt = NbtKt.nbtCompound(8);
        nbt.putUuid("UUID", uuid);
        nbt.putString("Name", name);
        nbt.putBoolean("Alive", currentAlive);
        nbt.putString("Team", currentTeam.name());
        nbt.putInt("Kills", statsKills);
        nbt.putInt("Deaths", statsDeaths);
        return nbt;
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

    public void setTeam(@NotNull GameTeam team) {
        Objects.requireNonNull(team, "Argument 'team'");
        if (currentTeam != team) {
            currentTeam = team;
        }
    }

    public void setAlive(boolean alive) {
        currentAlive = alive;
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

    private @Nullable Team getScoreboardAliveTeam() {
        return currentTeam.getAliveTeam(getScoreboard());
    }
    private @Nullable Team getScoreboardDeadTeam() {
        var team = currentTeam.getDeadTeam(getScoreboard());
        return team != null ? team : getScoreboardAliveTeam();
    }
    private @Nullable Team getScoreboardDamagedTeam() {
        var team = currentTeam.getDamagedTeam(getScoreboard());
        return team != null ? team : getScoreboardAliveTeam();
    }

    public @Nullable Team getScoreboardTeam() {
        if (currentAlive) {
            var player = getEntity();
            if (player != null && player.isAlive() && player.networkHandler.isConnectionOpen()) {
                return (
                    player.getWorld().getTime() - player.lastDamageTime < 10L
                        ? getScoreboardDamagedTeam()
                        : getScoreboardAliveTeam()
                );
            }
        }
        return getScoreboardDeadTeam();
    }

    public @NotNull Text getDisplayName() {
        return Team.decorateName(getScoreboardAliveTeam(), Text.literal(getName()));
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

        if (isOnScoreboard() && !(isGhost() && Game.getGame().getState().isRunning())) {
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
