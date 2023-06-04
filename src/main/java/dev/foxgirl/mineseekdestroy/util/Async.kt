package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.Game
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.*

object Async {

    fun <T> go(coroutine: suspend Async.() -> T): CompletableFuture<T> =
        go(EmptyCoroutineContext, coroutine)
    fun <T> go(context: CoroutineContext, coroutine: suspend Async.() -> T): CompletableFuture<T> =
        AsyncSupport.execute(context, suspend { coroutine(this) })

    fun <T> run(coroutine: suspend Async.() -> T): Unit =
        handle(go(coroutine))
    fun <T> run(context: CoroutineContext, coroutine: suspend Async.() -> T): Unit =
        handle(go(context, coroutine))

    private fun <T> handle(promise: CompletableFuture<T>) {
        promise.handle { _, cause -> Game.LOGGER.error("Unexpected exception in async task", cause) }
    }

    suspend fun <T> await(promise: CompletableFuture<T>): T =
        suspendCoroutine {
            promise.handle { value, err ->
                it.resumeWith(if (err != null) Result.failure(err) else Result.success(value))
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
