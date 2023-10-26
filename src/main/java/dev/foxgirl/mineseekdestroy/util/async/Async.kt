package dev.foxgirl.mineseekdestroy.util.async

import dev.foxgirl.mineseekdestroy.Game
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import kotlin.coroutines.*

object Async {

    private class CompletableFutureContinuation<T> : CompletableFuture<T>(), Continuation<T> {
        override val context get() = EmptyCoroutineContext
        override fun resumeWith(result: Result<T>) {
            result.fold(::complete, ::completeExceptionally)
        }
    }

    fun <T> execute(coroutine: suspend () -> T): CompletableFuture<T> =
        CompletableFutureContinuation<T>().also { coroutine.startCoroutine(it) }
    fun <T> go(coroutine: suspend Async.() -> T): Unit =
        CompletableFutureContinuation<T>().also { coroutine.startCoroutine(this, it) }.terminate()

    suspend fun <T> await(promise: CompletableFuture<T>): T =
        suspendCoroutine { promise.whenComplete { value, cause -> it.resumeWith(if (cause != null) Result.failure(cause) else Result.success(value)) } }

    suspend fun <T> awaitAny(vararg promises: CompletableFuture<T>): T = awaitAnyImpl(promises)
    suspend fun <T> awaitAny(promises: Collection<CompletableFuture<T>>): T = awaitAnyImpl(promises.toTypedArray())

    suspend fun <T> awaitAll(vararg promises: CompletableFuture<T>): List<T> = awaitAllImpl(promises)
    suspend fun <T> awaitAll(promises: Collection<CompletableFuture<T>>): List<T> = awaitAllImpl(promises.toTypedArray())

    suspend fun <T> awaitAllSettled(vararg promises: CompletableFuture<T>): List<Result<T>> = awaitAllSettledImpl(promises)
    suspend fun <T> awaitAllSettled(promises: Collection<CompletableFuture<T>>): List<Result<T>> = awaitAllSettledImpl(promises.toTypedArray())

    private suspend fun <T> awaitAnyImpl(promises: Array<out CompletableFuture<T>>): T {
        return suspendCoroutine {
            val done = AtomicBoolean()
            promises.forEach { promise ->
                promise.whenComplete { value, cause ->
                    if (done.getAndSet(true)) return@whenComplete
                    it.resumeWith(if (cause != null) Result.failure(cause) else Result.success(value))
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
        return suspendCoroutine { Waiter(promises, it).start() }
    }

    private class Waiter<T>(
        private val promises: Array<out CompletableFuture<T>>,
        private val continuation: Continuation<List<Result<T>>>,
    ) {
        @JvmField val results = arrayOfNulls<Result<T>>(promises.size)
        @JvmField var count = 0

        private fun resume() {
            @Suppress("UNCHECKED_CAST")
            continuation.resumeWith(Result.success(results.asList() as List<Result<T>>))
        }

        private inner class Handler(private val i: Int) : BiConsumer<T, Throwable?> {
            override fun accept(value: T, cause: Throwable?) {
                val result = if (cause != null) Result.failure(cause) else Result.success(value)
                if (synchronized(this@Waiter) { results[i] = result; results.size == ++count }) resume()
            }
        }

        fun start() {
            if (promises.isNotEmpty()) {
                promises.forEachIndexed { i, promise -> promise.whenComplete(Handler(i)) }
            } else {
                resume()
            }
        }
    }

    private class ResumingCallback(private val continuation: Continuation<Unit>) : Scheduler.Callback {
        override fun invoke(schedule: Scheduler.Schedule) = continuation.resume(Unit)
    }

    suspend fun delay(): Unit =
        suspendCoroutine { Scheduler.now(ResumingCallback(it)) }
    suspend fun delay(seconds: Double): Unit =
        suspendCoroutine { Scheduler.delay(seconds, ResumingCallback(it)) }

    suspend fun until(condition: suspend () -> Boolean) {
        while (!condition()) delay()
    }
    suspend fun until(seconds: Double, condition: suspend () -> Boolean) {
        while (!condition()) delay(seconds)
    }

}

suspend inline fun <T> CompletableFuture<T>.await(): T = Async.await(this)

fun <T> CompletableFuture<T>.terminate() {
    whenComplete { _, cause -> if (cause != null) Game.LOGGER.error("Unhandled exception in async task", cause) }
}
