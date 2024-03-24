package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.service.DamageService;
import dev.foxgirl.mineseekdestroy.util.Inventories;
import dev.foxgirl.mineseekdestroy.util.NbtKt;
import dev.foxgirl.mineseekdestroy.util.Rules;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class GamePlayer {

    private final GameContext context;

    private final UUID uuid;
    private final int hash;

    private final String name;
    private final String nameLowercase;
    private final String nameQuoted;

    private final ScoreHolder scoreboardScoreHolder;

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

    private enum AliveState { ALIVE, UNDEAD, DEAD }

    private AliveState currentAlive = AliveState.ALIVE;
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
        this.nameQuoted = "\"" + name + "\"";
        this.scoreboardScoreHolder = new ScoreHolder() {
            @Override
            public String getNameForScoreboard() {
                return getName();
            }
            @Override
            public @Nullable Text getDisplayName() {
                var entity = getEntity();
                return entity != null ? entity.getDisplayName() : GamePlayer.this.getDisplayName();
            }
        };
    }

    GamePlayer(@NotNull GameContext context, @NotNull ServerPlayerEntity player) {
        this(context, player.getUuid(), player.getNameForScoreboard());
    }

    GamePlayer(@NotNull GameContext context, @NotNull NbtCompound nbt) {
        this(
            context,
            NbtKt.toUUID(nbt.get("UUID")),
            NbtKt.toActualString(nbt.get("Name"))
        );

        currentAlive = AliveState.valueOf(NbtKt.toActualString(nbt.get("Alive")));
        currentTeam = GameTeam.valueOf(NbtKt.toActualString(nbt.get("Team")));
        statsSouls = NbtKt.toInt(nbt.get("Souls"));
        statsKills = NbtKt.toInt(nbt.get("Kills"));
        statsKills = NbtKt.toInt(nbt.get("Deaths"));
    }

    public @NotNull NbtCompound toNbt() {
        var nbt = NbtKt.nbtCompound(16);
        nbt.putUuid("UUID", uuid);
        nbt.putString("Name", name);
        nbt.putString("Alive", currentAlive.name());
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

    public @NotNull String getNameQuoted() {
        return nameQuoted;
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
        currentAlive = alive ? AliveState.ALIVE : AliveState.DEAD;
    }

    public void setUndead(boolean undead) {
        currentAlive = undead ? AliveState.UNDEAD : AliveState.DEAD;
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

    public @NotNull List<DamageService.@NotNull DamageRecord> getGivenDamageRecords() {
        return context.damageService.findRecordsForAttacker(this);
    }
    public @NotNull List<DamageService.@NotNull DamageRecord> getTakenDamageRecords() {
        return context.damageService.findRecordsForVictim(this);
    }

    private boolean cachedGivenDamageValid = false;
    private boolean cachedTakenDamageValid = false;
    private float cachedGivenDamage = 0.0F;
    private float cachedTakenDamage = 0.0F;

    private void uncacheDamageRecords() {
        cachedGivenDamageValid = false;
        cachedTakenDamageValid = false;
    }

    public float getGivenDamage() {
        if (!cachedGivenDamageValid) {
            cachedGivenDamage = DamageService.sumDamageRecords(getGivenDamageRecords());
            cachedGivenDamageValid = true;
        }
        return cachedGivenDamage;
    }
    public float getTakenDamage() {
        if (!cachedTakenDamageValid) {
            cachedTakenDamage = DamageService.sumDamageRecords(getTakenDamageRecords());
            cachedTakenDamageValid = true;
        }
        return cachedTakenDamage;
    }

    public @Nullable ServerPlayerEntity getEntity() {
        return context.playerManager.getPlayer(uuid);
    }

    public @Nullable PlayerInventory getInventory() {
        var entity = getEntity();
        return entity != null ? entity.getInventory() : null;
    }

    public boolean isAlive() {
        return currentAlive != AliveState.DEAD;
    }

    public boolean isUndead() {
        return currentAlive == AliveState.UNDEAD;
    }

    public boolean isLiving() {
        if (isAlive()) {
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

    @Override
    public String toString() {
        return "GamePlayer{name=" + nameQuoted + ", team=" + currentTeam + ", alive=" + currentAlive + "}";
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

    public @NotNull ScoreHolder getScoreHolder() {
        return scoreboardScoreHolder;
    }

    private @NotNull Scoreboard getScoreboard() {
        return context.scoreboard;
    }
    private @NotNull ScoreboardObjective getScoreboardDamageObjective() {
        return context.scoreboardDamage;
    }

    private @Nullable Team getScoreboardAliveTeam() {
        return currentTeam.getTeam(getScoreboard());
    }
    private @Nullable Team getScoreboardDeadTeam() {
        var team = currentTeam.getDeadTeam(getScoreboard());
        return team != null ? team : getScoreboardAliveTeam();
    }
    private @Nullable Team getScoreboardUndeadTeam() {
        var team = currentTeam.getUndeadTeam(getScoreboard());
        return team != null ? team : getScoreboardAliveTeam();
    }
    private @Nullable Team getScoreboardDamagedTeam() {
        var team = currentTeam.getDamagedTeam(getScoreboard());
        return team != null ? team : getScoreboardAliveTeam();
    }

    public @Nullable Team getScoreboardTeam() {
        if (isUndead()) {
            return getScoreboardUndeadTeam();
        }
        if (isAlive()) {
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
        var scoreHolder = getScoreHolder();
        var scoreHolderName = getScoreHolder().getNameForScoreboard();

        var scoreboard = getScoreboard();
        var scoreboardDamageObjective = getScoreboardDamageObjective();

        var teamExpected = getScoreboardTeam();
        var teamActual = scoreboard.getScoreHolderTeam(scoreHolderName);

        if (teamExpected == null) {
            if (teamActual != null) {
                scoreboard.clearTeam(scoreHolderName);
            }
        } else {
            if (!teamExpected.isEqual(teamActual)) {
                scoreboard.addScoreHolderToTeam(scoreHolderName, teamExpected);
            }
        }

        if (isOnScoreboard() && !(isGhost() && Game.getGame().getState().isRunning())) {
            var playerDamageValue = DamageService.damageToScore(getGivenDamage());
            var playerDamageScore = scoreboard.getOrCreateScore(scoreHolder, scoreboardDamageObjective);
            playerDamageScore.setScore(playerDamageValue);
        } else {
            if (scoreboard.getScore(scoreHolder, scoreboardDamageObjective) != null) {
                scoreboard.removeScore(scoreHolder, scoreboardDamageObjective);
            }
        }

        if (!isOperator()) {
            if (context.disguiseService.isDisguised(this)) {
                if (!isUndead()) {
                    context.disguiseService.deactivateDisguise(this);
                }
            } else if (isCannon() && isUndead()) {
                var entity = getEntity();
                if (entity != null && entity.isAlive()) {
                    context.disguiseService.activateDisguise(this);
                }
            }
        }

        getSoulsCurrent();
        uncacheDamageRecords();
    }

}
