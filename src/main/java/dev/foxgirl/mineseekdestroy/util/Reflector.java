package dev.foxgirl.mineseekdestroy.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * Reflector provides various tools for manipulating and side-stepping the
 * restrictions of the JVM, specifically OpenJDK.
 */
public final class Reflector {

    private Reflector() {
    }

    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup LOOKUP;

    private static final Method METHOD_setAccessible0;
    private static final Method METHOD_implAddExportsOrOpens;

    static {
        try {
            var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);

            UNSAFE = (Unsafe) unsafeField.get(null);

            var moduleField = Class.class.getDeclaredField("module");
            var moduleFieldOffset = UNSAFE.objectFieldOffset(moduleField);
            UNSAFE.putObject(Reflector.class, moduleFieldOffset, Object.class.getModule()); // We're the java.lang module now :3

            METHOD_setAccessible0 = AccessibleObject.class.getDeclaredMethod("setAccessible0", boolean.class);
            METHOD_setAccessible0.setAccessible(true);

            METHOD_implAddExportsOrOpens = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
            METHOD_setAccessible0.invoke(METHOD_implAddExportsOrOpens, true);

            var lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            METHOD_setAccessible0.invoke(lookupField, true);

            LOOKUP = (MethodHandles.Lookup) lookupField.get(null);
        } catch (Exception cause) {
            throw new RuntimeException("Reflector JVM manipulation failed", cause);
        }
    }

    /**
     * Returns the default instance of {@link Unsafe} provided by the JVM.
     * @return Instance of {@link Unsafe}.
     */
    public static @NotNull Unsafe unsafe() {
        return UNSAFE;
    }
    /**
     * Returns an instance of {@link MethodHandles.Lookup} that is trusted by
     * the JVM and has full access rights.
     * @return Instance of {@link MethodHandles.Lookup}.
     */
    public static @NotNull MethodHandles.Lookup lookup() {
        return LOOKUP;
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
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeException(cause);
        }
    }

    /**
     * Sets an {@link AccessibleObject} as accessible.
     * @param <T> Object type.
     * @param object Object to mark as accessible.
     * @return Original object, now marked as accessible.
     * @throws RuntimeException If the operation fails.
     * @throws NullPointerException If {@code object} is null.
     */
    public static <T extends AccessibleObject> @NotNull T forceAccessible(@NotNull T object) {
        Objects.requireNonNull(object, "Argument 'object'");
        try {
            METHOD_setAccessible0.invoke(object, true);
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeException(cause);
        }
        return object;
    }

    /**
     * Removes access restrictions on {@code target} by adding the module of
     * {@code accessor} to {@code target}'s exports/opens.
     * @param accessor Accessor class.
     * @param target Target class to negate access restrictions for.
     * @throws RuntimeException If the operation fails.
     * @throws NullPointerException
     *   If either {@code accessor} or {@code target} is null.
     */
    public static void allowAccess(@NotNull Class<?> accessor, @NotNull Class<?> target) {
        Objects.requireNonNull(accessor, "Argument 'accessor'");
        Objects.requireNonNull(target, "Argument 'target'");
        try {
            METHOD_implAddExportsOrOpens.invoke(target.getModule(), target.getPackageName(), accessor.getModule(), true, true);
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeException(cause);
        }
    }

    private static final class MethodKey {
        private static final Class<?>[] PARAMS_EMPTY = new Class[0];

        final String name;
        final Class<?> clazz;
        final Class<?>[] params;

        private final int hash;

        MethodKey(String name, Class<?> clazz, Class<?>[] params) {
            this.name = name;
            this.clazz = clazz;
            if (params == null || params.length == 0) {
                this.params = params = PARAMS_EMPTY;
            } else {
                this.params = params = params.clone();
            }
            int hash = 31 * name.hashCode() + clazz.hashCode();
            for (Class<?> param : params) {
                hash = 31 * hash + param.hashCode();
            }
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) return true;
            if (object == null || object.getClass() != MethodKey.class) return false;
            var other = (MethodKey) object;
            return hash == other.hash
                && name.equals(other.name)
                && clazz.equals(other.clazz)
                && Arrays.equals(params, other.params);
        }
    }

    private static final HashMap<MethodKey, MethodHandle> HANDLES = new HashMap<>(16);

    /**
     * Creates a {@link MethodHandle} by looking up a (possibly private or
     * access-restricted) method in the given class that takes the given
     * parameters.
     * @param name Method name.
     * @param clazz Method class.
     * @param params Method parameters.
     * @return
     *   Created {@link MethodHandle} or null if the method could not be found.
     * @throws RuntimeException If the operation fails.
     * @throws NullPointerException If any arguments are null.
     */
    public static @Nullable MethodHandle methodHandle(@NotNull String name, @NotNull Class<?> clazz, @NotNull Class<?>... params) {
        Objects.requireNonNull(name, "Argument 'name'");
        Objects.requireNonNull(clazz, "Argument 'clazz'");
        var key = new MethodKey(name, clazz, params);
        synchronized (HANDLES) {
            var handle = HANDLES.get(key);
            if (handle == null) {
                Method method;
                try {
                    method = forceAccessible(clazz.getDeclaredMethod(name, params));
                } catch (NoSuchMethodException ignored) {
                    return null;
                }
                try {
                    handle = LOOKUP.unreflect(method);
                } catch (IllegalAccessException cause) {
                    throw new RuntimeException(cause);
                }
                HANDLES.put(key, handle);
            }
            return handle;
        }
    }

}
