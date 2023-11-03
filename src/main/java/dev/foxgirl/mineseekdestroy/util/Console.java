package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Console {

    /**
     * Sends feedback to this console.
     *
     * @param values
     *   Message to send as feedback, values will be converted to strings and
     *   joined with spaces.
     */
    void sendInfo(Object... values);

    /**
     * Sends error feedback to this console.
     *
     * @param values
     *   Message to send as feedback, values will be converted to strings and
     *   joined with spaces.
     */
    void sendError(Object... values);

    static @NotNull Style STYLE_INFO = Style.EMPTY.withFormatting(Formatting.LIGHT_PURPLE);
    static @NotNull Style STYLE_ERROR = Style.EMPTY.withFormatting(Formatting.RED);

    static @NotNull MutableText format(@Nullable Object @NotNull [] values, boolean error) {
        return TextsKt.style(TextsKt.text(values), style -> style.withParent(error ? STYLE_ERROR : STYLE_INFO));
    }

    static @NotNull MutableText formatInfo(@Nullable Object... values) {
        return format(values, false);
    }
    static @NotNull MutableText formatError(@Nullable Object... values) {
        return format(values, true);
    }

}
