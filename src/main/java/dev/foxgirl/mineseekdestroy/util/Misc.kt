package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import java.util.*

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

val ServerPlayerEntity.player: GamePlayer?
    get() = Game.getGame().context?.getPlayer(this)

val DamageSource.player: GamePlayer?
    get() = (source as? ServerPlayerEntity)?.player ?: (attacker as? ServerPlayerEntity)?.player

inline fun lazyString(crossinline block: () -> Any?): Any {
    return object {
        override fun toString() = block().toString()
    }
}

fun nilUUID(): UUID = Util.NIL_UUID
val UUID.isNil get() = this == nilUUID()
