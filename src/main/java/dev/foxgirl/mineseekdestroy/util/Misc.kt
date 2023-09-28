package dev.foxgirl.mineseekdestroy.util

import net.minecraft.util.math.BlockPos

fun BlockPos.around(radius: Double) = around(radius, radius, radius)
fun BlockPos.around(radiusX: Double, radiusY: Double, radiusZ: Double) = sequence {
    val center = toCenterPos()
    val distance = doubleArrayOf(radiusX, radiusY, radiusZ).max().let { it * it }
    for (currentX in (x - radiusX.toInt())..(x + (radiusX + 1.0).toInt())) {
        for (currentY in (y - radiusY.toInt())..(y + (radiusY + 1.0).toInt())) {
            for (currentZ in (z - radiusZ.toInt())..(z + (radiusZ + 1.0).toInt())) {
                val pos = BlockPos(currentX, currentY, currentZ)
                if (pos.toCenterPos().squaredDistanceTo(center) <= distance) yield(pos)
            }
        }
    }
}
