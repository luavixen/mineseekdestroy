package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.command.Command;
import dev.foxgirl.mineseekdestroy.event.*;
import dev.foxgirl.mineseekdestroy.state.GameState;
import dev.foxgirl.mineseekdestroy.state.PlayingGameState;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import dev.foxgirl.mineseekdestroy.util.Console;
import dev.foxgirl.mineseekdestroy.util.Editor;
import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import dev.foxgirl.mineseekdestroy.util.Scheduler;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableSet;
import kotlinx.serialization.Serializable;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtByte;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

@Serializable(with = GameSerializer.class)
public final class Game implements Console, DedicatedServerModInitializer, ServerLifecycleEvents.ServerStarting, ServerTickEvents.StartTick {

    public static final @NotNull Logger LOGGER = LogManager.getLogger("MnSnD");

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_MESSAGE_DIRECT_ALLOWED =
        GameRuleRegistry.register("msdMessageDirectAllowed", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_MESSAGE_DIRECT_BROADCAST =
        GameRuleRegistry.register("msdMessageDirectBroadcast", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_MESSAGE_TEAM_ALLOWED =
        GameRuleRegistry.register("msdMessageTeamAllowed", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_MESSAGE_TEAM_BROADCAST =
        GameRuleRegistry.register("msdMessageTeamBroadcast", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_AUTOMATION_ENABLED =
        GameRuleRegistry.register("msdAutomationEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_AUTOMATION_GHOSTS_ENABLED =
        GameRuleRegistry.register("msdAutomationGhostsEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_AUTOMATION_DELAY_DURATION =
        GameRuleRegistry.register("msdAutomationDelayDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(5.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_AUTOMATION_INTERVAL_DURATION =
        GameRuleRegistry.register("msdAutomationIntervalDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(1.0));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_PREPARING_DURATION =
        GameRuleRegistry.register("msdStartingPreparingSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(3.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_STARTING_DURATION =
        GameRuleRegistry.register("msdStartingCountdownSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(20.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_STARTING_EFFECT_DURATION =
        GameRuleRegistry.register("msdStartingEffectDurationSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(20.0));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_PING_VOLUME =
        GameRuleRegistry.register("msdPingVolume", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(0.2));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_PING_PITCH =
        GameRuleRegistry.register("msdPingPitch", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(0.45));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_FINALIZING_DURATION =
        GameRuleRegistry.register("msdFinalizingSeconds", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(10.0));

    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_LOOT_COUNT =
        GameRuleRegistry.register("msdLootCount", GameRules.Category.MISC, GameRuleFactory.createIntRule(4));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_KILLZONE_BOUNDS_ENABLED =
        GameRuleRegistry.register("msdKillzoneBoundsEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_KILLZONE_BLIMP_ENABLED =
        GameRuleRegistry.register("msdKillzoneBlimpEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_KNOCKBACK_SNOWBALL =
        GameRuleRegistry.register("msdKnockbackSnowball", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(4.0, -Double.MAX_VALUE, Double.MAX_VALUE));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_KNOCKBACK_EGG =
        GameRuleRegistry.register("msdKnockbackEgg", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(-4.0, -Double.MAX_VALUE, Double.MAX_VALUE));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_BORDER_CLOSE_DURATION =
        GameRuleRegistry.register("msdBorderCloseDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(180.0));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_TOWER_EFFECT_DURATION =
        GameRuleRegistry.register("msdTowerEffectDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(20.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_TOWER_KNOCKBACK =
        GameRuleRegistry.register("msdTowerKnockback", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(4.0));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_GHOULS_ENABLED =
        GameRuleRegistry.register("msdGhoulsEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_GHOULS_SPAWN_DELAY_MIN =
        GameRuleRegistry.register("msdGhoulsSpawnDelayMin", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(15.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_GHOULS_SPAWN_DELAY_MAX =
        GameRuleRegistry.register("msdGhoulsSpawnDelayMax", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(30.0));

    public static final @NotNull GameRules.Key<DoubleRule> RULE_CARS_COOLDOWN_DURATION =
        GameRuleRegistry.register("msdCarsCooldownDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(15.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_CARS_BOOST_DURATION =
        GameRuleRegistry.register("msdCarsBoostDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(3.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_CARS_KNOCKBACK =
        GameRuleRegistry.register("msdCarsKnockback", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(3.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_CARS_DAMAGE =
        GameRuleRegistry.register("msdCarsDamage", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(4.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_CARS_SPEED =
        GameRuleRegistry.register("msdCarsSpeed", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(1.2));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SUMMONS_ENABLED =
        GameRuleRegistry.register("msdSummonsEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final @NotNull Set<@NotNull UUID> OPERATORS = ImmutableSet.copyOf(new UUID[] {
        UUID.fromString("84cc25f6-1689-4729-a3fa-43a79e428404"), // luavixen
        UUID.fromString("ea5f3df6-eba5-47b6-a7f8-fbfec4078069"), // bread_enu
        UUID.fromString("53489b5d-d23a-4758-9374-2d8151fba31a"), // ReachOutLaz
        UUID.fromString("01dc40cd-2dba-4063-b2c5-bc333e864e0c"), // Karma_Dragon
    });

    public static final @NotNull Set<@NotNull Block> PLACABLE_BLOCKS = ImmutableSet.copyOf(new Block[] {
        Blocks.WHITE_CONCRETE_POWDER, Blocks.WHITE_CONCRETE,
        Blocks.ORANGE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE,
        Blocks.MAGENTA_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE,
        Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE,
        Blocks.YELLOW_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE,
        Blocks.LIME_CONCRETE_POWDER, Blocks.LIME_CONCRETE,
        Blocks.PINK_CONCRETE_POWDER, Blocks.PINK_CONCRETE,
        Blocks.GRAY_CONCRETE_POWDER, Blocks.GRAY_CONCRETE,
        Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE,
        Blocks.CYAN_CONCRETE_POWDER, Blocks.CYAN_CONCRETE,
        Blocks.PURPLE_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE,
        Blocks.BLUE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE,
        Blocks.BROWN_CONCRETE_POWDER, Blocks.BROWN_CONCRETE,
        Blocks.GREEN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE,
        Blocks.RED_CONCRETE_POWDER, Blocks.RED_CONCRETE,
        Blocks.BLACK_CONCRETE_POWDER, Blocks.BLACK_CONCRETE,
        Blocks.WHITE_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
        Blocks.TARGET, Blocks.TNT, Blocks.LADDER,
        Blocks.BLUE_ICE,
        Blocks.SNOW_BLOCK,
        Blocks.BONE_BLOCK,
        Blocks.SLIME_BLOCK,
        Blocks.CHIPPED_ANVIL,
        Blocks.DAMAGED_ANVIL,
        Blocks.FIRE,
    });

    public static final @NotNull Set<@NotNull Block> UNSTEALABLE_BLOCKS = ImmutableSet.copyOf(new Block[] {
        Blocks.MAGENTA_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE, Blocks.SLIME_BLOCK,
        Blocks.AIR, Blocks.CAVE_AIR, Blocks.FIRE,
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
        Blocks.SMOKER, Blocks.FLETCHING_TABLE,
        Blocks.TARGET, Blocks.TNT, Blocks.LADDER,
        Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
        Blocks.GRAVEL, Blocks.STONE, Blocks.DIRT, Blocks.DIRT_PATH,
        Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.FARMLAND,
        Blocks.ACACIA_TRAPDOOR,
        Blocks.BEDROCK,
        Blocks.BLACKSTONE_WALL,
        Blocks.CHISELED_NETHER_BRICKS,
        Blocks.CRYING_OBSIDIAN,
        Blocks.DARK_OAK_FENCE,
        Blocks.DARK_OAK_SLAB,
        Blocks.DARK_OAK_STAIRS,
        Blocks.DARK_OAK_TRAPDOOR,
        Blocks.GOLD_BLOCK,
        Blocks.IRON_BARS,
        Blocks.LIME_STAINED_GLASS,
        Blocks.LOOM,
        Blocks.NETHER_BRICKS,
        Blocks.NETHER_BRICK_FENCE,
        Blocks.NETHER_BRICK_WALL,
        Blocks.OBSIDIAN,
        Blocks.PRISMARINE_STAIRS,
        Blocks.PRISMARINE_WALL,
        Blocks.RAW_GOLD_BLOCK,
        Blocks.REDSTONE_BLOCK,
        Blocks.REDSTONE_LAMP,
        Blocks.WARPED_FENCE,
        Blocks.WARPED_PLANKS,
        Blocks.WARPED_STAIRS,
        Blocks.WAXED_CUT_COPPER,
        Blocks.WAXED_CUT_COPPER_STAIRS,
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
        Items.TRIDENT,
        Items.FISHING_ROD,
        Items.CARROT_ON_A_STICK,
        Items.FIREWORK_ROCKET,
        Items.FLINT_AND_STEEL,
        Items.ENDER_PEARL,
        Items.EGG,
        Items.SNOWBALL,
        Items.POTION,
        Items.SPLASH_POTION,
        Items.LINGERING_POTION,
        Items.MILK_BUCKET,
        Items.WATER_BUCKET,
        Items.POTATO,
        Items.BAKED_POTATO,
        Items.POISONOUS_POTATO,
        Items.BREAD,
        Items.BEEF,
        Items.COOKED_BEEF,
        Items.PORKCHOP,
        Items.COOKED_PORKCHOP,
        Items.CHICKEN,
        Items.COOKED_CHICKEN,
        Items.BOOK,
        Items.WRITTEN_BOOK,
        Items.WRITABLE_BOOK,
        Items.KNOWLEDGE_BOOK,
        Items.ENCHANTED_BOOK,
    });

    public static final @NotNull Set<@NotNull Item> DROPPED_ITEMS = ImmutableSet.copyOf(new Item[] {
        Items.SHIELD,
        Items.FIREWORK_ROCKET,
        Items.CARROT_ON_A_STICK,
        Items.TARGET,
    });

    public static final @NotNull Set<@NotNull Item> UNDROPPABLE_ITEMS = ImmutableSet.copyOf(new Item[] {
        Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
        Items.BOW, Items.CROSSBOW,
        Items.TRIDENT,
        Items.ELYTRA,
        Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
        Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
        Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
        Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
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
        Items.PLAYER_HEAD,
        Items.YELLOW_STAINED_GLASS_PANE,
        Items.BLUE_STAINED_GLASS_PANE,
        Items.LIME_STAINED_GLASS_PANE,
        Items.RED_CONCRETE,
        Items.LIME_CONCRETE,
    });

    public static final @NotNull ItemStack stackEggBlock = new ItemStack(Items.BONE_BLOCK);
    static {
        stackEggBlock.getOrCreateNbt().put("MsdFancyEggBlock", NbtByte.ONE);
        stackEggBlock.setCustomName(Text.literal("Egg Block").setStyle(Style.EMPTY.withItalic(false).withFormatting(Formatting.GREEN)));
    }
    public static final @NotNull ItemStack stackEctoplasm = new ItemStack(Items.SLIME_BLOCK);
    static {
        stackEctoplasm.getOrCreateNbt().put("MsdFancyEctoplasm", NbtByte.ONE);
        stackEctoplasm.setCustomName(Text.literal("Ectogasm").setStyle(Style.EMPTY.withItalic(false).withFormatting(Formatting.GREEN)));
    }

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

    /**
     * Gets the singleton instance of {@link Game}.
     * @throws NullPointerException If the mod has not yet been initialized.
     */
    public static @NotNull Game getGame() {
        return Objects.requireNonNull(Game.INSTANCE, "Expression 'Game.INSTANCE'");
    }

    /**
     * Gets the game's properties list.
     * @throws NullPointerException If the mod has not yet been initialized.
     */
    public static @NotNull GameProperties getGameProperties() {
        return getGame().getProperties();
    }

    private MinecraftServer server;

    private GameProperties properties = GameProperties.Macander.INSTANCE;

    private GameState state = new WaitingGameState();
    private GameContext context = null;

    /**
     * Gets the current {@link MinecraftServer} instance.
     * @throws NullPointerException If the server is not ready yet.
     */
    public @NotNull MinecraftServer getServer() {
        return Objects.requireNonNull(server, "Expression 'server'");
    }

    public @NotNull GameProperties getProperties() {
        return Objects.requireNonNull(properties, "Expression 'properties'");
    }

    public @NotNull GameState getState() {
        return Objects.requireNonNull(state, "Expression 'state'");
    }

    public void setState(@NotNull GameState newState) {
        Objects.requireNonNull(newState, "Argument 'newState'");
        var oldState = state;
        if (oldState != newState) {
            state = newState;
            Bus.publish(new StateChangeEvent(oldState, newState));
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

    public void setRuleDouble(GameRules.Key<DoubleRule> key, double value) {
        getRule(key).setValue(GameRuleFactory.createDoubleRule(value).createRule(), getServer());
    }
    public void setRuleInt(GameRules.Key<GameRules.IntRule> key, int value) {
        getRule(key).set(value, getServer());
    }
    public void setRuleBoolean(GameRules.Key<GameRules.BooleanRule> key, boolean value) {
        getRule(key).set(value, getServer());
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
        for (var playerEntity : getServer().getPlayerManager().getPlayerList()) {
            playerEntity.sendMessageToClient(message, false);
        }
    }

    public void sendToOperators(@NotNull Text message) {
        Objects.requireNonNull(message, "Argument 'message'");
        for (var playerEntity : getServer().getPlayerManager().getPlayerList()) {
            if (isOperator(playerEntity)) playerEntity.sendMessageToClient(message, false);
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
        ExtraEvents.BLOCK_USED.register((playerEntity, world, hand, blockHit, blockState) -> getState().onUseBlock(getContext(), playerEntity, world, hand, blockHit, blockState));
        ExtraEvents.BLOCK_USED_WITH.register((playerEntity, world, hand, blockHit, blockItemStack) -> getState().onUseBlockWith(getContext(), playerEntity, world, hand, blockHit, blockItemStack));
        ExtraEvents.ITEM_USED.register((playerEntity, world, hand, stack) -> getState().onUseItem(getContext(), playerEntity, world, hand, stack));
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> getState().onUseEntity(getContext(), playerEntity, world, hand, entity, hitResult));
        AttackBlockCallback.EVENT.register((playerEntity, world, hand, pos, direction) -> getState().onAttackBlock(getContext(), playerEntity, world, hand, pos, direction));
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> getState().onAttackEntity(getContext(), playerEntity, world, hand, entity, hitResult));
        ExtraEvents.ITEM_DROPPED.register((playerEntity, stack, throwRandomly, retainOwnership) -> getState().onItemDropped(getContext(), playerEntity, stack, throwRandomly, retainOwnership));
        ExtraEvents.ITEM_ACQUIRED.register((playerEntity, inventory, stack, slot) -> getState().onItemAcquired(getContext(), playerEntity, inventory, stack, slot));

        ServerKt.start();
    }

    @Override
    public void onServerStarting(MinecraftServer server) {
        Objects.requireNonNull(server, "Argument 'server'");
        this.server = server;

        Scheduler.interval(0.5, (schedule) -> Bus.publish(new UpdateEvent(this)));
    }

    @Override
    public void onStartTick(MinecraftServer server) {
        updateContext();
        updateBounds();

        Editor.update();
    }

    private void updateContext() {
        var context = getContext();
        if (context != null) {
            context.syncPlayers();

            var state = getState().update(context);
            if (state != null) setState(state);

            context.update();
        }
    }

    private void updateBounds() {
        var world = server.getOverworld();
        var playerEntities = server.getPlayerManager().getPlayerList();

        var properties = getProperties();

        for (var playerEntity : playerEntities) {
            if (hasOperator(playerEntity)) continue;
            if (playerEntity.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                LOGGER.info("Player '" + playerEntity.getEntityName() + "' in incorrect gamemode");
                playerEntity.interactionManager.changeGameMode(GameMode.SURVIVAL);
                playerEntity.kill();
                playerEntity.setHealth(0.0F);
            } else if (playerEntity.isAlive()) {
                if (
                    (playerEntity.getWorld() != world) || (playerEntity.getY() < -256.0) ||
                    (getRuleBoolean(Game.RULE_KILLZONE_BOUNDS_ENABLED) && properties.getRegionLegal().excludes(playerEntity))
                ) {
                    LOGGER.info("Player '" + playerEntity.getEntityName() + "' entered out of bounds killzone or is out of the world");
                    var position = properties.getPositionSpawn().toCenterPos();
                    playerEntity.teleport(
                        world,
                        position.getX(), position.getY(), position.getZ(),
                        playerEntity.getYaw(), playerEntity.getPitch()
                    );
                    Scheduler.now((schedule) -> {
                        playerEntity.kill();
                        playerEntity.setHealth(0.0F);
                    });
                } else if (context != null && getState() instanceof PlayingGameState) {
                    var player = context.getPlayer(playerEntity);
                    if (
                        player.isPlaying() && player.isAlive() &&
                        getRuleBoolean(Game.RULE_KILLZONE_BLIMP_ENABLED) &&
                        properties.getRegionBlimp().contains(playerEntity)
                    ) {
                        playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40));
                    }
                }
            }
        }
    }

    public @NotNull GameContext initialize(@NotNull GameProperties properties) {
        Objects.requireNonNull(properties, "Argument 'properties'");
        if (context == null) {
            if (this.properties != properties) {
                this.properties = properties;
                LOGGER.info("Game properties changed to " + properties.getClass().getSimpleName());
            }
            context = new GameContext(this);
            context.initialize();
            LOGGER.info("Game initialized");
        } else {
            throw new IllegalStateException("Attempted to double-initialize game");
        }
        return context;
    }

    public void destroy() {
        if (context != null) {
            context.destroy();
            context = null;
        }
        setState(new WaitingGameState());
    }

}
