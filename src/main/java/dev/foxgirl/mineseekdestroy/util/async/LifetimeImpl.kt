package dev.foxgirl.mineseekdestroy.util.async

import java.time.Instant

internal object BackgroundLifetime : Lifetime() {
    override val isCancelled get() = false
}

internal class ConditionLifetime(
    private val base: Lifetime,
    private val condition: () -> Boolean,
) : Lifetime() {
    override val isCancelled get() = base.isCancelled || condition()
}

internal class TimeoutLifetime(
    private val base: Lifetime,
    private val expiresAt: Instant,
) : Lifetime() {
    override val isCancelled get() = base.isCancelled || Instant.now() >= expiresAt
}
