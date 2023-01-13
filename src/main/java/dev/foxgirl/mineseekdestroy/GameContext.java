package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.service.*;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GameContext {

    private static final String scoreboardKillsName = "msd_kills";
    private static final Text scoreboardKillsDisplayName = Text.of("Mine n Seek n Destroy");

    private static final String scoreboardHeartsName = "msd_hearts";
    private static final Text scoreboardHeartsDisplayName = Text.of("Health");

    public final @NotNull Game game;

    public final @NotNull MinecraftServer server;
    public final @NotNull ServerWorld world;

    public final @NotNull Scoreboard scoreboard;
    public final @NotNull ScoreboardObjective scoreboardKills;
    public final @NotNull ScoreboardObjective scoreboardHearts;

    public final @NotNull Team teamOperator;
    public final @NotNull Team teamBlack;
    public final @NotNull Team teamBlackDead;
    public final @NotNull Team teamYellow;
    public final @NotNull Team teamYellowDead;
    public final @NotNull Team teamBlue;
    public final @NotNull Team teamBlueDead;

    public final @NotNull PlayerManager playerManager;

    private final Map<UUID, GamePlayer> playerMap;
    private final Object playerMapLock;

    public final @NotNull InventoryService inventoryService;
    public final @NotNull LootService lootService;
    public final @NotNull ArmorService armorService;
    public final @NotNull InvisibilityService invisibilityService;
    public final @NotNull BarrierService barrierService;
    public final @NotNull SaturationService saturationService;
    public final @NotNull GlowService glowService;
    public final @NotNull PowderService powderService;
    public final @NotNull SnapshotService snapshotService;
    public final @NotNull TowerService towerService;

    GameContext(@NotNull Game game) {
        Objects.requireNonNull(game, "Argument 'game'");

        this.game = game;

        game.setState(new WaitingGameState());

        server = Objects.requireNonNull(game.getServer());
        world = Objects.requireNonNull(game.getServer().getOverworld());

        scoreboard = server.getScoreboard();

        var scoreboardKillsOld = scoreboard.getObjective(scoreboardKillsName);
        if (scoreboardKillsOld != null) {
            scoreboard.removeObjective(scoreboardKillsOld);
        }

        scoreboardKills = scoreboard.addObjective(
            scoreboardKillsName,
            ScoreboardCriterion.DUMMY,
            scoreboardKillsDisplayName,
            ScoreboardCriterion.RenderType.INTEGER
        );

        scoreboard.setObjectiveSlot(Scoreboard.SIDEBAR_DISPLAY_SLOT_ID, scoreboardKills);

        var scoreboardHeartsOld = scoreboard.getObjective(scoreboardHeartsName);
        if (scoreboardHeartsOld != null) {
            scoreboard.removeObjective(scoreboardHeartsOld);
        }

        scoreboardHearts = scoreboard.addObjective(
            scoreboardHeartsName,
            ScoreboardCriterion.HEALTH,
            scoreboardHeartsDisplayName,
            ScoreboardCriterion.RenderType.HEARTS
        );

        scoreboard.setObjectiveSlot(Scoreboard.LIST_DISPLAY_SLOT_ID, scoreboardHearts);

        scoreboard.getTeams().removeIf(team -> team.getName().startsWith("msd_"));

        teamOperator = Objects.requireNonNull(GameTeam.OPERATOR.getAliveTeam(scoreboard));
        teamBlack = Objects.requireNonNull(GameTeam.PLAYER_BLACK.getAliveTeam(scoreboard));
        teamBlackDead = Objects.requireNonNull(GameTeam.PLAYER_BLACK.getDeadTeam(scoreboard));
        teamYellow = Objects.requireNonNull(GameTeam.PLAYER_YELLOW.getAliveTeam(scoreboard));
        teamYellowDead = Objects.requireNonNull(GameTeam.PLAYER_YELLOW.getDeadTeam(scoreboard));
        teamBlue = Objects.requireNonNull(GameTeam.PLAYER_BLUE.getAliveTeam(scoreboard));
        teamBlueDead = Objects.requireNonNull(GameTeam.PLAYER_BLUE.getDeadTeam(scoreboard));

        playerManager = server.getPlayerManager();

        playerMap = new HashMap<>();
        playerMapLock = new Object();

        inventoryService = new InventoryService();
        lootService = new LootService();
        armorService = new ArmorService();
        invisibilityService = new InvisibilityService();
        barrierService = new BarrierService();
        saturationService = new SaturationService();
        glowService = new GlowService();
        powderService = new PowderService();
        snapshotService = new SnapshotService();
        towerService = new TowerService();
    }

    public void initialize() {
        syncPlayers();

        for (var operator : Game.OPERATORS) {
            var player = getPlayer(operator);
            if (player != null) {
                player.setTeam(GameTeam.OPERATOR);
            }
        }

        server.setDifficulty(Difficulty.NORMAL, true);

        game.getRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE).set(200, server);
        game.getRule(GameRules.KEEP_INVENTORY).set(true, server);
        game.getRule(GameRules.DO_FIRE_TICK).set(false, server);
        game.getRule(GameRules.DO_MOB_GRIEFING).set(false, server);

        inventoryService.initialize(this);
        lootService.initialize(this);
        armorService.initialize(this);
        invisibilityService.initialize(this);
        barrierService.initialize(this);
        saturationService.initialize(this);
        glowService.initialize(this);
        powderService.initialize(this);
        snapshotService.initialize(this);
        towerService.initialize(this);
    }

    public void destroy() {
        scoreboard.removeObjective(scoreboardKills);
        scoreboard.removeObjective(scoreboardHearts);
        scoreboard.removeTeam(teamOperator);
        scoreboard.removeTeam(teamBlack);
        scoreboard.removeTeam(teamBlackDead);
        scoreboard.removeTeam(teamYellow);
        scoreboard.removeTeam(teamYellowDead);
        scoreboard.removeTeam(teamBlue);
        scoreboard.removeTeam(teamBlueDead);
    }

    public @NotNull List<@NotNull GamePlayer> getPlayers() {
        Object[] players;
        synchronized (playerMapLock) {
            players = playerMap.values().toArray();
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<GamePlayer> list = (List) Arrays.asList(players);
        return list;
    }

    public @NotNull List<@NotNull GamePlayer> getPlayers(@NotNull Collection<?> values) {
        Objects.requireNonNull(values, "Argument 'values'");
        var objects = new ArrayList<>(values);
        var players = new ArrayList<GamePlayer>(objects.size());
        synchronized (playerMapLock) {
            for (Object obj : objects) {
                if (obj == null) {
                    throw new IllegalArgumentException("Argument 'values' contains null element" );
                }
                if (obj instanceof GamePlayer) {
                    players.add((GamePlayer) obj);
                } else if (obj instanceof ServerPlayerEntity) {
                    players.add(getPlayer((ServerPlayerEntity) obj));
                } else if (obj instanceof Entity) {
                    players.add(Objects.requireNonNull(getPlayer((Entity) obj), "Expression 'getPlayer((Entity) obj)'"));
                } else if (obj instanceof UUID) {
                    players.add(Objects.requireNonNull(getPlayer((UUID) obj), "Expression 'getPlayer((UUID) obj)'"));
                } else if (obj instanceof String) {
                    players.add(Objects.requireNonNull(getPlayer((String) obj), "Expression 'getPlayer((String) obj)'"));
                } else {
                    throw new IllegalArgumentException(
                        "Argument 'values' contains element of unexpected type: " + obj.getClass().getName()
                    );
                }
            }
        }
        return players;
    }

    public void syncPlayers() {
        var players = playerManager.getPlayerList().toArray();
        synchronized (playerMapLock) {
            for (var player : players) {
                getPlayer((ServerPlayerEntity) player);
            }
        }
    }
    public void updatePlayers() {
        getPlayers().forEach(GamePlayer::update);
    }

    public @Nullable GamePlayer getPlayer(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "Argument 'entity'");
        return getPlayer(entity.getUuid());
    }

    public @Nullable GamePlayer getPlayer(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "Argument 'uuid'");
        synchronized (playerMapLock) {
            return playerMap.get(uuid);
        }
    }

    public @Nullable GamePlayer getPlayer(@NotNull String name) {
        Objects.requireNonNull(name, "Argument 'name'");
        synchronized (playerMapLock) {
            for (var player : playerMap.values()) {
                if (Objects.equals(player.getName(), name)) {
                    return player;
                }
            }
        }
        return null;
    }

    public @NotNull GamePlayer getPlayer(@NotNull ServerPlayerEntity player) {
        Objects.requireNonNull(player, "Argument 'player'");
        var uuid = Objects.requireNonNull(player.getUuid(), "Expression 'player.getUuid()'");
        synchronized (playerMapLock) {
            var wrapper = playerMap.get(uuid);
            if (wrapper == null) {
                wrapper = new GamePlayer(this, player);
                playerMap.put(uuid, wrapper);
            }
            return wrapper;
        }
    }

}
