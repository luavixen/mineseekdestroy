package dev.foxgirl.mineseekdestroy.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.*

object Async {

    fun <T> run(coroutine: suspend Async.() -> T): CompletableFuture<T> =
        run(EmptyCoroutineContext, coroutine)
    fun <T> run(context: CoroutineContext, coroutine: suspend Async.() -> T): CompletableFuture<T> =
        AsyncSupport.execute(context, suspend { coroutine(this) })

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
