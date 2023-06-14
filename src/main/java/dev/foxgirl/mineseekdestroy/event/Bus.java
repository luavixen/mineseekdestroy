package dev.foxgirl.mineseekdestroy.event;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public final class Bus {

    private Bus() {
    }

    @FunctionalInterface
    public interface Listener {
        void handle(@NotNull Event event);
    }

    @FunctionalInterface
    public interface Subscription {
        boolean unsubscribe();
    }

    private static final ArrayList<Handler> handlers = new ArrayList<>();
    private static final Object lock = new Object();

    private static final class Handler implements Subscription {
        private final Listener listener;

        private Handler(Listener listener) {
            this.listener = listener;
            synchronized (lock) {
                handlers.add(this);
            }
        }

        private void handle(Event event) {
            listener.handle(event);
        }

        @Override
        public boolean unsubscribe() {
            synchronized (lock) {
                return handlers.remove(this);
            }
        }
    }

    public static @NotNull Subscription subscribe(@NotNull Listener listener) {
        Objects.requireNonNull(listener, "Argument 'listener'");
        return new Handler(listener);
    }

    public static <T extends Event> @NotNull T publish(@NotNull T event) {
        Objects.requireNonNull(event, "Argument 'event'");
        Object[] handlerArray;
        synchronized (lock) {
            handlerArray = handlers.toArray();
        }
        for (Object handler : handlerArray) {
            try {
                ((Handler) handler).handle(event);
            } catch (Exception cause) {
                Game.LOGGER.error("Unexpected exception handling event", cause);
            }
        }
        return event;
    }

}
