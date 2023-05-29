package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.service.*;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableMap;
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

    public final @NotNull Team teamSkip;
    public final @NotNull Team teamOperator;
    public final @NotNull Team teamDuel;
    public final @NotNull Team teamDuelDead;
    public final @NotNull Team teamWarden;
    public final @NotNull Team teamWardenDead;
    public final @NotNull Team teamBlack;
    public final @NotNull Team teamBlackDead;
    public final @NotNull Team teamYellow;
    public final @NotNull Team teamYellowDead;
    public final @NotNull Team teamBlue;
    public final @NotNull Team teamBlueDead;

    public final @NotNull PlayerManager playerManager;

    private final HashMap<UUID, GamePlayer> playerMap;
    private final Object playerMapLock;

    public final @NotNull InventoryService inventoryService;
    public final @NotNull LootService lootService;
    public final @NotNull ArmorService armorService;
    public final @NotNull InvisibilityService invisibilityService;
    public final @NotNull BarrierService barrierService;
    public final @NotNull SaturationService saturationService;
    public final @NotNull GlowService glowService;
    public final @NotNull ItemService itemService;
    public final @NotNull SnapshotService snapshotService;
    public final @NotNull StormService stormService;
    public final @NotNull SmokerService smokerService;
    public final @NotNull AutomationService automationService;
    public final @NotNull SpecialTowerService specialTowerService;
    public final @NotNull SpecialGhostService specialGhostService;
    public final @NotNull SpecialCarService specialCarService;
    public final @NotNull SpecialSummonsService specialSummonsService;
    public final @NotNull SpecialPianoService specialPianoService;
    public final @NotNull SpecialFamilyGuyService specialFamilyGuyService;

    private final Service[] services;

    private List<GamePlayer> cachePlayers;
    private List<GamePlayer> cachePlayersNormal;
    private List<GamePlayer> cachePlayersIn;
    private List<GamePlayer> cachePlayersOut;
    private Map<GamePlayer, ServerPlayerEntity> cachePlayerEntities;
    private Map<GamePlayer, ServerPlayerEntity> cachePlayerEntitiesNormal;
    private Map<GamePlayer, ServerPlayerEntity> cachePlayerEntitiesIn;
    private Map<GamePlayer, ServerPlayerEntity> cachePlayerEntitiesOut;

    GameContext(@NotNull Game game) {
        Objects.requireNonNull(game, "Argument 'game'");

        this.game = game;

        game.setState(new WaitingGameState());

        server = Objects.requireNonNull(game.getServer());
        world = Objects.requireNonNull(game.getServer().getOverworld());

        world.setSpawnPos(game.getProperties().getPositionSpawn(), 0.0F);

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

        teamSkip = Objects.requireNonNull(GameTeam.SKIP.getAliveTeam(scoreboard));
        teamOperator = Objects.requireNonNull(GameTeam.OPERATOR.getAliveTeam(scoreboard));
        teamDuel = Objects.requireNonNull(GameTeam.PLAYER_DUEL.getAliveTeam(scoreboard));
        teamDuelDead = Objects.requireNonNull(GameTeam.PLAYER_DUEL.getDeadTeam(scoreboard));
        teamWarden = Objects.requireNonNull(GameTeam.PLAYER_WARDEN.getAliveTeam(scoreboard));
        teamWardenDead = Objects.requireNonNull(GameTeam.PLAYER_WARDEN.getDeadTeam(scoreboard));
        teamBlack = Objects.requireNonNull(GameTeam.PLAYER_BLACK.getAliveTeam(scoreboard));
        teamBlackDead = Objects.requireNonNull(GameTeam.PLAYER_BLACK.getDeadTeam(scoreboard));
        teamYellow = Objects.requireNonNull(GameTeam.PLAYER_YELLOW.getAliveTeam(scoreboard));
        teamYellowDead = Objects.requireNonNull(GameTeam.PLAYER_YELLOW.getDeadTeam(scoreboard));
        teamBlue = Objects.requireNonNull(GameTeam.PLAYER_BLUE.getAliveTeam(scoreboard));
        teamBlueDead = Objects.requireNonNull(GameTeam.PLAYER_BLUE.getDeadTeam(scoreboard));

        playerManager = server.getPlayerManager();

        playerMap = new HashMap<>(32);
        playerMapLock = new Object();

        services = new Service[] {
            inventoryService = new InventoryService(),
            lootService = new LootService(),
            armorService = new ArmorService(),
            invisibilityService = new InvisibilityService(),
            barrierService = new BarrierService(),
            saturationService = new SaturationService(),
            glowService = new GlowService(),
            itemService = new ItemService(),
            snapshotService = new SnapshotService(),
            stormService = new StormService(),
            smokerService = new SmokerService(),
            automationService = new AutomationService(),
            specialTowerService = new SpecialTowerService(),
            specialGhostService = new SpecialGhostService(),
            specialCarService = new SpecialCarService(),
            specialSummonsService = new SpecialSummonsService(),
            specialPianoService = new SpecialPianoService(),
            specialFamilyGuyService = new SpecialFamilyGuyService(),
        };
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

        game.setRuleInt(GameRules.RANDOM_TICK_SPEED, GameRules.DEFAULT_RANDOM_TICK_SPEED);
        game.setRuleInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 200);
        game.setRuleBoolean(GameRules.KEEP_INVENTORY, true);
        game.setRuleBoolean(GameRules.DO_FIRE_TICK, false);
        game.setRuleBoolean(GameRules.DO_MOB_GRIEFING, false);
        game.setRuleBoolean(GameRules.DO_DAYLIGHT_CYCLE, true);

        game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, true);
        game.setRuleBoolean(Game.RULE_SUMMONS_ENABLED, true);
        game.setRuleBoolean(Game.RULE_GHOSTS_ENABLED, false);
        game.setRuleDouble(Game.RULE_BORDER_CLOSE_DURATION, 180.0);
        game.setRuleBoolean(Game.RULE_KILLZONE_BOUNDS_ENABLED, true);
        game.setRuleBoolean(Game.RULE_KILLZONE_BLIMP_ENABLED, true);

        game.getProperties().setup(this);

        for (var service : services) {
            service.initialize(this);
        }
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

    public void update() {
        for (var service : services) {
            service.update();
        }
        for (var player : getPlayers()) {
            player.update();
        }
    }

    @FunctionalInterface
    private interface PlayerPredicate {
        boolean test(GamePlayer player);
    }

    private static final PlayerPredicate isPlayerNormal =
        (player) -> (!player.isOperator());
    private static final PlayerPredicate isPlayerIn =
        (player) -> (!player.isOperator() && (player.isPlaying() && player.isAlive()));
    private static final PlayerPredicate isPlayerOut =
        (player) -> (!player.isOperator() && (!player.isPlaying() || !player.isAlive()));

    private List<GamePlayer> filterPlayers(List<GamePlayer> playerList, PlayerPredicate predicate) {
        var builder = ImmutableList.<GamePlayer>builder(playerList.size());
        for (var player : playerList) {
            if (predicate.test(player)) {
                builder.add(player);
            }
        }
        return builder.build();
    }
    private Map<GamePlayer, ServerPlayerEntity> filterPlayerEntities(Map<GamePlayer, ServerPlayerEntity> playerMap, PlayerPredicate predicate) {
        var builder = ImmutableMap.<GamePlayer, ServerPlayerEntity>builder(playerMap.size());
        for (var entry : playerMap.entrySet()) {
            if (predicate.test(entry.getKey())) {
                builder.put(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public void syncPlayers() {
        var entities = playerManager.getPlayerList().toArray();

        ImmutableMap.Builder<GamePlayer, ServerPlayerEntity> playerEntitiesBuilder = ImmutableMap.builder(entities.length);
        ImmutableMap<GamePlayer, ServerPlayerEntity> playerEntities;

        ImmutableList<GamePlayer> playerList;

        synchronized (playerMapLock) {
            for (int i = 0, length = entities.length; i < length; i++) {
                var playerEntity = (ServerPlayerEntity) entities[i];
                var player = getPlayer(playerEntity);
                playerEntitiesBuilder.put(player, playerEntity);
            }
            playerList = ImmutableList.copyOf(playerMap.values());
        }

        playerEntities = playerEntitiesBuilder.build();

        cachePlayers = playerList;
        cachePlayersNormal = filterPlayers(playerList, isPlayerNormal);
        cachePlayersIn = filterPlayers(playerList, isPlayerIn);
        cachePlayersOut = filterPlayers(playerList, isPlayerOut);
        cachePlayerEntities = playerEntities;
        cachePlayerEntitiesNormal = filterPlayerEntities(playerEntities, isPlayerNormal);
        cachePlayerEntitiesIn = filterPlayerEntities(playerEntities, isPlayerIn);
        cachePlayerEntitiesOut = filterPlayerEntities(playerEntities, isPlayerOut);
    }

    public @NotNull List<@NotNull GamePlayer> getPlayers(@NotNull Collection<? extends ServerPlayerEntity> collection) {
        Objects.requireNonNull(collection, "Argument 'collection'");
        var entities = ImmutableList.<ServerPlayerEntity>copyOf(collection);
        var players = ImmutableList.<GamePlayer>builder(entities.size());
        synchronized (playerMapLock) {
            for (ServerPlayerEntity entity : entities) players.add(getPlayer(entity));
        }
        return players.build();
    }

    public @NotNull List<@NotNull GamePlayer> getPlayers() {
        return Objects.requireNonNull(cachePlayers, "Expression 'cachePlayers'");
    }
    public @NotNull List<@NotNull GamePlayer> getPlayersNormal() {
        return Objects.requireNonNull(cachePlayersNormal, "Expression 'cachePlayersNormal'");
    }
    public @NotNull List<@NotNull GamePlayer> getPlayersIn() {
        return Objects.requireNonNull(cachePlayersIn, "Expression 'cachePlayersIn'");
    }
    public @NotNull List<@NotNull GamePlayer> getPlayersOut() {
        return Objects.requireNonNull(cachePlayersOut, "Expression 'cachePlayersOut'");
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntities() {
        return Objects.requireNonNull(cachePlayerEntities, "Expression 'cachePlayerEntities'");
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntitiesNormal() {
        return Objects.requireNonNull(cachePlayerEntitiesNormal, "Expression 'cachePlayerEntitiesNormal'");
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntitiesIn() {
        return Objects.requireNonNull(cachePlayerEntitiesIn, "Expression 'cachePlayerEntitiesIn'");
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntitiesOut() {
        return Objects.requireNonNull(cachePlayerEntitiesOut, "Expression 'cachePlayerEntitiesOut'");
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

    public @Nullable GamePlayer getPlayer(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "Argument 'entity'");
        return getPlayer(entity.getUuid());
    }

    public @NotNull GamePlayer getPlayer(@NotNull ServerPlayerEntity entity) {
        Objects.requireNonNull(entity, "Argument 'entity'");
        var uuid = Objects.requireNonNull(entity.getUuid(), "Expression 'entity.getUuid()'");
        synchronized (playerMapLock) {
            var wrapper = playerMap.get(uuid);
            if (wrapper == null) {
                wrapper = new GamePlayer(this, entity);
                playerMap.put(uuid, wrapper);
            }
            return wrapper;
        }
    }

}
