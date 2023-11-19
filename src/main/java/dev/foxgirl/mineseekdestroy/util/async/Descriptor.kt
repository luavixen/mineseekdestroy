package dev.foxgirl.mineseekdestroy.util.async

import kotlin.coroutines.CoroutineContext

class Descriptor internal constructor(val name: String) : CoroutineContext.Element {

    object Key : CoroutineContext.Key<Descriptor>
    override val key = Key

    override fun toString() = "Descriptor(name='$name')"

}
