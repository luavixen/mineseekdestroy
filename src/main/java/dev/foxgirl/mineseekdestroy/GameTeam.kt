package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.util.collect.buildImmutableList
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Formatting.*
import java.util.*
import net.minecraft.scoreboard.Team as ScoreboardTeam

enum class GameTeam(
    val color: Formatting = WHITE,
    val colorDead: Formatting = WHITE,
    private val hasTeam: Boolean = true,
    private val hasDeadTeam: Boolean = true,
    private val hasUndeadTeam: Boolean = true,
    private val hasDamagedTeam: Boolean = true,
) {

    // Definitions

    // Non-playing teams
    NONE(hasTeam = false),
    SKIP(color = GREEN, colorDead = GRAY, hasUndeadTeam = false, hasDamagedTeam = false),
    GHOST(color = GRAY, hasUndeadTeam = false, hasDamagedTeam = false),
    OPERATOR(color = GREEN, hasDeadTeam = false, hasUndeadTeam = false, hasDamagedTeam = false),
    // Actively playing teams
    DUELIST(color = RED, colorDead = DARK_GRAY, hasUndeadTeam = false),
    WARDEN(color = RED, colorDead = DARK_RED, hasUndeadTeam = false),
    BLACK(color = DARK_PURPLE, colorDead = DARK_GRAY, hasUndeadTeam = false),
    // Actively playing and canon teams
    YELLOW(color = Formatting.YELLOW, colorDead = Formatting.GOLD),
    BLUE(color = Formatting.AQUA, colorDead = Formatting.BLUE);

    // Implementation

    /** Is this player actively playing in the game? (not a ghost) */
    val isPlaying
        get() = this != NONE && this != SKIP && this != GHOST && this != OPERATOR
    /** Is this player actively playing in the game? (including ghosts) */
    val isPlayingOrGhost
        get() = this != NONE && this != OPERATOR
    /** Is this player a ghost? */
    val isGhost
        get() = this == SKIP || this == GHOST
    /** Is this player an operator? (on the operator team) */
    val isOperator
        get() = this == OPERATOR
    /** Is this player a spectator? (not a ghost) */
    val isSpectator
        get() = this == NONE
    /** Should this player be displayed on the scoreboard? */
    val isOnScoreboard
        get() = this != NONE && this != GHOST && this != OPERATOR
    /** Should this player be considered when automatically ending the round? */
    val isCanon
        get() = this == YELLOW || this == BLUE

    val displayName: Text = Text.literal(name).formatted(color)

    val teamName: String?
    val teamNameDead: String?
    val teamNameUndead: String?
    val teamNameDamaged: String?

    val teamNames: List<String>

    init {
        if (hasTeam) {
            teamName = "msd_${name.lowercase(Locale.ROOT)}"
            teamNameDead = if (hasDeadTeam) teamName + "_dead" else null
            teamNameUndead = if (hasUndeadTeam) teamName + "_undead" else null
            teamNameDamaged = if (hasDamagedTeam) teamName + "_damaged" else null
        } else {
            teamName = null
            teamNameDead = null
            teamNameUndead = null
            teamNameDamaged = null
        }
        teamNames = buildImmutableList(4) {
            if (teamName != null) add(teamName)
            if (teamNameDead != null) add(teamNameDead)
            if (teamNameUndead != null) add(teamNameUndead)
            if (teamNameDamaged != null) add(teamNameDamaged)
        }
    }

    private fun getOrCreateTeam(
        scoreboard: Scoreboard,
        name: String?, color: Formatting, block: (ScoreboardTeam) -> Unit = {},
    ): ScoreboardTeam? {
        if (name != null) {
            var team = scoreboard.getTeam(name)
            if (team == null) {
                team = scoreboard.addTeam(name)
                team.displayName = Text.literal(name).formatted(color)
                team.color = color
                block(team)
            }
            return team
        }
        return null
    }

    fun getTeam(scoreboard: Scoreboard)
        = getOrCreateTeam(scoreboard, teamName, color)
    fun getDeadTeam(scoreboard: Scoreboard)
        = getOrCreateTeam(scoreboard, teamNameDead, colorDead) { it.prefix = Text.of("\u2620 ") }
    fun getUndeadTeam(scoreboard: Scoreboard)
        = getOrCreateTeam(scoreboard, teamNameUndead, colorDead) { it.prefix = Text.of("\u2620 ") }
    fun getDamagedTeam(scoreboard: Scoreboard)
        = getOrCreateTeam(scoreboard, teamNameDamaged, LIGHT_PURPLE)

}
