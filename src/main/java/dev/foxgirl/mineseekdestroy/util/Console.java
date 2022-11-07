package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
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
    void sendInfo(Object ...values);

    /**
     * Sends error feedback to this console.
     *
     * @param values
     *   Message to send as feedback, values will be converted to strings and
     *   joined with spaces.
     */
    void sendError(Object ...values);

    static @NotNull Style STYLE_INFO = Style.EMPTY.withFormatting(Formatting.LIGHT_PURPLE);
    static @NotNull Style STYLE_ERROR = Style.EMPTY.withFormatting(Formatting.RED);

    static @NotNull MutableText format(@Nullable Object @NotNull [] values, boolean error) {
        if (values == null) throw new NullPointerException("Argument 'values'");
        if (values.length == 0) return Text.empty();
        Style style = error ? STYLE_ERROR : STYLE_INFO;
        MutableText message = null;
        for (Object value : values) {
            MutableText part;
            if (value instanceof Text) {
                part = ((Text) value).copy();
                part.setStyle(part.getStyle().withParent(style));
            } else {
                part = Text.literal(String.valueOf(value)).setStyle(style);
            }
            if (message == null) {
                message = part;
            } else {
                message.append(Text.literal(" "));
                message.append(part);
            }
        }
        return message;
    }

}
