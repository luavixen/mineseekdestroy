package dev.foxgirl.mineseekdestroy.util;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * AsyncSupport provides support for executing Kotlin coroutines without proper
 * coroutine support. Coroutines are executed eagerly whenever they are
 * resumed.
 */
public final class AsyncSupport {

    private AsyncSupport() {
        throw new UnsupportedOperationException();
    }

    private static final class CompletableContinuation<T> implements Continuation<T> {
        private final CoroutineContext context;
        private final CompletableFuture<T> future;

        private CompletableContinuation(CoroutineContext context, CompletableFuture<T> future) {
            this.context = context;
            this.future = future;
        }

        @Override
        public @NotNull CoroutineContext getContext() {
            return this.context;
        }

        @Override
        public void resumeWith(@NotNull Object result) {
            resolveFuture(this.future, result);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resolveFuture(CompletableFuture future, Object result) {
        if (result instanceof Result.Failure) {
            future.completeExceptionally(((Result.Failure) result).exception);
        } else {
            future.complete(result);
        }
    }

    private static final MethodType TYPE_INVOKE = MethodType.methodType(Object.class, Continuation.class);
    private static final MethodType TYPE_INVOKE_GENERIC = MethodType.methodType(Object.class, Object.class);

    private static <T> Object invokeInterface(Object coroutine, CompletableContinuation<T> continuation) {
        try {
            @SuppressWarnings({"unchecked", "rawtypes" })
            Function1<Continuation<T>, Object> function = (Function1) coroutine;
            return function.invoke(continuation);
        } catch (Throwable err) {
            throw new RuntimeException(err);
        }
    }

    private static <T> Object invokeReflection(Object coroutine, CompletableContinuation<T> continuation) {
        Class<?> clazz = coroutine.getClass();
        try {
            MethodHandles.Lookup handleLookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            MethodHandle handle;
            try {
                handle = handleLookup.findVirtual(clazz, "invoke", TYPE_INVOKE);
            } catch (NoSuchMethodException err0) {
                try {
                    handle = handleLookup.findVirtual(clazz, "invoke", TYPE_INVOKE_GENERIC);
                } catch (NoSuchMethodException err1) {
                    var err = new IllegalArgumentException("No such method 'invoke' for coroutine object", err1);
                    err.addSuppressed(err0);
                    throw err;
                }
            }
            return handle.invoke(coroutine, continuation);
        } catch (Throwable err) {
            throw new RuntimeException(err);
        }
    }

    /**
     * Executes the given coroutine.
     *
     * @param context Coroutine context to execute in.
     * @param coroutine
     *   Kotlin coroutine object to execute, should be of type
     *   {@code suspend () -> T}.
     * @return The result of the execution.
     * @param <T> Coroutine return type.
     * @throws RuntimeException
     *   If invoking the coroutine fails or the coroutine throws an exception.
     * @throws IllegalArgumentException
     *   If {@code coroutine} is not a valid coroutine.
     * @throws NullPointerException
     *   If either {@code context} or {@code coroutine} is null.
     */
    public static <T> @NotNull CompletableFuture<T> execute(
        @NotNull CoroutineContext context,
        @NotNull Object coroutine
    ) {
        Objects.requireNonNull(context, "Argument 'context'");
        Objects.requireNonNull(coroutine, "Argument 'coroutine'");

        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableContinuation<T> continuation = new CompletableContinuation<>(context, future);

        Object result = coroutine instanceof Function1
            ? invokeInterface(coroutine, continuation)
            : invokeReflection(coroutine, continuation);

        if (result != IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
            resolveFuture(future, result);
        }

        return future;
    }

}
