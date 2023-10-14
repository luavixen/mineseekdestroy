package dev.foxgirl.mineseekdestroy;

import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public enum GameTeam {

    NONE(Text.of("NONE"), null, null, null, Formatting.WHITE, null),
    SKIP(Text.of("SKIP"), "msd_skip", "msd_skip_dead", null, Formatting.GREEN, Formatting.GRAY),
    GHOST(Text.of("GHOST"), "msd_ghost", "msd_ghost_dead", null, Formatting.GRAY, Formatting.GRAY),
    OPERATOR(Text.of("ADMIN"), "msd_operator", null, null, Formatting.GREEN, null),
    PLAYER_DUEL(Text.of("DUEL"), "msd_duel", "msd_duel_dead", "msd_duel_damaged", Formatting.RED, Formatting.DARK_GRAY),
    PLAYER_WARDEN(Text.of("WARDEN"), "msd_warden", "msd_warden_dead", "msd_warden_damaged", Formatting.RED, Formatting.DARK_RED),
    PLAYER_BLACK(Text.of("BLACK"), "msd_black", "msd_black_dead", "msd_black_damaged", Formatting.DARK_PURPLE, Formatting.DARK_GRAY),
    PLAYER_YELLOW(Text.of("YELLOW"), "msd_yellow", "msd_yellow_dead", "msd_yellow_damaged", Formatting.YELLOW, Formatting.GOLD),
    PLAYER_BLUE(Text.of("BLUE"), "msd_blue", "msd_blue_dead", "msd_blue_damaged", Formatting.AQUA, Formatting.BLUE);

    private final Text displayName;
    private final String nameAlive;
    private final String nameDead;
    private final String nameDamaged;
    private final Formatting colorAlive;
    private final Formatting colorDead;
    private final List<String> names;

    GameTeam(Text displayName, String nameAlive, String nameDead, String nameDamaged, Formatting colorAlive, Formatting colorDead) {
        this.displayName = displayName;
        this.nameAlive = nameAlive;
        this.nameDead = nameDead;
        this.nameDamaged = nameDamaged;
        this.colorAlive = colorAlive;
        this.colorDead = colorDead;

        var names = ImmutableList.<String>builder(3);
        if (nameAlive != null) names.add(nameAlive);
        if (nameDead != null) names.add(nameDead);
        if (nameDamaged != null) names.add(nameDamaged);
        this.names = names.build();
    }

    public boolean isPlaying() {
        return this != NONE && this != SKIP && this != GHOST && this != OPERATOR;
    }

    public boolean isOperator() {
        return this == OPERATOR;
    }

    public boolean isSpectator() {
        return this == NONE;
    }

    public boolean isGhost() {
        return this == SKIP || this == GHOST;
    }

    public boolean isPlayingOrGhost() {
        return this != NONE && this != OPERATOR;
    }

    public boolean isOnScoreboard() {
        return this != NONE && this != GHOST && this != OPERATOR;
    }

    public boolean isCannon() {
        return this == PLAYER_YELLOW || this == PLAYER_BLUE;
    }

    public @Nullable String getName() {
        return nameAlive;
    }

    public @NotNull List<@NotNull String> getNames() {
        return names;
    }

    public @NotNull Formatting getColor() {
        return colorAlive != null ? colorAlive : Formatting.WHITE;
    }

    public @NotNull Text getDisplayName() {
        return displayName != null ? displayName.copy().formatted(getColor()) : ScreenTexts.EMPTY;
    }

    private static Team getOrCreateTeam(Scoreboard scoreboard, String name, Consumer<Team> initializer) {
        var team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.addTeam(name);
            initializer.accept(team);
        }
        return team;
    }

    public @Nullable Team getAliveTeam(@NotNull Scoreboard scoreboard) {
        Objects.requireNonNull(scoreboard, "Argument 'scoreboard'");
        if (nameAlive == null) return null;
        return getOrCreateTeam(scoreboard, nameAlive, team -> {
            team.setDisplayName(displayName);
            team.setColor(colorAlive);
        });
    }

    public @Nullable Team getDeadTeam(@NotNull Scoreboard scoreboard) {
        Objects.requireNonNull(scoreboard, "Argument 'scoreboard'");
        if (nameDead == null) return null;
        return getOrCreateTeam(scoreboard, nameDead, team -> {
            team.setDisplayName(displayName);
            team.setColor(colorDead);
            team.setPrefix(Text.of("\u2620 "));
        });
    }

    public @Nullable Team getDamagedTeam(@NotNull Scoreboard scoreboard) {
        Objects.requireNonNull(scoreboard, "Argument 'scoreboard'");
        if (nameDamaged == null) return null;
        return getOrCreateTeam(scoreboard, nameDamaged, team -> {
            team.setDisplayName(displayName);
            team.setColor(Formatting.LIGHT_PURPLE);
        });
    }

}
