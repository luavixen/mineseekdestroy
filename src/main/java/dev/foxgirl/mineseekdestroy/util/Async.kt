package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.Game
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import kotlin.coroutines.*

object Async {

    fun <T> go(coroutine: suspend () -> T): CompletableFuture<T> =
        AsyncSupport.execute(EmptyCoroutineContext, coroutine)
    fun <T> go(context: CoroutineContext, coroutine: suspend () -> T): CompletableFuture<T> =
        AsyncSupport.execute(context, coroutine)

    fun <T> run(coroutine: suspend Async.() -> T): Unit =
        terminate(go(suspend { coroutine(this) }))
    fun <T> run(context: CoroutineContext, coroutine: suspend Async.() -> T): Unit =
        terminate(go(context, suspend { coroutine(this) }))

    internal fun <T> terminate(promise: CompletableFuture<T>) {
        promise.whenComplete { _, cause -> if (cause != null) Game.LOGGER.error("Unhandled exception in async task", cause) }
    }

    suspend fun <T> await(promise: CompletableFuture<T>): T =
        suspendCoroutine { promise.whenComplete { value, cause -> it.resumeWith(if (cause != null) Result.failure(cause) else Result.success(value)) } }

    suspend fun <T> await(vararg promises: CompletableFuture<T>): List<T> = awaitInternal(promises)
    suspend fun <T> await(promises: Collection<CompletableFuture<T>>): List<T> = awaitInternal(promises.toTypedArray())

    suspend fun <T> awaitResults(vararg promises: CompletableFuture<T>): List<Result<T>> = awaitInternalResults(promises)
    suspend fun <T> awaitResults(promises: Collection<CompletableFuture<T>>): List<Result<T>> = awaitInternalResults(promises.toTypedArray())

    private suspend fun <T> awaitInternal(promises: Array<out CompletableFuture<T>>): List<T> {
        val results = suspendCoroutine { Waiter(promises, it).start() }
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

    private suspend fun <T> awaitInternalResults(promises: Array<out CompletableFuture<T>>) =
        suspendCoroutine { Waiter(promises, it).start() }.asList()

    private class Waiter<T>(
        private val promises: Array<out CompletableFuture<T>>,
        private val continuation: Continuation<Array<Result<T>>>,
    ) {
        @JvmField val results = arrayOfNulls<Result<T>>(promises.size)
        @JvmField var count = 0

        private inner class Handler(private val i: Int) : BiConsumer<T, Throwable?> {
            override fun accept(value: T, cause: Throwable?) {
                val result = if (cause != null) Result.failure(cause) else Result.success(value)
                if (synchronized(this@Waiter) { results[i] = result; results.size == ++count }) {
                    continuation.resumeWith(Result.success(results as Array<Result<T>>))
                }
            }
        }

        fun start() {
            if (promises.isNotEmpty()) {
                promises.forEachIndexed { i, promise -> promise.whenComplete(Handler(i)) }
            } else {
                continuation.resumeWith(Result.success(emptyArray()))
            }
        }
    }

    suspend fun delay(): Unit =
        suspendCoroutine { Scheduler.now { _ -> it.resume(Unit) } }
    suspend fun delay(seconds: Double): Unit =
        suspendCoroutine { Scheduler.delay(seconds) { _ -> it.resume(Unit) } }

    suspend fun thread(executor: Executor): Unit =
        suspendCoroutine { executor.execute { it.resume(Unit) } }

    suspend fun until(condition: suspend () -> Boolean) = until(0.0, condition)
    suspend fun until(seconds: Double, condition: suspend () -> Boolean) {
        while (!condition()) delay(seconds)
    }

}

suspend inline fun <T> CompletableFuture<T>.await(): T = Async.await(this)

fun <T> CompletableFuture<T>.terminate() = Async.terminate(this)
