package dev.foxgirl.mineseekdestroy.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Formats the given array of values for display in console feedback.
     *
     * @param values
     *   Message to format, values will be converted to strings and joined
     *   with spaces.
     * @return Formatted message string.
     */
    static @NotNull String formatValues(@Nullable Object @NotNull [] values) {
        Objects.requireNonNull(values, "Argument 'values'");
        if (values.length == 0) return "";
        if (values.length == 1) return String.valueOf(values[0]);
        return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(" "));
    }

}
