package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object Texts {
    fun text(): MutableText = Text.empty()
    fun text(string: String): MutableText = Text.literal(string)

    fun translatable(string: String): MutableText = Text.translatable(string)
    fun translatable(string: String, vararg args: Any?): MutableText = Text.translatable(string, args)
}

fun String.text() = Texts.text(this)
fun String.translatable() = Texts.translatable(this)

operator fun MutableText.plus(text: Text): MutableText {
    this.append(text)
    return this
}
operator fun MutableText.plus(string: String): MutableText {
    this.append(string)
    return this
}

operator fun MutableText.plus(player: GamePlayer) = this + player.displayName
operator fun MutableText.plus(team: GameTeam) = this + team.displayName

operator fun MutableText.times(formatting: Formatting): MutableText {
    this.formatted(formatting)
    return this
}
operator fun MutableText.times(style: Style): MutableText {
    this.fillStyle(style)
    return this
}

fun MutableText.style(block: (Style) -> Style?): MutableText {
    val style = block(this.style)
    if (style != null) this.style = style
    return this
}

operator fun MutableText.times(team: GameTeam) = this * team.color

fun MutableText.bold() = this * Formatting.BOLD
fun MutableText.italics() = this * Formatting.ITALIC
fun MutableText.underline() = this * Formatting.UNDERLINE
fun MutableText.strikethrough() = this * Formatting.STRIKETHROUGH
fun MutableText.obfuscated() = this * Formatting.OBFUSCATED

fun MutableText.reset() = this.style {
    Style.EMPTY
        .withBold(false)
        .withItalic(false)
        .withUnderline(false)
        .withStrikethrough(false)
        .withObfuscated(false)
        .withColor(Formatting.WHITE)
}

fun MutableText.teamGhost() = this * GameTeam.GHOST
fun MutableText.teamOperator() = this * GameTeam.OPERATOR
fun MutableText.teamDuel() = this * GameTeam.PLAYER_DUEL
fun MutableText.teamWarden() = this * GameTeam.PLAYER_WARDEN
fun MutableText.teamBlack() = this * GameTeam.PLAYER_BLACK
fun MutableText.teamYellow() = this * GameTeam.PLAYER_YELLOW
fun MutableText.teamBlue() = this * GameTeam.PLAYER_BLUE

fun MutableText.green() = this * Formatting.GREEN
fun MutableText.yellow() = this * Formatting.YELLOW
fun MutableText.blue() = this * Formatting.BLUE

fun Item.name(): MutableText = this.name.copy()
fun ItemStack.name(): MutableText = this.name.copy()
