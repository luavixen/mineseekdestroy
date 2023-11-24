package dev.foxgirl.mineseekdestroy.util.async

import java.time.Instant
import kotlin.coroutines.CoroutineContext

sealed class Lifetime : CoroutineContext.Element {

    object Key : CoroutineContext.Key<Lifetime>
    override val key = Key

    abstract val isCancelled: Boolean

    override fun toString() = "Lifetime(isCancelled=$isCancelled)"

    fun withCondition(condition: () -> Boolean): Lifetime = ConditionLifetime(this, condition)

    fun withTimeout(seconds: Double): Lifetime = TimeoutLifetime(this, Instant.now().plusNanos((seconds * 1e9).toLong()))
    fun withTimeout(expiresAt: Instant): Lifetime = TimeoutLifetime(this, expiresAt)

    fun withCancel() = CancelableLifetime(this)

    fun <T> execute(name: String? = null, coroutine: suspend () -> T) = Async.execute(name, this, coroutine)
    fun <T> go(name: String? = null, coroutine: suspend Async.() -> T) = Async.go(name, this, coroutine)

    suspend fun <T> innerExecute(coroutine: suspend () -> T) = Async.innerExecute(this, coroutine)
    suspend fun <T> innerGo(coroutine: suspend Async.() -> T) = Async.innerGo(this, coroutine)

}
