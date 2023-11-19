package dev.foxgirl.mineseekdestroy.util.async

class CancelableLifetime internal constructor(private val base: Lifetime) : Lifetime() {

    override var isCancelled = false; get() = base.isCancelled || field; private set

    fun cancel() {
        isCancelled = true
    }

}
