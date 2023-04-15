package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.service.*;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import dev.foxgirl.mineseekdestroy.util.collect.ArrayMap;
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

    private final Service[] services;

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

        game.setRuleInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 200);
        game.setRuleBoolean(GameRules.KEEP_INVENTORY, true);
        game.setRuleBoolean(GameRules.DO_FIRE_TICK, false);
        game.setRuleBoolean(GameRules.DO_MOB_GRIEFING, false);

        game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, false);
        game.setRuleBoolean(Game.RULE_GHOSTS_ENABLED, false);
        game.setRuleDouble(Game.RULE_BORDER_CLOSE_DURATION, 180.0);

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

    public void syncPlayers() {
        var players = playerManager.getPlayerList().toArray();
        synchronized (playerMapLock) {
            for (var player : players) getPlayer((ServerPlayerEntity) player);
        }
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

    public @NotNull List<@NotNull GamePlayer> getPlayers(@NotNull Collection<? extends ServerPlayerEntity> collection) {
        Objects.requireNonNull(collection, "Argument 'collection'");
        var entities = new ArrayList<ServerPlayerEntity>(collection);
        var players = new ArrayList<GamePlayer>(entities.size());
        synchronized (playerMapLock) {
            for (ServerPlayerEntity entity : entities) players.add(getPlayer(entity));
        }
        return players;
    }

    @FunctionalInterface
    private interface PlayerPredicate {
        boolean test(GamePlayer player);
    }

    private static final PlayerPredicate isPlayerNormal =
        (player) -> (!player.isOperator());
    private static final PlayerPredicate isPlayerIn =
        (player) -> (player.isPlaying() && player.isAlive());
    private static final PlayerPredicate isPlayerOut =
        (player) -> (!player.isOperator() && (!player.isPlaying() || !player.isAlive()));

    private List<GamePlayer> getPlayersFiltered(PlayerPredicate predicate) {
        var players = getPlayers();
        var results = new ArrayList<GamePlayer>(players.size());
        for (var player : players) if (predicate.test(player)) results.add(player);
        return results;
    }

    public @NotNull List<@NotNull GamePlayer> getPlayersNormal() {
        return getPlayersFiltered(isPlayerNormal);
    }
    public @NotNull List<@NotNull GamePlayer> getPlayersIn() {
        return getPlayersFiltered(isPlayerIn);
    }
    public @NotNull List<@NotNull GamePlayer> getPlayersOut() {
        return getPlayersFiltered(isPlayerOut);
    }

    private Map<GamePlayer, ServerPlayerEntity> associatePlayersWithEntities(Collection<GamePlayer> players) {
        var results = new ArrayMap<GamePlayer, ServerPlayerEntity>(players.size());
        for (var player : players) {
            var entity = player.getEntity();
            if (entity != null) results.putUnsafe(player, entity);
        }
        return results;
    }

    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntities() {
        return associatePlayersWithEntities(getPlayers());
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntitiesNormal() {
        return associatePlayersWithEntities(getPlayersNormal());
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntitiesIn() {
        return associatePlayersWithEntities(getPlayersIn());
    }
    public @NotNull Map<@NotNull GamePlayer, @NotNull ServerPlayerEntity> getPlayerEntitiesOut() {
        return associatePlayersWithEntities(getPlayersOut());
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
