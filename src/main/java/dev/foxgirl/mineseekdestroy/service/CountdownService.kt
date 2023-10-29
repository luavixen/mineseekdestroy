package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos

class CountdownService : Service() {

    private fun ticksFor(iteration: Int): Int {
        val seconds = when (iteration) {
            0 -> 360 // 6 minutes
            1 -> 180 // 3 minutes
            2 -> 90 // 1.5 minutes
            3 -> 45 // .75 minutes
            4 -> 30 // .50 minutes
            5 -> 15 // .25 minutes
            6 -> 10 // 1/6 minutes
            else -> 5 // 1/12 minutes (repeating)
        }
        return seconds * 20
    }

    private fun secondsFor(ticks: Int): Int {
        return Math.round(ticks.toDouble() / 20.0).toInt()
    }

    private var running = false

    private var iteration = 0
    private var ticks = 0

    private fun start(iteration: Int) {
        running = true
        this.iteration = iteration
        this.ticks = ticksFor(iteration)
        logger.info("Countdown started")
    }

    private fun isOpaque(pos: BlockPos) = world.getBlockState(pos).let { it.isOpaque || it.isSolid }
    private fun isProtected(playerEntity: ServerPlayerEntity): Boolean {
        val posPlayer = playerEntity.blockPos.mutableCopy().apply { y++ }
        val posBlimp = properties.regionBlimp.start
        while (posPlayer.y < posBlimp.y) {
            if (isOpaque(posPlayer)) return false
            posPlayer.y++
        }
        return isOpaque(posPlayer.setY(posBlimp.y)) || isOpaque(posPlayer.setY(posBlimp.y + 1))
    }

    override fun update() {
        if (!Rules.countdownEnabled || !running) {
            running = false
            iteration = 0
            ticks = 0
            return
        }

        var seconds = 0

        if (ticks == 0) {
            logger.info("Countdown iteration $iteration finished, snipping")
            ticks = ticksFor(++iteration)
            Broadcast.send(TitleFadeS2CPacket(
                Rules.countdownTextFadeinDuration,
                Rules.countdownTextStayDuration,
                Rules.countdownTextFadeoutDuration,
            ))
            Broadcast.send(TitleS2CPacket(text("-1 ❤").red()))
            Broadcast.send(SubtitleS2CPacket(text()))
            Broadcast.sendSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.0F, 1.0F)
            Broadcast.sendParticles(ParticleTypes.ELDER_GUARDIAN, 0.0F, 0) { player, playerEntity ->
                if (player.isPlaying && player.isAlive) playerEntity.pos else null
            }
            for ((player, playerEntity) in playerEntitiesNormal) {
                if (!player.isPlayingOrGhost || !player.isAlive) continue
                if (isProtected(playerEntity)) continue
                playerEntity.hurtHearts(1.0) { it.create(Game.DAMAGE_TYPE_BITTEN) }
            }
        } else {
            seconds = secondsFor(ticks--)
        }

        if (ticks % 5 == 0) {
            Broadcast.send(OverlayMessageS2CPacket(text("${seconds}s until snip").red()))
        }
    }

    fun executeStart(console: Console, iteration: Int = 0) {
        if (!Rules.countdownEnabled) {
            console.sendError("Countdown is disabled")
            return
        }
        console.sendInfo("Countdown ${if (running) "(re)" else ""}starting with iteration $iteration")
        start(iteration)
    }
    fun executeStop(console: Console) {
        if (!running) {
            console.sendError("Countdown not running")
            return
        }
        console.sendInfo("Countdown stopping")
        running = false
    }

    fun executeSetEnabled(console: Console, enabled: Boolean) {
        Rules.countdownEnabled = enabled
        if (enabled) {
            console.sendInfo("Countdown enabled")
        } else {
            console.sendInfo("Countdown disabled")
        }
    }
    fun executeSetAutostart(console: Console, enabled: Boolean) {
        Rules.countdownAutostartEnabled = enabled
        if (enabled) {
            console.sendInfo("Countdown autostart enabled")
        } else {
            console.sendInfo("Countdown autostart disabled")
        }
    }

    fun handleRoundStart() {
        if (Rules.countdownEnabled && Rules.countdownAutostartEnabled) {
            start(0)
            Game.CONSOLE_OPERATORS.sendInfo("Countdown automatically started")
        }
    }
    fun handleRoundEnd() {
        running = false
    }

}