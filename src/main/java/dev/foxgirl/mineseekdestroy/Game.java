package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.command.Command;
import dev.foxgirl.mineseekdestroy.state.GameState;
import dev.foxgirl.mineseekdestroy.state.WaitingGameState;
import dev.foxgirl.mineseekdestroy.util.Console;
import dev.foxgirl.mineseekdestroy.util.Editor;
import dev.foxgirl.mineseekdestroy.util.ExtraEvents;
import dev.foxgirl.mineseekdestroy.util.async.Scheduler;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableSet;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Game implements Console, DedicatedServerModInitializer, ServerLifecycleEvents.ServerStarting, ServerTickEvents.StartTick {

    public static final @NotNull Logger LOGGER = LogManager.getLogger("MnSnD");

    public static final @NotNull Path CONFIGDIR = FabricLoader.getInstance().getConfigDir();

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

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_HIDDEN_ARMOR_ENABLED =
        GameRuleRegistry.register("msdHiddenArmorEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_ENHANCED_FURNACES_ENABLED =
        GameRuleRegistry.register("msdEnhancedFurnacesEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_BLUE_MELEE_CRITS =
        GameRuleRegistry.register("msdBlueMeleeCrits", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_BLUE_ARROW_CRITS =
        GameRuleRegistry.register("msdBlueArrowCrits", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_DAMAGE_FLASH_ENABLED =
        GameRuleRegistry.register("msdDamageFlashEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_LOOT_COUNT =
        GameRuleRegistry.register("msdLootCount", GameRules.Category.MISC, GameRuleFactory.createIntRule(5));

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

    public static final @NotNull GameRules.Key<DoubleRule> RULE_FANS_EFFECT_DURATION =
        GameRuleRegistry.register("msdFansEffectDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(20.0));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_FANS_KNOCKBACK =
        GameRuleRegistry.register("msdFansKnockback", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(2.0));

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
    public static final @NotNull GameRules.Key<DoubleRule> RULE_SUMMONS_ALTAR_GLOW_DURATION =
        GameRuleRegistry.register("msdSummonsAltarGlowDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(15.0));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SUMMONS_SHOW_TOOLTIP_ENABLED =
        GameRuleRegistry.register("msdSummonsShowTooltipEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SOULS_DROPPING_ENABLED =
        GameRuleRegistry.register("msdSoulsDroppingEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SOULS_CONSUMING_ENABLED =
        GameRuleRegistry.register("msdSoulsConsumingEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_SOULS_CONSUMING_EFFECT_DURATION =
        GameRuleRegistry.register("msdSoulsConsumingEffectDuration", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(10.0));
    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_SOULS_CONSUMING_EFFECT_JUMP_STRENGTH =
        GameRuleRegistry.register("msdSoulsConsumingEffectJumpStrength", GameRules.Category.MISC, GameRuleFactory.createIntRule(8));
    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_SOULS_CONSUMING_EFFECT_SPEED_STRENGTH =
        GameRuleRegistry.register("msdSoulsConsumingEffectSpeedStrength", GameRules.Category.MISC, GameRuleFactory.createIntRule(6));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SOULS_GIVE_YELLOW_OWN_SOUL_ENABLED =
        GameRuleRegistry.register("msdSoulsGiveYellowOwnSoulEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SOULS_GIVE_BLUE_OWN_SOUL_ENABLED =
        GameRuleRegistry.register("msdSoulsGiveBlueOwnSoulEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_SOULS_GIVE_HEROBRINES_SOUL_ENABLED =
        GameRuleRegistry.register("msdSoulsGiveHerobrinesSoulEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_BUDDY_ENABLED =
        GameRuleRegistry.register("msdBuddyEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    public static final @NotNull GameRules.Key<DoubleRule> RULE_BUDDY_HEALTH_PENALTY =
        GameRuleRegistry.register("msdBuddyHealthPenalty", GameRules.Category.MISC, GameRuleFactory.createDoubleRule(8.0));
    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_BUDDY_ABSORPTION_STRENGTH =
        GameRuleRegistry.register("msdBuddyAbsorptionStrength", GameRules.Category.MISC, GameRuleFactory.createIntRule(1));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_CHAOS_ENABLED =
        GameRuleRegistry.register("msdChaosEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));

    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_COUNTDOWN_ENABLED =
        GameRuleRegistry.register("msdCountdownEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.BooleanRule> RULE_COUNTDOWN_AUTOSTART_ENABLED =
        GameRuleRegistry.register("msdCountdownAutostartEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_COUNTDOWN_TEXT_FADEIN_DURATION =
        GameRuleRegistry.register("msdCountdownTextFadeinDuration", GameRules.Category.MISC, GameRuleFactory.createIntRule(5));
    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_COUNTDOWN_TEXT_STAY_DURATION =
        GameRuleRegistry.register("msdCountdownTextStayDuration", GameRules.Category.MISC, GameRuleFactory.createIntRule(20));
    public static final @NotNull GameRules.Key<GameRules.IntRule> RULE_COUNTDOWN_TEXT_FADEOUT_DURATION =
        GameRuleRegistry.register("msdCountdownTextFadeoutDuration", GameRules.Category.MISC, GameRuleFactory.createIntRule(10));

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
        Items.BUCKET,
        Items.MILK_BUCKET,
        Items.LAVA_BUCKET,
        Items.WATER_BUCKET,
        Items.POWDER_SNOW_BUCKET,
        Items.AXOLOTL_BUCKET,
        Items.COD_BUCKET,
        Items.PUFFERFISH_BUCKET,
        Items.SALMON_BUCKET,
        Items.TADPOLE_BUCKET,
        Items.TROPICAL_FISH_BUCKET,
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
        Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
        Items.BOW, Items.CROSSBOW,
        Items.TRIDENT,
        Items.ELYTRA,
        Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
        Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
        Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
        Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
        Items.SKELETON_SKULL, Items.BONE, Items.COBWEB, Items.GHAST_TEAR,
    });

    public static final @NotNull Set<@NotNull Item> STACKABLE_ITEMS = ImmutableSet.copyOf(new Item[] {
        Items.EGG, Items.SNOWBALL,
        Items.ENDER_PEARL,
        Items.POTION,
        Items.SPLASH_POTION,
        Items.LINGERING_POTION,
        Items.GLASS_BOTTLE,
        Items.HONEY_BOTTLE,
        Items.EXPERIENCE_BOTTLE,
        Items.BOOK,
        Items.WRITTEN_BOOK,
        Items.WRITABLE_BOOK,
        Items.KNOWLEDGE_BOOK,
        Items.ENCHANTED_BOOK,
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

    public static final @NotNull RegistryKey<DamageType> DAMAGE_TYPE_HEARTBREAK =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("mineseekdestroy", "heartbreak"));
    public static final @NotNull RegistryKey<DamageType> DAMAGE_TYPE_ABYSS =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("mineseekdestroy", "abyss"));
    public static final @NotNull RegistryKey<DamageType> DAMAGE_TYPE_BITTEN =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("mineseekdestroy", "bitten"));

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

    public void setState(@NotNull GameState state) {
        Objects.requireNonNull(state, "Argument 'state'");
        if (this.state != state) {
            this.state = state;
            LOGGER.info("Game state changed to " + state.getName());
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
        PlayerBlockBreakEvents.BEFORE.register((world, playerEntity, blockPos, blockState, blockEntity) -> getState().allowBlockBreak(getContext(), playerEntity, world, blockPos, blockState, blockEntity));
        ExtraEvents.BLOCK_USED.register((playerEntity, world, hand, blockHit, blockState) -> getState().onUseBlock(getContext(), playerEntity, world, hand, blockHit, blockState));
        ExtraEvents.BLOCK_USED_WITH.register((playerEntity, world, hand, blockHit, blockItemStack) -> getState().onUseBlockWith(getContext(), playerEntity, world, hand, blockHit, blockItemStack));
        ExtraEvents.ITEM_USED.register((playerEntity, world, hand, stack) -> getState().onUseItem(getContext(), playerEntity, world, hand, stack));
        UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHit) -> getState().onUseEntity(getContext(), playerEntity, world, hand, entity, entityHit));
        AttackBlockCallback.EVENT.register((playerEntity, world, hand, pos, direction) -> getState().onAttackBlock(getContext(), playerEntity, world, hand, pos, direction));
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHit) -> getState().onAttackEntity(getContext(), playerEntity, world, hand, entity, entityHit));
        ExtraEvents.ITEM_DROPPED.register((playerEntity, stack, throwRandomly, retainOwnership) -> getState().onItemDropped(getContext(), playerEntity, stack, throwRandomly, retainOwnership));
        ExtraEvents.ITEM_ACQUIRED.register((playerEntity, inventory, stack, slot) -> getState().onItemAcquired(getContext(), playerEntity, inventory, stack, slot));
    }

    @Override
    public void onServerStarting(MinecraftServer server) {
        Objects.requireNonNull(server, "Argument 'server'");
        this.server = server;
    }

    @Override
    public void onStartTick(MinecraftServer server) {
        updateContext();
        updateBounds();
        updateHunger();

        Scheduler.update();
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
                LOGGER.info("OOB Player '{}' in incorrect gamemode", playerEntity.getEntityName());
                LOGGER.info("OOB Switching player '{}' to survival gamemode", playerEntity.getEntityName());
                playerEntity.interactionManager.changeGameMode(GameMode.SURVIVAL);
                LOGGER.info("OOB Killing player '{}'", playerEntity.getEntityName());
                playerEntity.damage(playerEntity.getDamageSources().outsideBorder(), 5000.0F);
                playerEntity.setHealth(0.0F);
            } else if (playerEntity.isAlive()) {
                if (
                    (playerEntity.getWorld() != world) || (playerEntity.getY() < -256.0) || (
                        getRuleBoolean(Game.RULE_KILLZONE_BOUNDS_ENABLED) &&
                        properties.getRegionAll().excludes(playerEntity) &&
                        properties.getRegionLegal().excludes(playerEntity) &&
                        properties.getRegionPlayable().excludes(playerEntity)
                    )
                ) {
                    LOGGER.info("OOB Player '{}' entered out of bounds killzone or is out of the world", playerEntity.getEntityName());
                    LOGGER.info("OOB Killing player '{}'", playerEntity.getEntityName());
                    playerEntity.damage(playerEntity.getDamageSources().outsideBorder(), 5000.0F);
                    playerEntity.setHealth(0.0F);
                } else if (context != null && getState().isPlaying()) {
                    var player = context.getPlayer(playerEntity);
                    if (
                        player.isPlaying() && player.isAlive() &&
                        getRuleBoolean(Game.RULE_KILLZONE_BLIMP_ENABLED) && (
                            properties.getRegionBlimp().contains(playerEntity) ||
                            properties.getRegionBlimpBalloons().contains(playerEntity)
                        )
                    ) {
                        playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40));
                    }
                }
            }
        }
    }

    private void updateHunger() {
        if (context == null) {
            for (var playerEntity : server.getPlayerManager().getPlayerList()) {
                if (!playerEntity.isAlive()) continue;
                var hungerManager = playerEntity.getHungerManager();
                hungerManager.setFoodLevel(20);
                hungerManager.setSaturationLevel(5.0F);
                hungerManager.setExhaustion(0.0F);
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
            LOGGER.info("Game destroyed");
        }
        setState(new WaitingGameState());
    }

}
