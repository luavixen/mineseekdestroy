package dev.foxgirl.mineseekdestroy.util;

import org.jetbrains.annotations.Nullable;

public interface Console {

    /**
     * Sends feedback to this console.
     *
     * @param values
     *   Message to send as feedback, values will be converted to strings and
     *   joined with spaces.
     */
    void sendInfo(@Nullable Object ...values);

    /**
     * Sends error feedback to this console.
     *
     * @param values
     *   Message to send as feedback, values will be converted to strings and
     *   joined with spaces.
     */
    void sendError(@Nullable Object ...values);

}
