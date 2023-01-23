package dev.foxgirl.mineseekdestroy.util;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Fuck provides functionality for side-stepping the JVM and creating instances
 * of classes without running any constructors.
 */
public final class Fuck {

    /**
     * Instance of the internal {@link sun.misc.Unsafe} class.
     */
    public static final @NotNull Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException err) {
            throw new RuntimeException(err);
        }
    }

    /**
     * Allocates an instance of the given class without running any
     * constructors.
     * @param <T> Instance type.
     * @param clazz Class to instantiate.
     * @return Newly created instance.
     * @throws RuntimeException If the operation fails.
     * @throws NullPointerException If {@code clazz} is null.
     */
    public static <T> @NotNull T create(@NotNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "Argument 'clazz'");
        try {
            @SuppressWarnings("unchecked")
            var object = (T) UNSAFE.allocateInstance(clazz);
            return object;
        } catch (ReflectiveOperationException err) {
            throw new RuntimeException(err);
        }
    }

}
