package dev.foxgirl.mineseekdestroy.util.async

import dev.foxgirl.mineseekdestroy.Game
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import kotlin.coroutines.*

object Async {

    private val activeContexts = mutableSetOf<CoroutineContext>()
    private fun onCoroutineOpen(context: CoroutineContext) {
        synchronized(activeContexts) {
            activeContexts.add(context)
        }
    }
    private fun onCoroutineClose(context: CoroutineContext) {
        synchronized(activeContexts) {
            activeContexts.remove(context)
        }
    }

    private class CompletableFutureContinuation<T>(override val context: CoroutineContext) : CompletableFuture<T>(), Continuation<T> {
        init {
            whenComplete { _, _ -> onCoroutineClose(context) }
        }
        fun startCoroutine(coroutine: suspend () -> T): CompletableFutureContinuation<T> {
            onCoroutineOpen(context)
            coroutine.startCoroutine(this)
            return this
        }
        fun <R> startCoroutine(coroutine: suspend R.() -> T, receiver: R): CompletableFutureContinuation<T> {
            onCoroutineOpen(context)
            coroutine.startCoroutine(receiver, this)
            return this
        }
        override fun resumeWith(result: Result<T>) {
            result.fold(::complete, ::completeExceptionally)
        }
    }

    private fun <T> Continuation<T>.resumeChecked(value: T) {
        resumeWithChecked(Result.success(value))
    }
    private fun <T> Continuation<T>.resumeWithChecked(result: Result<T>) {
        if (isCancelled) {
            resumeWithException(CanceledException(this.context))
        } else {
            resumeWith(result)
        }
    }

    private fun createDescriptor(name: String?): Descriptor {
        return if (name != null) Descriptor(name) else anonymousDescriptor
    }
    private fun createContext(name: String?, lifetime: Lifetime): CoroutineContext {
        return AsyncCoroutineContext(createDescriptor(name), lifetime)
    }

    fun background(): Lifetime = BackgroundLifetime

    fun <T> execute(coroutine: suspend () -> T) = execute(null, background(), coroutine)
    fun <T> execute(name: String?, coroutine: suspend () -> T) = execute(name, background(), coroutine)
    fun <T> execute(name: String?, lifetime: Lifetime, coroutine: suspend () -> T): CompletableFuture<T> {
        return execute(createContext(name, lifetime), coroutine)
    }
    private fun <T> execute(context: CoroutineContext, coroutine: suspend () -> T): CompletableFuture<T> {
        return if (context.isCancelled) {
            CompletableFuture.failedFuture(CanceledException(context))
        } else {
            CompletableFutureContinuation<T>(context).startCoroutine(coroutine)
        }
    }

    fun <T> go(coroutine: suspend Async.() -> T) = go(null, background(), coroutine)
    fun <T> go(name: String?, coroutine: suspend Async.() -> T) = go(name, background(), coroutine)
    fun <T> go(name: String?, lifetime: Lifetime, coroutine: suspend Async.() -> T) {
        go(createContext(name, lifetime), coroutine)
    }
    private fun <T> go(context: CoroutineContext, coroutine: suspend Async.() -> T) {
        if (context.isCancelled) {
            Game.LOGGER.warn("Lifetime already cancelled, failed to start async task '${context.name}'")
        } else {
            CompletableFutureContinuation<T>(context).startCoroutine(coroutine, this).terminate()
        }
    }

    suspend fun <T> innerExecute(coroutine: suspend () -> T) = innerExecute(lifetime(), coroutine)
    suspend fun <T> innerExecute(lifetime: Lifetime, coroutine: suspend () -> T): CompletableFuture<T> {
        return execute(AsyncCoroutineContext(coroutineContext.descriptor, lifetime), coroutine)
    }

    suspend fun <T> innerGo(coroutine: suspend Async.() -> T) = innerGo(lifetime(), coroutine)
    suspend fun <T> innerGo(lifetime: Lifetime, coroutine: suspend Async.() -> T) {
        go(AsyncCoroutineContext(coroutineContext.descriptor, lifetime), coroutine)
    }

    private fun getCancellation(throwable: Throwable?): CanceledException? = getCancellation(throwable, HashSet())
    private fun getCancellation(throwable: Throwable?, visited: MutableSet<Throwable>): CanceledException? {
        if (throwable != null && throwable !in visited) {
            visited.add(throwable)
            if (throwable is CanceledException) return throwable
            val causeCancellation = getCancellation(throwable.cause, visited)
            if (causeCancellation != null) return causeCancellation
            for (suppressedError in throwable.suppressed) {
                val suppressedCancellation = getCancellation(suppressedError, visited)
                if (suppressedCancellation != null) return suppressedCancellation
            }
        }
        return null
    }

    @JvmStatic
    fun printThrowable(throwable: Throwable?) {
        if (throwable == null) return
        if (throwable is Error) {
            Game.LOGGER.error("Unhandled async error", throwable)
            return
        }
        if (throwable is CanceledException) {
            Game.LOGGER.debug("Async task '${throwable.context.name}' cancelled")
            return
        }
        val cancellation = getCancellation(throwable)
        if (cancellation != null) {
            Game.LOGGER.warn("Unhandled async exception, probably due to cancellation of task '${cancellation.context.name}'", throwable)
        } else {
            Game.LOGGER.error("Unhandled async exception", throwable)
        }
    }

    suspend fun lifetime(): Lifetime = coroutineContext.lifetime

    suspend fun <T> await(promise: CompletableFuture<T>): T = awaitSettled(promise).getOrThrow()
    suspend fun <T> awaitSettled(promise: CompletableFuture<T>): Result<T> {
        return suspendCoroutine {
            promise.whenComplete { value, cause ->
                it.resumeChecked(if (cause != null) Result.failure(cause) else Result.success(value))
            }
        }
    }

