package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.util.Console
import dev.foxgirl.mineseekdestroy.util.Rules
import net.minecraft.world.border.WorldBorder

class StormService : Service() {

    private val border: WorldBorder get() = world.worldBorder

    private val sizeMin: Double = 10.0
    private val sizeMax: Double = WorldBorder.STATIC_AREA_SIZE
    private val sizeCurrent get() = properties.borderSize

    private val center get() = properties.borderCenter

    private val time get() = (Rules.borderCloseDuration * 1000.0).toLong()

    override fun setup() {
        border.damagePerBlock = 0.5
        border.safeZone = 5.0
        border.warningTime = 15
        border.warningBlocks = 5
        border.size = sizeMax
        border.setCenter(center.x, center.z)
    }

    fun executeStormClear(console: Console) {
        border.size = sizeMax
        console.sendInfo("Cleared the storm")
    }

    fun executeStormStop(console: Console) {
        border.size = border.size
        console.sendInfo("Stopped the storm")
    }

    fun executeStormStart(console: Console) {
        border.size = sizeCurrent
        border.interpolateSize(sizeCurrent, sizeMin, time)
        console.sendInfo("Started the storm")
    }

}
