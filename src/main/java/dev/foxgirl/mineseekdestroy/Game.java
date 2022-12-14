package dev.foxgirl.mineseekdestroy;

import com.google.common.collect.ImmutableSet;
import dev.foxgirl.mineseekdestroy.command.Command;
import dev.foxgirl.mineseekdestroy.state.GameState;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import dev.foxgirl.mineseekdestroy.util.Console;
import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Game implements Console, DedicatedServerModInitializer, ServerLifecycleEvents.ServerStarting, ServerTickEvents.StartTick {

    public static final @NotNull Logger LOGGER = LogManager.getLogger("MnSnD");

    public static final @NotNull GameRules.Key<DoubleRule> RULE_PREPARING_DURATION =
        GameRuleRegistry.register("msdStartingPreparingSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(3.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_STARTING_DURATION =
        GameRuleRegistry.register("msdStartingCountdownSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(20.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_STARTING_EFFECT_DURATION =
        GameRuleRegistry.register("msdStartingEffectDurationSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(20.0));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_STARTING_PING_VOLUME =
        GameRuleRegistry.register("msdStartingPingVolume", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(0.2));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_STARTING_PING_PITCH =
        GameRuleRegistry.register("msdStartingPingPitch", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(0.45));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_FINALIZING_DURATION =
        GameRuleRegistry.register("msdFinalizingSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(5.0));

    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_LOOT_COUNT =
        GameRuleRegistry.register("msdLootCount", GameRules.Category.MISC, GameRuleFactory.createIntRule(4));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_KNOCKBACK_SNOWBALL =
        GameRuleRegistry.register("msdKnockbackSnowball", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(4.0, -Double.MAX_VALUE, Double.MAX_VALUE));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_KNOCKBACK_EGG =
        GameRuleRegistry.register("msdKnockbackEgg", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(-4.0, -Double.MAX_VALUE, Double.MAX_VALUE));

    public static final @NotNull Set<@NotNull UUID> OPERATORS = ImmutableSet.copyOf(new UUID[] {
        UUID.fromString("ea5f3df6-eba5-47b6-a7f8-fbfec4078069"), // bread_enu
        UUID.fromString("84cc25f6-1689-4729-a3fa-43a79e428404"), // luavixen
        UUID.fromString("ae60cf7c-6ba0-4cf6-884e-23decd3e0ab6"), // ry755
        UUID.fromString("01dc40cd-2dba-4063-b2c5-bc333e864e0c"), // Karma_Dragon
    });

    public static final @NotNull Set<@NotNull Block> PLACABLE_BLOCKS = ImmutableSet.copyOf(new Block[] {
        Blocks.WHITE_CONCRETE_POWDER,
        Blocks.ORANGE_CONCRETE_POWDER,
        Blocks.MAGENTA_CONCRETE_POWDER,
        Blocks.LIGHT_BLUE_CONCRETE_POWDER,
        Blocks.YELLOW_CONCRETE_POWDER,
        Blocks.LIME_CONCRETE_POWDER,
        Blocks.PINK_CONCRETE_POWDER,
        Blocks.GRAY_CONCRETE_POWDER,
        Blocks.LIGHT_GRAY_CONCRETE_POWDER,
        Blocks.CYAN_CONCRETE_POWDER,
        Blocks.PURPLE_CONCRETE_POWDER,
        Blocks.BLUE_CONCRETE_POWDER,
        Blocks.BROWN_CONCRETE_POWDER,
        Blocks.GREEN_CONCRETE_POWDER,
        Blocks.RED_CONCRETE_POWDER,
        Blocks.BLACK_CONCRETE_POWDER,
        Blocks.SNOW_BLOCK,
        Blocks.BONE_BLOCK,
    });

    public static final @NotNull Set<@NotNull Block> INTERACTABLE_BLOCKS = ImmutableSet.copyOf(new Block[] {
        Blocks.CHEST,
        Blocks.BARREL,
        Blocks.ACACIA_DOOR,
        Blocks.BIRCH_DOOR,
        Blocks.DARK_OAK_DOOR,
        Blocks.JUNGLE_DOOR,
        Blocks.MANGROVE_DOOR,
        Blocks.OAK_DOOR,
        Blocks.SPRUCE_DOOR,
        Blocks.WARPED_DOOR,
        Blocks.MANGROVE_FENCE_GATE,
        Blocks.BIRCH_TRAPDOOR,
        Blocks.SHULKER_BOX,
        Blocks.WHITE_SHULKER_BOX,
        Blocks.ORANGE_SHULKER_BOX,
        Blocks.MAGENTA_SHULKER_BOX,
        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.YELLOW_SHULKER_BOX,
        Blocks.LIME_SHULKER_BOX,
        Blocks.PINK_SHULKER_BOX,
        Blocks.GRAY_SHULKER_BOX,
        Blocks.LIGHT_GRAY_SHULKER_BOX,
        Blocks.CYAN_SHULKER_BOX,
        Blocks.PURPLE_SHULKER_BOX,
        Blocks.BLUE_SHULKER_BOX,
        Blocks.BROWN_SHULKER_BOX,
        Blocks.GREEN_SHULKER_BOX,
        Blocks.RED_SHULKER_BOX,
        Blocks.BLACK_SHULKER_BOX,
    });

    public static final @NotNull Set<@NotNull Item> USABLE_ITEMS = ImmutableSet.copyOf(new Item[] {
        Items.SHIELD,
        Items.BOW,
        Items.CROSSBOW,
        Items.FISHING_ROD,
        Items.FIREWORK_ROCKET,
        Items.EGG,
        Items.SNOWBALL,
        Items.POTION,
        Items.SPLASH_POTION,
        Items.LINGERING_POTION,
        Items.MILK_BUCKET,
        Items.POTATO,
        Items.BREAD,
    });

    public static final @NotNull Set<@NotNull Item> ILLEGAL_ITEMS = ImmutableSet.copyOf(new Item[] {
        Items.BEDROCK,
        Items.COMMAND_BLOCK,
        Items.CHAIN_COMMAND_BLOCK,
        Items.REPEATING_COMMAND_BLOCK,
        Items.COMMAND_BLOCK_MINECART,
        Items.STRUCTURE_BLOCK,
        Items.STRUCTURE_VOID,
        Items.BARRIER,
        Items.LIGHT,
        Items.SPAWNER,
        Items.END_PORTAL_FRAME,
    });

    public static final @NotNull Console CONSOLE_PLAYERS = new Console() {
        @Override
        public void sendInfo(@Nullable Object... values) {
            var message = Console.format(values, false);
            LOGGER.info(message.getString());
            Game.getGame().sendToPlayers(message);

        }
        @Override
        public void sendError(@Nullable Object... values) {
            var message = Console.format(values, true);
            LOGGER.error(message.getString());
            Game.getGame().sendToPlayers(message);
        }
    };
    public static final @NotNull Console CONSOLE_OPERATORS = new Console() {
        @Override
        public void sendInfo(@Nullable Object... values) {
            var message = Console.format(values, false);
            LOGGER.info(message.getString());
            Game.getGame().sendToOperators(message);

        }
        @Override
        public void sendError(@Nullable Object... values) {
            var message = Console.format(values, true);
            LOGGER.error(message.getString());
            Game.getGame().sendToOperators(message);
        }
    };

    private static Game INSTANCE;

    public Game() {
        if (INSTANCE == null) {
            INSTANCE = this;
        } else if (INSTANCE != this) {
            throw new IllegalStateException("Attempted to create multiple Game instances");
        }
    }

    public static @NotNull Game getGame() {
        return Game.INSTANCE;
    }

    public static @NotNull GameProperties getGameProperties() {
        return getGame().getProperties();
    }

    private MinecraftServer server;

    private GameProperties properties = GameProperties.Macander.INSTANCE;

    private GameState state = new WaitingGameState();
    private GameContext context = null;

    public @NotNull MinecraftServer getServer() {
        return server;
    }

    public @NotNull GameProperties getProperties() {
        return properties;
    }

    public void setProperties(@NotNull GameProperties properties) {
        Objects.requireNonNull(properties, "Argument 'properties'");
        if (this.properties != properties) {
            this.properties = properties;
            LOGGER.info("Game properties changed to " + properties.getClass().getSimpleName());
        }
    }

    public @NotNull GameState getState() {
        return state;
    }

    public void setState(@NotNull GameState state) {
        Objects.requireNonNull(state, "Argument 'state'");
        if (this.state != state) {
            this.state = state;
            LOGGER.info("Game state changed to " + state.getClass().getSimpleName());
        }
    }

    public @Nullable GameContext getContext() {
        return context;
    }

    public <T extends GameRules.Rule<T>> @NotNull T getRule(GameRules.Key<T> key) {
        Objects.requireNonNull(key, "Argument 'key'");

        var context = getContext();
        var world = context != null ? context.world : getServer().getOverworld();
        if (world == null) {
            throw new IllegalStateException("Game rules unavailable, world not loaded");
        }

        var rule = world.getGameRules().get(key);
        if (rule == null) {
            throw new NoSuchElementException("Game rule for key '" + key + "' not found");
        }

        return rule;
    }

    public double getRuleDouble(GameRules.Key<DoubleRule> key) {
        return getRule(key).get();
    }
    public int getRuleInt(GameRules.Key<GameRules.IntRule> key) {
        return getRule(key).get();
    }
    public boolean getRuleBoolean(GameRules.Key<GameRules.BooleanRule> key) {
        return getRule(key).get();
    }

    public boolean isOperator(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "Argument 'entity'");

        var context = getContext();
        if (context != null) {
            var player = context.getPlayer(entity);
            if (player != null) {
                return player.isOperator();
            }
        }

        return hasOperator(entity);
    }

    public boolean hasOperator(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "Argument 'entity'");

        if (entity instanceof PlayerEntity playerEntity) {
            var playerManager = getServer().getPlayerManager();
            if (playerManager.isOperator(playerEntity.getGameProfile())) {
                return true;
            }
        }

        var uuid = Objects.requireNonNull(entity.getUuid(), "Expression 'entity.getUuid()'");

        if (OPERATORS.contains(uuid)) {
            return true;
        }

        var context = getContext();
        if (context != null) {
            var player = context.getPlayer(uuid);
            return player != null && player.isOperator();
        }

        return false;
    }

    public void sendToPlayers(@NotNull Text message) {
        Objects.requireNonNull(message, "Argument 'message'");
        for (var player : getServer().getPlayerManager().getPlayerList()) {
            player.sendMessageToClient(message, false);
        }
    }

    public void sendToOperators(@NotNull Text message) {
        Objects.requireNonNull(message, "Argument 'message'");
        for (var player : getServer().getPlayerManager().getPlayerList()) {
            if (isOperator(player)) player.sendMessageToClient(message, false);
        }
    }

    @Override
    public void sendInfo(@Nullable Object... values) {
        CONSOLE_PLAYERS.sendInfo(values);
    }

    @Override
    public void sendError(@Nullable Object... values) {
        CONSOLE_OPERATORS.sendError(values);
    }

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(this);
        ServerTickEvents.START_SERVER_TICK.register(this);

        CommandRegistrationCallback.EVENT.register(Command.INSTANCE);

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayerEntity, newPlayerEntity, alive) -> getState().onRespawn(getContext(), oldPlayerEntity, newPlayerEntity, alive));
        ServerPlayerEvents.ALLOW_DEATH.register((playerEntity, damageSource, damageAmount) -> getState().allowDeath(getContext(), playerEntity, damageSource, damageAmount));
        ExtraEvents.PLAYER_DAMAGED.register((playerEntity, damageSource, damageAmount) -> getState().onTakeDamage(getContext(), playerEntity, damageSource, damageAmount));
        PlayerBlockBreakEvents.BEFORE.register((world, playerEntity, pos, blockState, blockEntity) -> getState().allowBlockBreak(getContext(), playerEntity, world, pos, blockState, blockEntity));
        ExtraEvents.BLOCK_USED.register((player, world, hand, blockHit, blockState) -> getState().onUseBlock(getContext(), player, world, hand, blockHit, blockState));
        ExtraEvents.BLOCK_USED_WITH.register((player, world, hand, blockHit, blockItemStack) -> getState().onUseBlockWith(getContext(), player, world, hand, blockHit, blockItemStack));
        ExtraEvents.ITEM_USED.register((player, world, hand, stack) -> getState().onUseItem(getContext(), player, world, hand, stack));
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> getState().onUseEntity(getContext(), playerEntity, world, hand, entity, hitResult));
        AttackBlockCallback.EVENT.register((playerEntity, world, hand, pos, direction) -> getState().onAttackBlock(getContext(), playerEntity, world, hand, pos, direction));
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> getState().onAttackEntity(getContext(), playerEntity, world, hand, entity, hitResult));
        ExtraEvents.ITEM_DROPPED.register((player, stack, throwRandomly, retainOwnership) -> getState().onItemDropped(getContext(), player, stack, throwRandomly, retainOwnership));
        ExtraEvents.ITEM_ACQUIRED.register((player, inventory, stack, slot) -> getState().onItemAcquired(getContext(), player, inventory, stack, slot));
    }

    @Override
    public void onServerStarting(MinecraftServer server) {
        Objects.requireNonNull(server, "Argument 'server'");
        this.server = server;
    }

    @Override
    public void onStartTick(MinecraftServer server) {
        var world = server.getOverworld();
        var players = server.getPlayerManager().getPlayerList();
        for (var player : players) {
            if ((player.getWorld() != world || !properties.getRegionLegal().contains(player)) && !hasOperator(player)) {
                player.teleport(
                    world,
                    properties.getPositionArena().getX(),
                    properties.getPositionArena().getY(),
                    properties.getPositionArena().getZ(),
                    player.getYaw(),
                    player.getPitch()
                );
            }
            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL && !hasOperator(player)) {
                player.interactionManager.changeGameMode(GameMode.SURVIVAL);
                player.kill();
            }
        }

        var context = getContext();
        if (context == null) return;

        context.syncPlayers();

        var state = getState().update(context);
        if (state != null) setState(state);

        context.inventoryService.handleUpdate();
        context.armorService.handleUpdate();
        context.invisibilityService.handleUpdate();
        context.saturationService.handleUpdate();
        context.glowService.handleUpdate();
        context.powderService.handleUpdate();

        context.updatePlayers();
    }

    public @NotNull GameContext initialize() {
        if (context == null) {
            context = new GameContext(this);
            context.initialize();
        }
        return context;
    }

    public void destroy() {
        context.destroy();
        context = null;
        setState(new WaitingGameState());
    }

}
