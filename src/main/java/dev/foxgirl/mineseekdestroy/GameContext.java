package dev.foxgirl.mineseekdestroy;

import com.mojang.serialization.Lifecycle;
import dev.foxgirl.mineseekdestroy.service.*;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableMap;
import kotlin.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GameContext {

    private static final String scoreboardDamageName = "msd_damage";
    private static final Text scoreboardDamageDisplayName = Text.of("-- MNSND: Damage Dealt --");

    // private static final String scoreboardSoulsName = "msd_souls";
    // private static final Text scoreboardSoulsDisplayName = Text.of("souls");

    private static final String scoreboardHeartsName = "msd_hearts";
    private static final Text scoreboardHeartsDisplayName = Text.empty();

    public final @NotNull Game game;

    public final @NotNull MinecraftServer server;
    public final @NotNull ServerWorld world;

    public final @NotNull Scoreboard scoreboard;
    public final @NotNull ScoreboardObjective scoreboardDamage;
    // public final @NotNull ScoreboardObjective scoreboardSouls;
    public final @NotNull ScoreboardObjective scoreboardHearts;

    private final Map<String, Team> teamMap;
    private final Map<String, Team> teamBaseMap;

    public final @NotNull PlayerManager playerManager;

    private final HashMap<UUID, GamePlayer> playerMap;
    private final Object playerMapLock;

    private final GamePlayer playerHerobrine;

    public final @NotNull DamageService damageService;
    public final @NotNull InventoryService inventoryService;
    public final @NotNull LootService lootService;
    public final @NotNull ArmorService armorService;
    public final @NotNull DisguiseService disguiseService;
    public final @NotNull InvisibilityService invisibilityService;
    public final @NotNull BarrierService barrierService;
    public final @NotNull GlowService glowService;
    public final @NotNull ItemService itemService;
    public final @NotNull ShieldService shieldService;
    public final @NotNull SyphonService syphonService;
    public final @NotNull ConduitService conduitService;
    public final @NotNull GhostService ghostService;
    public final @NotNull SoulService soulService;
    public final @NotNull DuelService duelService;
    public final @NotNull SnapshotService snapshotService;
    public final @NotNull StormService stormService;
    public final @NotNull SmokerService smokerService;
    public final @NotNull AutomationService automationService;
    public final @NotNull PagesService pagesService;
    public final @NotNull SummonsService summonsService;
    public final @NotNull TemporalGearService temporalGearService;
    public final @NotNull CountdownService countdownService;
    public final @NotNull SpecialGhoulService specialGhoulService;
    public final @NotNull SpecialCarService specialCarService;
    public final @NotNull SpecialPianoService specialPianoService;
    public final @NotNull SpecialFamilyGuyService specialFamilyGuyService;
    public final @NotNull SpecialBuddyService specialBuddyService;
    public final @NotNull SpecialBoosterService specialBoosterService;
    public final @NotNull SpecialGiftService specialGiftService;

    private final Service[] services;

    private List<GamePlayer> cachePlayers;
    private List<GamePlayer> cachePlayersNormal;
    private List<GamePlayer> cachePlayersIn;
    private List<GamePlayer> cachePlayersOut;
    private List<Pair<GamePlayer, ServerPlayerEntity>> cachePlayerEntities;
    private List<Pair<GamePlayer, ServerPlayerEntity>> cachePlayerEntitiesNormal;
    private List<Pair<GamePlayer, ServerPlayerEntity>> cachePlayerEntitiesIn;
    private List<Pair<GamePlayer, ServerPlayerEntity>> cachePlayerEntitiesOut;

    GameContext(@NotNull Game game) {
        Objects.requireNonNull(game, "Argument 'game'");

        this.game = game;

        game.setState(new WaitingGameState());

        server = Objects.requireNonNull(game.getServer());
        world = Objects.requireNonNull(game.getServer().getOverworld());

        world.setSpawnPos(game.getProperties().getPositionSpawn(), 0.0F);

        scoreboard = server.getScoreboard();

        var scoreboardDamageOld = scoreboard.getNullableObjective(scoreboardDamageName);
        if (scoreboardDamageOld != null) {
            scoreboard.removeObjective(scoreboardDamageOld);
        }
        scoreboardDamage = scoreboard.addObjective(
            scoreboardDamageName,
            ScoreboardCriterion.DUMMY,
            scoreboardDamageDisplayName,
            ScoreboardCriterion.RenderType.INTEGER,
            true, null
        );
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, scoreboardDamage);

        /*
        var scoreboardSoulsOld = scoreboard.getNullableObjective(scoreboardSoulsName);
        if (scoreboardSoulsOld != null) {
            scoreboard.removeObjective(scoreboardSoulsOld);
        }
        scoreboardSouls = scoreboard.addObjective(
            scoreboardSoulsName,
            ScoreboardCriterion.DUMMY,
            scoreboardSoulsDisplayName,
            ScoreboardCriterion.RenderType.INTEGER,
            true, null
        );
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.BELOW_NAME, scoreboardSouls);
        */

        var scoreboardHeartsOld = scoreboard.getNullableObjective(scoreboardHeartsName);
        if (scoreboardHeartsOld != null) {
            scoreboard.removeObjective(scoreboardHeartsOld);
        }
        scoreboardHearts = scoreboard.addObjective(
            scoreboardHeartsName,
            ScoreboardCriterion.HEALTH,
            scoreboardHeartsDisplayName,
            ScoreboardCriterion.RenderType.HEARTS,
            true, null
        );
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.LIST, scoreboardHearts);

        scoreboard.getTeams().removeIf(team -> team.getName().startsWith("msd_"));

        var teamMapBuilder = ImmutableMap.<String, Team>builder(32);
        var teamBaseMapBuilder = ImmutableMap.<String, Team>builder(32);

        for (var value : GameTeam.getEntries()) {
            var team = value.getTeam(scoreboard);
            if (team != null) {
                for (var name : value.getTeamNames()) {
                    teamBaseMapBuilder.put(name, team);
                }
                teamMapBuilder.put(Objects.requireNonNull(value.getTeamName()), team);
            }
            var teamDead = value.getDeadTeam(scoreboard);
            if (teamDead != null) teamMapBuilder.put(teamDead.getName(), teamDead);
            var teamUndead = value.getUndeadTeam(scoreboard);
            if (teamUndead != null) teamMapBuilder.put(teamUndead.getName(), teamUndead);
            var teamDamaged = value.getDamagedTeam(scoreboard);
            if (teamDamaged != null) teamMapBuilder.put(teamDamaged.getName(), teamDamaged);
        }

        teamMap = teamMapBuilder.build();
        teamBaseMap = teamBaseMapBuilder.build();

        playerManager = server.getPlayerManager();

        playerMap = new HashMap<>(64);
        playerMapLock = new Object();

        playerHerobrine = new GamePlayer(this, UUID.fromString("f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2"), "Herobrine");
        playerHerobrine.setKills(666);
        playerHerobrine.setDeaths(-999999);
        playerMap.put(playerHerobrine.getUUID(), playerHerobrine);

        try {
            services = new Service[] {
                damageService = new DamageService(),
                inventoryService = new InventoryService(),
                lootService = new LootService(),
                armorService = new ArmorService(),
                disguiseService = new DisguiseService(),
                invisibilityService = new InvisibilityService(),
                barrierService = new BarrierService(),
                glowService = new GlowService(),
                itemService = new ItemService(),
                shieldService = new ShieldService(),
                syphonService = new SyphonService(),
                conduitService = new ConduitService(),
                ghostService = new GhostService(),
                soulService = new SoulService(),
                duelService = new DuelService(),
                snapshotService = new SnapshotService(),
                stormService = new StormService(),
                smokerService = new SmokerService(),
                automationService = new AutomationService(),
                summonsService = new SummonsService(),
                pagesService = new PagesService(),
                temporalGearService = new TemporalGearService(),
                countdownService = new CountdownService(),
                specialGhoulService = new SpecialGhoulService(),
                specialCarService = new SpecialCarService(),
                specialPianoService = new SpecialPianoService(),
                specialFamilyGuyService = new SpecialFamilyGuyService(),
                specialBuddyService = new SpecialBuddyService(),
                specialBoosterService = new SpecialBoosterService(),
                specialGiftService = new SpecialGiftService(),
            };
        } catch (Throwable cause) {
            Game.LOGGER.error("Failed to instantiate services", cause);
            throw new IllegalStateException("Failed to instantiate services", cause);
        }
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

        var damageTypes = (SimpleRegistry<DamageType>) world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
        var damageTypesThawed = false;
        if (damageTypes.getEntry(Game.DAMAGE_TYPE_ABYSS).isEmpty()) {
            damageTypesThawed = true;
            damageTypes.frozen = false;
            damageTypes.add(Game.DAMAGE_TYPE_ABYSS, new DamageType("abyss", 0.0F), Lifecycle.experimental());
        }
        if (damageTypes.getEntry(Game.DAMAGE_TYPE_HEARTBREAK).isEmpty()) {
            damageTypesThawed = true;
            damageTypes.frozen = false;
            damageTypes.add(Game.DAMAGE_TYPE_HEARTBREAK, new DamageType("heartbreak", 0.0F), Lifecycle.experimental());
        }
        if (damageTypes.getEntry(Game.DAMAGE_TYPE_BITTEN).isEmpty()) {
            damageTypesThawed = true;
            damageTypes.frozen = false;
            damageTypes.add(Game.DAMAGE_TYPE_BITTEN, new DamageType("bitten", 0.0F), Lifecycle.experimental());
        }
        if (damageTypesThawed) damageTypes.freeze();

        game.setRuleInt(GameRules.RANDOM_TICK_SPEED, GameRules.DEFAULT_RANDOM_TICK_SPEED);
        game.setRuleInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 200);
        game.setRuleBoolean(GameRules.KEEP_INVENTORY, true);
        game.setRuleBoolean(GameRules.DO_FIRE_TICK, true);
        game.setRuleBoolean(GameRules.DO_MOB_GRIEFING, true);
        game.setRuleBoolean(GameRules.DO_DAYLIGHT_CYCLE, true);

        game.setRuleBoolean(Game.RULE_AUTOMATION_ENABLED, true);
        game.setRuleBoolean(Game.RULE_SUMMONS_ENABLED, true);
        game.setRuleBoolean(Game.RULE_GHOULS_ENABLED, false);
        game.setRuleBoolean(Game.RULE_BUDDY_ENABLED, false);
        game.setRuleBoolean(Game.RULE_CHAOS_ENABLED, false);
        game.setRuleBoolean(Game.RULE_COUNTDOWN_ENABLED, true);
        game.setRuleBoolean(Game.RULE_COUNTDOWN_AUTOSTART_ENABLED, true);
        game.setRuleBoolean(Game.RULE_HIDDEN_ARMOR_ENABLED, false);
        game.setRuleDouble(Game.RULE_BORDER_CLOSE_DURATION, 180.0);
        game.setRuleBoolean(Game.RULE_KILLZONE_BOUNDS_ENABLED, true);
        game.setRuleBoolean(Game.RULE_KILLZONE_BLIMP_ENABLED, true);
        game.setRuleBoolean(Game.RULE_SKIRMISH_ENABLED, false);

        game.getProperties().setup(this);

        for (var service : services) {
            try {
                service.initialize(this);
            } catch (Throwable cause) {
                String message = "Service " + service.getClass().getSimpleName() + " failed to initialize";
                Game.LOGGER.error(message, cause);
                throw new IllegalStateException(message, cause);
            }
        }
    }

    public void destroy() {
        scoreboard.removeObjective(scoreboardDamage);
        // scoreboard.removeObjective(scoreboardSouls);
        scoreboard.removeObjective(scoreboardHearts);

        game.setRuleBoolean(GameRules.DO_FIRE_TICK, false);
        game.setRuleBoolean(GameRules.DO_MOB_GRIEFING, false);
        game.setRuleBoolean(Game.RULE_KILLZONE_BOUNDS_ENABLED, false);
        game.setRuleBoolean(Game.RULE_KILLZONE_BLIMP_ENABLED, false);

        for (var team : teamMap.values()) {
            scoreboard.removeTeam(team);
        }
    }

    public void update() {
        for (var service : services) {
            try {
                service.update();
            } catch (Throwable cause) {
                String message = "Service " + service.getClass().getSimpleName() + " failed to update";
                Game.LOGGER.error(message, cause);
                throw new IllegalStateException(message, cause);
            }
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

    private List<GamePlayer> filterPlayers(List<GamePlayer> players, PlayerPredicate predicate) {
        var builder = ImmutableList.<GamePlayer>builder(players.size());
        for (var player : players) {
            if (predicate.test(player)) {
                builder.add(player);
            }
        }
        return builder.build();
    }
    private List<Pair<GamePlayer, ServerPlayerEntity>> filterPlayerEntities(List<Pair<GamePlayer, ServerPlayerEntity>> playerEntities, PlayerPredicate predicate) {
        var builder = ImmutableList.<Pair<GamePlayer, ServerPlayerEntity>>builder(playerEntities.size());
        for (var pair : playerEntities) {
            if (predicate.test(pair.getFirst())) {
                builder.put(pair);
            }
        }
        return builder.build();
    }

    public void syncPlayers() {
        var entities = playerManager.getPlayerList().toArray();

        ImmutableList.Builder<Pair<GamePlayer, ServerPlayerEntity>> playerEntitiesBuilder = ImmutableList.builder(entities.length);
        ImmutableList<Pair<GamePlayer, ServerPlayerEntity>> playerEntities;

        ImmutableList<GamePlayer> players;

        synchronized (playerMapLock) {
            for (int i = 0, length = entities.length; i < length; i++) {
                var playerEntity = (ServerPlayerEntity) entities[i];
                var player = getPlayer(playerEntity);
                playerEntitiesBuilder.add(new Pair<>(player, playerEntity));
            }
            players = ImmutableList.copyOf(playerMap.values());
        }

        playerEntities = playerEntitiesBuilder.build();

        cachePlayers = players;
        cachePlayersNormal = filterPlayers(players, isPlayerNormal);
        cachePlayersIn = filterPlayers(players, isPlayerIn);
        cachePlayersOut = filterPlayers(players, isPlayerOut);
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
    public @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntities() {
        return Objects.requireNonNull(cachePlayerEntities, "Expression 'cachePlayerEntities'");
    }
    public @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntitiesNormal() {
        return Objects.requireNonNull(cachePlayerEntitiesNormal, "Expression 'cachePlayerEntitiesNormal'");
    }
    public @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntitiesIn() {
        return Objects.requireNonNull(cachePlayerEntitiesIn, "Expression 'cachePlayerEntitiesIn'");
    }
    public @NotNull List<@NotNull Pair<@NotNull GamePlayer, @NotNull ServerPlayerEntity>> getPlayerEntitiesOut() {
        return Objects.requireNonNull(cachePlayerEntitiesOut, "Expression 'cachePlayerEntitiesOut'");
    }

    public @Nullable GamePlayer getPlayer(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "Argument 'uuid'");
        if (uuid.equals(Util.NIL_UUID)) return null;
        synchronized (playerMapLock) {
            return playerMap.get(uuid);
        }
    }

    public @Nullable GamePlayer getPlayer(@NotNull String name) {
        Objects.requireNonNull(name, "Argument 'name'");
        var nameLowercase = name.toLowerCase(Locale.ROOT);
        synchronized (playerMapLock) {
            for (var player : playerMap.values()) {
                if (Objects.equals(player.getNameLowercase(), nameLowercase)) {
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

    public @NotNull GamePlayer getPlayer(@NotNull NbtCompound nbt) {
        Objects.requireNonNull(nbt, "Argument 'nbt'");
        var wrapperNew = new GamePlayer(this, nbt);
        synchronized (playerMapLock) {
            var wrapperOld = playerMap.get(wrapperNew.getUUID());
            if (wrapperOld != null) {
                return wrapperOld;
            }
            playerMap.put(wrapperNew.getUUID(), wrapperNew);
            return wrapperNew;
        }
    }

    public @NotNull GamePlayer getPlayerHerobrine() {
        return playerHerobrine;
    }

    public @Nullable GameTeam getGameTeam(@NotNull Team team) {
        Objects.requireNonNull(team, "Argument 'team'");
        return getGameTeam(team.getName());
    }
    public @Nullable GameTeam getGameTeam(@NotNull String name) {
        Objects.requireNonNull(name, "Argument 'name'");
        for (GameTeam team : GameTeam.getEntries()) {
            if (team.getTeamNames().contains(name)) {
                return team;
            }
        }
        return null;
    }

    public @Nullable Team getTeam(@NotNull GameTeam team) {
        Objects.requireNonNull(team, "Argument 'team'");
        var name = team.getTeamName();
        return name == null ? null : getTeam(name);
    }
    public @Nullable Team getTeam(@NotNull String name) {
        Objects.requireNonNull(name, "Argument 'name'");
        return teamMap.get(name);
    }

    public @Nullable Team getBaseTeam(@NotNull Team team) {
        Objects.requireNonNull(team, "Argument 'team'");
        return getBaseTeam(team.getName());
    }
    public @Nullable Team getBaseTeam(@NotNull String name) {
        Objects.requireNonNull(name, "Argument 'name'");
        return teamBaseMap.get(name);
    }

}