    suspend fun awaitCancelled(promise: CompletableFuture<*>) {
        try {
            promise.await()
            throw IllegalStateException("Expected promise to complete exceptionally")
        } catch (error: CanceledException) {
            return
        }
    }

    suspend fun <T> awaitAny(vararg promises: CompletableFuture<T>): T = awaitAnyImpl(promises)
    suspend fun <T> awaitAny(promises: Collection<CompletableFuture<T>>): T = awaitAnyImpl(promises.toTypedArray())

    suspend fun <T> awaitAll(vararg promises: CompletableFuture<T>): List<T> = awaitAllImpl(promises)
    suspend fun <T> awaitAll(promises: Collection<CompletableFuture<T>>): List<T> = awaitAllImpl(promises.toTypedArray())

    suspend fun <T> awaitAllSettled(vararg promises: CompletableFuture<T>): List<Result<T>> = awaitAllSettledImpl(promises)
    suspend fun <T> awaitAllSettled(promises: Collection<CompletableFuture<T>>): List<Result<T>> = awaitAllSettledImpl(promises.toTypedArray())

    private suspend fun <T> awaitAnyImpl(promises: Array<out CompletableFuture<T>>): T {
        return suspendCoroutine {
            val waiting = AtomicBoolean(true)
            promises.forEach { promise ->
                promise.whenComplete { value, cause ->
                    if (waiting.getAndSet(false)) {
                        it.resumeWithChecked(if (cause != null) Result.failure(cause) else Result.success(value))
                    } else if (cause != null) {
                        printThrowable(cause)
                    }
                }
            }
        }
    }

    private suspend fun <T> awaitAllImpl(promises: Array<out CompletableFuture<T>>): List<T> {
        val results = awaitAllSettledImpl(promises)
        val values = ArrayList<T>(results.size)

        val iterator = results.iterator()
        while (iterator.hasNext()) {
            val value = iterator.next().getOrElse { causeFirst ->
                throw if (iterator.hasNext()) {
                    RuntimeException(causeFirst).also {
                        do {
                            val causeNext = iterator.next().exceptionOrNull()
                            if (causeNext != null) it.addSuppressed(causeNext)
                        } while (iterator.hasNext())
                    }
                } else {
                    causeFirst
                }
            }
            values.add(value)
        }

        return values
    }

    private suspend fun <T> awaitAllSettledImpl(promises: Array<out CompletableFuture<T>>): List<Result<T>> {
        return suspendCoroutine { Waiter(it, promises).start() }
    }

    private class Waiter<T>(
        private val continuation: Continuation<List<Result<T>>>,
        private val promises: Array<out CompletableFuture<T>>,
    ) {
        private val results = arrayOfNulls<Result<T>>(promises.size)
        private var count = 0

        private inner class Callback(private val i: Int) : BiConsumer<T, Throwable?> {
            override fun accept(value: T, cause: Throwable?) {
                val result = if (cause != null) Result.failure(cause) else Result.success(value)
                if (synchronized(this@Waiter) { results[i] = result; results.size == ++count }) resume()
            }
        }

        private fun resume() {
            @Suppress("UNCHECKED_CAST")
            continuation.resumeChecked(results.asList() as List<Result<T>>)
        }

        fun start() {
            if (promises.isNotEmpty()) {
                promises.forEachIndexed { i, promise -> promise.whenComplete(Callback(i)) }
            } else {
                resume()
            }
        }
    }

    private class ResumingCallback(private val continuation: Continuation<Unit>) : Scheduler.Callback {
        override fun invoke(schedule: Scheduler.Schedule) {
            continuation.resumeChecked(Unit)
        }
    }

    suspend fun delay() {
        suspendCoroutine { Scheduler.now(ResumingCallback(it)) }
    }
    suspend fun delay(seconds: Double) {
        suspendCoroutine { Scheduler.delay(seconds, ResumingCallback(it)) }
    }

    suspend fun until(condition: suspend () -> Boolean) {
        while (!condition()) delay()
    }
    suspend fun until(seconds: Double, condition: suspend () -> Boolean) {
        while (!condition()) delay(seconds)
    }

}

internal class AsyncCoroutineContext(
    private val descriptor: Descriptor,
    private val lifetime: Lifetime,
) : CoroutineContext {
    @Suppress("UNCHECKED_CAST")
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        if (key == Descriptor.Key) return descriptor as E
        if (key == Lifetime.Key) return lifetime as E
        return null
    }

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        if (key == Descriptor.Key) return lifetime
        if (key == Lifetime.Key) return descriptor
        return this
    }

    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R
        = operation(operation(initial, descriptor), lifetime)

    override fun toString() = "AsyncCoroutineContext(descriptor=$descriptor, lifetime=$lifetime)"
}

private val anonymousDescriptor = Descriptor("anonymous")
private val unknownDescriptor = Descriptor("unknown")

val CoroutineContext.lifetime get() = get(Lifetime.Key) ?: BackgroundLifetime
val CoroutineContext.isCancelled get() = lifetime.isCancelled
val Continuation<*>.isCancelled get() = context.isCancelled

val CoroutineContext.descriptor get() = get(Descriptor.Key) ?: unknownDescriptor
val CoroutineContext.name get() = descriptor.name
val Continuation<*>.name get() = context.name

fun <T> CompletableFuture<T>.terminate() {
    whenComplete { _, error -> Async.printThrowable(error) }
}

suspend inline fun <T> CompletableFuture<T>.await(): T = Async.await(this)
suspend inline fun <T> CompletableFuture<T>.awaitSettled(): Result<T> = Async.awaitSettled(this)

suspend inline fun CompletableFuture<*>.awaitCancelled() = Async.awaitCancelled(this)
