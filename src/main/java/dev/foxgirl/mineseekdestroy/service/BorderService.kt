package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.util.Console
import net.minecraft.world.border.WorldBorder

class BorderService : Service() {

    private val border: WorldBorder get() = world.worldBorder

    private val sizeMin: Double = 8.0
    private val sizeMax: Double = 4096.0
    private val sizeCurrent get() = properties.borderSize

    override fun setup() {
        border.damagePerBlock = 0.5
        border.safeZone = 5.0
        border.warningTime = 15
        border.warningBlocks = 5
        border.size = sizeMax
        border.setCenter(properties.positionBlimp.x, properties.positionBlimp.z)
    }

    fun executeBorderStop(console: Console) {
        border.size = sizeMax
        console.sendInfo("Stopped world border")
    }

    fun executeBorderStart(console: Console) {
        border.size = sizeCurrent
        border.interpolateSize(sizeCurrent, sizeMin, (game.getRuleDouble(Game.RULE_BORDER_CLOSE_DURATION) * 1000.0).toLong())
        console.sendInfo("Started world border")
    }

}
