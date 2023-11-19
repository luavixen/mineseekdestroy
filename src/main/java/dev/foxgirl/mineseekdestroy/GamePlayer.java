package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.util.Inventories;
import dev.foxgirl.mineseekdestroy.util.NbtKt;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class GamePlayer {

    private final GameContext context;

    private final UUID uuid;
    private final int hash;

    private final String name;
    private final String nameLowercase;

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

    private int statsSouls = 0;
    private int statsKills = 0;
    private int statsDeaths = 0;

    GamePlayer(@NotNull GameContext context, UUID uuid, String name) {
        Objects.requireNonNull(context, "Argument 'context'");
        Objects.requireNonNull(uuid, "Argument 'uuid'");
        Objects.requireNonNull(name, "Argument 'name'");
        this.context = context;
        this.uuid = uuid;
        this.hash = uuid.hashCode();
        this.name = name;
        this.nameLowercase = name.toLowerCase(Locale.ROOT);
    }

    GamePlayer(@NotNull GameContext context, @NotNull ServerPlayerEntity player) {
        this(context, player.getUuid(), player.getEntityName());
    }

    GamePlayer(@NotNull GameContext context, @NotNull NbtCompound nbt) {
        this(
            context,
            NbtKt.toUUID(nbt.get("UUID")),
            NbtKt.toActualString(nbt.get("Name"))
        );

        currentAlive = NbtKt.toBoolean(nbt.get("Alive"));
        currentTeam = GameTeam.valueOf(NbtKt.toActualString(nbt.get("Team")));
        statsSouls = NbtKt.toInt(nbt.get("Souls"));
        statsKills = NbtKt.toInt(nbt.get("Kills"));
        statsKills = NbtKt.toInt(nbt.get("Deaths"));
    }

    public @NotNull NbtCompound toNbt() {
        var nbt = NbtKt.nbtCompound(16);
        nbt.putUuid("UUID", uuid);
        nbt.putString("Name", name);
        nbt.putBoolean("Alive", currentAlive);
        nbt.putString("Team", currentTeam.name());
        nbt.putInt("Souls", statsSouls);
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

    public @NotNull String getNameLowercase() {
        return nameLowercase;
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

    public int getSouls() {
        return statsSouls;
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
        statsSouls = 0;
        statsKills = 0;
        statsDeaths = 0;
    }

    public @Nullable ServerPlayerEntity getEntity() {
        return context.playerManager.getPlayer(uuid);
    }

    public @Nullable PlayerInventory getInventory() {
        var entity = getEntity();
        return entity != null ? entity.getInventory() : null;
    }

    public boolean isAlive() {
        return currentAlive;
    }

    public boolean isLiving() {
        if (currentAlive) {
            var entity = getEntity();
            return entity != null && entity.isAlive() && entity.networkHandler.isConnectionOpen();
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

    public boolean isCannon() {
        return currentTeam.isCanon();
    }

    public void teleport(@NotNull Position position) {
        var entity = getEntity();
        if (entity != null) {
            entity.teleport(
                context.world,
                position.getX(),
                position.getY(),
                position.getZ(),
                entity.getYaw(),
                entity.getPitch()
            );
        }
    }

    private @NotNull Scoreboard getScoreboard() {
        return context.scoreboard;
    }

    private @NotNull ScoreboardObjective getScoreboardKillsObjective() {
        return context.scoreboardKills;
    }
    private @NotNull ScoreboardObjective getScoreboardSoulsObjective() {
        return context.scoreboardSouls;
    }

    private @Nullable Team getScoreboardAliveTeam() {
        return currentTeam.getTeam(getScoreboard());
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
            var entity = getEntity();
            if (entity != null && entity.isAlive() && entity.networkHandler.isConnectionOpen()) {
                return (
                    entity.getWorld().getTime() - entity.lastDamageTime < 10L && Rules.getDamageFlashEnabled()
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

    private int getSoulsCurrent() {
        var entity = getEntity();
        if (entity != null) {
            int count = 0;
            for (var stack : Inventories.list(entity.getInventory())) {
                var nbt = stack.getNbt();
                if (nbt != null && nbt.contains("MsdSoul")) {
                    count += stack.getCount();
                }
            }
            statsSouls = count;
        }
        return statsSouls;
    }

    public void update() {
        var scoreboard = getScoreboard();
        var scoreboardKillsObjective = getScoreboardKillsObjective();
        var scoreboardSoulsObjective = getScoreboardSoulsObjective();

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
            var playerKillsValue = getKills();
            var playerKillsScore = scoreboard.getPlayerScore(playerName, scoreboardKillsObjective);

            if (playerKillsScore.getScore() != playerKillsValue) {
                playerKillsScore.setScore(playerKillsValue);
            }
        } else {
            if (scoreboard.playerHasObjective(playerName, scoreboardKillsObjective)) {
                scoreboard.resetPlayerScore(playerName, scoreboardKillsObjective);
            }
        }

        {
            var playerSoulsValue = getSoulsCurrent();
            var playerSoulsScore = scoreboard.getPlayerScore(playerName, scoreboardSoulsObjective);

            if (playerSoulsScore.getScore() != playerSoulsValue) {
                playerSoulsScore.setScore(playerSoulsValue);
            }
        }
    }

}
