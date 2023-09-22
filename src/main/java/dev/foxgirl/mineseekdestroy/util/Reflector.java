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
import java.util.function.Function;

/**
 * Reflector provides various tools for manipulating and side-stepping the
 * restrictions of the JVM, specifically OpenJDK.
 */
public final class Reflector {

    private Reflector() {
    }

    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup LOOKUP;

    private static final MethodHandle HANDLE_setAccessible0;
    private static final MethodHandle HANDLE_implAddExportsOrOpens;

    static {
        try {
            var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);

            UNSAFE = (Unsafe) unsafeField.get(null);

            var moduleField = Class.class.getDeclaredField("module");
            var moduleFieldOffset = UNSAFE.objectFieldOffset(moduleField);
            UNSAFE.putObject(Reflector.class, moduleFieldOffset, Object.class.getModule()); // We're the java.lang module now :3

            var setAccessible0Method = AccessibleObject.class.getDeclaredMethod("setAccessible0", boolean.class);
            setAccessible0Method.setAccessible(true);

            var implAddExportsOrOpensMethod = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
            setAccessible0Method.invoke(implAddExportsOrOpensMethod, true);

            var lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            setAccessible0Method.invoke(lookupField, true);

            LOOKUP = (MethodHandles.Lookup) lookupField.get(null);

            HANDLE_setAccessible0 = LOOKUP.unreflect(setAccessible0Method);
            HANDLE_implAddExportsOrOpens = LOOKUP.unreflect(implAddExportsOrOpensMethod);
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
            HANDLE_setAccessible0.invoke(object, true);
        } catch (Throwable cause) {
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
            HANDLE_implAddExportsOrOpens.invoke(
                target.getModule(),
                target.getPackageName(),
                accessor.getModule(),
                true, true
            );
        } catch (Throwable cause) {
            throw new RuntimeException(cause);
        }
    }

    private static final class MethodKey {
        private static final Class<?>[] PARAMS_EMPTY = new Class[0];

        private final Class<?> clazz;
        private final String name;

        private Class<?>[] params;

        private final int hash;

        private MethodKey(Class<?> clazz, String name, Class<?>[] params) {
            int hash = 31 * clazz.hashCode() + name.hashCode();
            if (params == null || params.length == 0) {
                params = PARAMS_EMPTY;
            } else {
                for (Class<?> param : params) {
                    hash = 31 * hash + param.hashCode();
                }
            }
            this.clazz = clazz;
            this.name = name;
            this.params = params;
            this.hash = hash;
        }

        private void trust() {
            if (params != PARAMS_EMPTY) {
                params = params.clone();
            }
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
                && clazz == other.clazz
                && name.equals(other.name)
                && Arrays.equals(params, other.params);
        }
    }

    private static final class MethodFinder implements Function<MethodKey, MethodHandle> {
        private static final MethodFinder INSTANCE = new MethodFinder();

        private MethodFinder() {
        }

        @Override
        public MethodHandle apply(MethodKey key) {
            key.trust();
            Method method;
            try {
                method = key.clazz.getMethod(key.name, key.params);
            } catch (NoSuchMethodException ignored1) {
                try {
                    method = key.clazz.getDeclaredMethod(key.name, key.params);
                } catch (NoSuchMethodException ignored2) {
                    return null;
                }
            }
            try {
                return LOOKUP.unreflect(forceAccessible(method));
            } catch (IllegalAccessException cause) {
                throw new RuntimeException(cause);
            }
        }
    }

    private static final HashMap<MethodKey, MethodHandle> HANDLES = new HashMap<>(16);

    /**
     * Creates a {@link MethodHandle} by looking up a (possibly private or
     * access-restricted) method in the given class that takes the given
     * parameters.
     * @param clazz Method class.
     * @param name Method name.
     * @param params Method parameters.
     * @return
     *   Created {@link MethodHandle} or null if the method could not be found.
     * @throws RuntimeException If the operation fails.
     * @throws NullPointerException If any arguments are null.
     */
    public static @Nullable MethodHandle methodHandle(@NotNull Class<?> clazz, @NotNull String name, @NotNull Class<?>... params) {
        var key = new MethodKey(clazz, name, params);
        synchronized (HANDLES) {
            return HANDLES.computeIfAbsent(key, MethodFinder.INSTANCE);
        }
    }

    /**
     * Creates a {@link MethodHandle} by looking up a (possibly private or
     * access-restricted) method in the given class that takes no parameters.
     * @param clazz Method class.
     * @param name Method name.
     * @return
     *   Created {@link MethodHandle} or null if the method could not be found.
     * @throws RuntimeException If the operation fails.
     * @throws NullPointerException If any arguments are null.
     */
    public static @Nullable MethodHandle methodHandle(@NotNull Class<?> clazz, @NotNull String name) {
        return methodHandle(clazz, name, (Class<?>[]) null);
    }

}
