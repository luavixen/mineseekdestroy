package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

inline fun text(): MutableText = Text.empty()
inline fun text(string: String): MutableText = Text.literal(string)

fun text(value: Any?): MutableText {
    return when (value) {
        null -> text("null")
        is Text -> value.copy()
        is String -> value.asText()
        is Boolean -> value.toString().asText().format(if (value) Formatting.GREEN else Formatting.RED)
        is Number -> value.toString().asText().format(Formatting.YELLOW)
        else -> {
            val clazz = value::class.java
            val handle = Reflector.methodHandle(clazz, "getDisplayName")
            if (handle != null) {
                val result = handle.invoke(value)
                if (result !is Text) {
                    throw IllegalStateException("Method getDisplayName of class ${clazz.simpleName} returned ${result::class.java.simpleName} instead of Text")
                }
                result.copy()
            } else {
                value.toString().asText().format(Formatting.WHITE)
            }
        }
    }
}
fun text(vararg values: Any?): MutableText {
    val message = text()
    if (values.isNotEmpty()) {
        message.append(text(values[0]))
        for (i in 1 until values.size) {
            message.append(text(" "))
            message.append(text(values[i]))
        }
    }
    return message
}

fun translatable(string: String): MutableText = Text.translatable(string)
fun translatable(string: String, vararg args: Any?): MutableText = Text.translatable(string, *args)

fun String.asText() = text(this)
fun String.asTranslatable() = translatable(this)

fun MutableText.format(vararg formatting: Formatting): MutableText {
    this.formatted(*formatting)
    return this
}

inline fun MutableText.style(block: (Style) -> Style?): MutableText {
    val style = block(this.style)
    if (style != null) this.style = style
    return this
}
inline fun MutableText.styleParent(block: (Style) -> Style?): MutableText {
    val style = block(this.style)
    if (style != null) this.style = this.style.withParent(style)
    return this
}

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
    this.format(formatting)
    return this
}
operator fun MutableText.times(style: Style): MutableText {
    this.fillStyle(style)
    return this
}

operator fun MutableText.times(team: GameTeam) = this * team.color

fun MutableText.black() = this * Formatting.BLACK
fun MutableText.darkBlue() = this * Formatting.DARK_BLUE
fun MutableText.darkGreen() = this * Formatting.DARK_GREEN
fun MutableText.darkAqua() = this * Formatting.DARK_AQUA
fun MutableText.darkRed() = this * Formatting.DARK_RED
fun MutableText.darkPurple() = this * Formatting.DARK_PURPLE
fun MutableText.gold() = this * Formatting.GOLD
fun MutableText.gray() = this * Formatting.GRAY
fun MutableText.darkGray() = this * Formatting.DARK_GRAY
fun MutableText.blue() = this * Formatting.BLUE
fun MutableText.green() = this * Formatting.GREEN
fun MutableText.aqua() = this * Formatting.AQUA
fun MutableText.red() = this * Formatting.RED
fun MutableText.lightPurple() = this * Formatting.LIGHT_PURPLE
fun MutableText.yellow() = this * Formatting.YELLOW
fun MutableText.white() = this * Formatting.WHITE
fun MutableText.obfuscated() = this * Formatting.OBFUSCATED
fun MutableText.bold() = this * Formatting.BOLD
fun MutableText.strikethrough() = this * Formatting.STRIKETHROUGH
fun MutableText.underline() = this * Formatting.UNDERLINE
fun MutableText.italic() = this * Formatting.ITALIC

fun MutableText.teamGhost() = this * GameTeam.GHOST
fun MutableText.teamOperator() = this * GameTeam.OPERATOR
fun MutableText.teamDuel() = this * GameTeam.DUELIST
fun MutableText.teamWarden() = this * GameTeam.WARDEN
fun MutableText.teamBlack() = this * GameTeam.BLACK
fun MutableText.teamYellow() = this * GameTeam.YELLOW
fun MutableText.teamBlue() = this * GameTeam.BLUE

private val mnsndItemName = Style.EMPTY.withColor(Formatting.GREEN).withItalic(false)
private val mnsndItemLore = Style.EMPTY.withColor(Formatting.GREEN).withItalic(true)

fun MutableText.mnsndItemName() = this.styleParent { mnsndItemName }
fun MutableText.mnsndItemLore() = this.styleParent { mnsndItemLore }

fun Item.name(): MutableText = this.name.copy()
fun ItemStack.name(): MutableText = this.name.copy()
