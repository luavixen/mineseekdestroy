package dev.foxgirl.mineseekdestroy.util

class Event<T> {

    inner class Subscription internal constructor(val callback: (T) -> Unit) {
        init {
            synchronized(subscriptions) {
                subscriptions.add(this)
            }
        }
        fun unsubscribe(): Boolean {
            synchronized(subscriptions) {
                return subscriptions.remove(this)
            }
        }
    }

    private val subscriptions = mutableListOf<Subscription>()

    fun subscribe(callback: (T) -> Unit) = Subscription(callback)

    fun publish(value: T) {
        subscriptions.toTypedArray().forEach { it.callback(value) }
    }

}
