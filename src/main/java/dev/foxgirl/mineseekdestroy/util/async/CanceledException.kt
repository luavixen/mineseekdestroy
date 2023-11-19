package dev.foxgirl.mineseekdestroy.util.async

import kotlin.coroutines.CoroutineContext

class CanceledException internal constructor(val context: CoroutineContext)
    : RuntimeException("Lifetime cancelled for async task '${context.name}'")
