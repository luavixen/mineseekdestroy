package dev.foxgirl.mineseekdestroy;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public enum GameTeam {

    NONE(null, null, null, null, null),
    OPERATOR(Text.of("ADMIN"), "msd_operator", null, Formatting.GREEN, null),
    PLAYER_DUEL(Text.of("DUEL"), "msd_duel", "msd_duel_dead", Formatting.RED, Formatting.DARK_GRAY),
    PLAYER_BLACK(Text.of("BLACK"), "msd_black", "msd_black_dead", Formatting.DARK_PURPLE, Formatting.DARK_GRAY),
    PLAYER_YELLOW(Text.of("YELLOW"), "msd_yellow", "msd_yellow_dead", Formatting.YELLOW, Formatting.GOLD),
    PLAYER_BLUE(Text.of("BLUE"), "msd_blue", "msd_blue_dead", Formatting.AQUA, Formatting.BLUE);

    private final Text displayName;
    private final String nameAlive;
    private final String nameDead;
    private final Formatting colorAlive;
    private final Formatting colorDead;

    GameTeam(Text displayName, String nameAlive, String nameDead, Formatting colorAlive, Formatting colorDead) {
        this.displayName = displayName;
        this.nameAlive = nameAlive;
        this.nameDead = nameDead;
        this.colorAlive = colorAlive;
        this.colorDead = colorDead;
    }

    public boolean isPlaying() {
        return this != NONE && this != OPERATOR;
    }

    public boolean isOperator() {
        return this == OPERATOR;
    }

    public @NotNull Formatting getColor() {
        return colorAlive != null ? colorAlive : Formatting.GREEN;
    }

    public @NotNull Text getName() {
        return displayName != null ? displayName : ScreenTexts.EMPTY;
    }

    public @NotNull Text getNameColored() {
        return getName().copy().formatted(getColor());
    }

    private static Team getOrCreateTeam(Scoreboard scoreboard, String name, Consumer<Team> initializer) {
        var team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.addTeam(name);
            initializer.accept(team);
        }
        return team;
    }

    public @Nullable Team getAliveTeam(@Nullable Scoreboard scoreboard) {
        if (scoreboard == null || nameAlive == null) {
            return null;
        }
        return getOrCreateTeam(scoreboard, nameAlive, team -> {
            team.setDisplayName(displayName);
            team.setColor(colorAlive);
        });
    }

    public @Nullable Team getDeadTeam(@Nullable Scoreboard scoreboard) {
        if (scoreboard == null || nameDead == null) {
            return null;
        }
        return getOrCreateTeam(scoreboard, nameDead, team -> {
            team.setDisplayName(displayName);
            team.setColor(colorDead);
            team.setPrefix(Text.of("\u2620 "));
        });
    }

}
