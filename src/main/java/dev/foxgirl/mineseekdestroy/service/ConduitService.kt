package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import dev.foxgirl.mineseekdestroy.util.async.await
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import net.minecraft.block.Blocks
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageTypes
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.*
import net.minecraft.util.math.Vec3d
import kotlin.random.Random

class ConduitService : Service() {

    private enum class UnusableReason {
        SUCCESS {
            override val startingMessage: Text get() = text()
            override val stoppingMessage: Text get() = text()
        },
        PASS {
            override val startingMessage: Text get() = text()
            override val stoppingMessage: Text get() = text()
        },
        CANCELLED {
            override val startingMessage: Text get() = text("Conduit start cancelled!")
            override val stoppingMessage: Text get() = text("Conduit cancelled!")
        },
        INVALID,
        WRONG_STATE,
        WRONG_TEAM,
        NOT_ALIVE {
            override val startingMessage: Text get() = text("You can't use your conduit while you're dead!")
            override val stoppingMessage: Text get() = text("Your conduit's connection was broken because you died!")
        },
        NOT_HOLDING_CONDUIT,
        NOT_ENOUGH_FOOD {
            override val startingMessage: Text get() = text("You can't use your conduit while you're hungry!")
        };

        open val startingMessage: Text get() = text("You can't use your conduit right now!")
        open val stoppingMessage: Text get() = text("Your conduit's connection was broken!")

        val isFailure get() = this != SUCCESS && this != PASS
        val isSuccess get() = this == SUCCESS

        val isPass get() = this == PASS
        val isNotPass get() = !isPass
    }

    private inner abstract class Handler(val state: State) {
        val player get() = state.player
        val playerEntity get() = player.entity

        val ticks get() = state.ticks

        var isActive by state::isActive
        var isCancelled by state::isCancelled

        abstract val team: GameTeam

        abstract fun checkUsable(isTryingToUse: Boolean): UnusableReason

        protected fun checkNotHungry(amount: Int = 2): UnusableReason {
            val playerEntity = playerEntity ?: return UnusableReason.INVALID
            if (playerEntity.hungerManager.foodLevel < amount) {
                return UnusableReason.NOT_ENOUGH_FOOD
            }
            return UnusableReason.PASS
        }

        abstract fun activate()
        abstract fun deactivate()

        protected fun startOwnHunger(hungerPerSecond: Double) {
            Async.go {
                delay(0.5)
                while (isActive) {
                    val playerEntity = playerEntity ?: continue
                    if (playerEntity.hungerManager.foodLevel > 0) {
                        playerEntity.hungerManager.foodLevel--
                    } else {
                        playerEntity.hurtHearts(0.5) { it.starve() }
                    }
                    delay(1.0 / hungerPerSecond)
                }
            }
        }
        protected fun startOtherHunger(hungerPerSecond: Double) {
            Async.go {
                delay(0.5)
                val targets = mutableMapOf<GamePlayer, Int>()
                while (isActive) {
                    val playerEntity = playerEntity ?: continue
                    for ((target, targetEntity) in playerEntitiesIn) {
                        if (player == target) continue
                        if (player.team === target.team) continue
                        if (playerEntity.squaredDistanceTo(targetEntity) <= 6.25) {
                            if (targetEntity.hungerManager.foodLevel > 0) {
                                targetEntity.hungerManager.foodLevel--
                            }
                            val ticksLast = targets.put(target, ticks)
                            if (ticksLast == null || ticksLast < ticks - 80) {
                                targetEntity.sendInfo(text("You're being starved by ") + player.displayName + "'s conduit!")
                            }
                        }
                    }
                    delay(1.0 / hungerPerSecond)
                }
            }
        }
    }

    private inner class YellowHandler(state: State) : Handler(state) {
        private inner class Structure(val region: Region, val center: BlockPos, val fans: Region.Set)

        private val structures = enumMapOf<Direction, Structure>(
            SOUTH to Structure(Region(BlockPos(-54, -31, -697), BlockPos(-36, -20, -661)), BlockPos(-45, -29, -691), Region.Set(
                Region(BlockPos(-48, -22, -689), BlockPos(-42, -21, -683)),
                Region(BlockPos(-48, -20, -666), BlockPos(-42, -21, -672)),
            )),
            WEST to Structure(Region(BlockPos(-39, -11, -700), BlockPos(-75, 0, -682)), BlockPos(-45, -9, -691), Region.Set(
                Region(BlockPos(-47, -1, -694), BlockPos(-53, -2, -688)),
                Region(BlockPos(-64, 0, -688), BlockPos(-70, -1, -694)),
            )),
            NORTH to Structure(Region(BlockPos(-54, 20, -721), BlockPos(-36, 9, -685)), BlockPos(-45, 11, -691), Region.Set(
                Region(BlockPos(-48, 19, -699), BlockPos(-42, 18, -693)),
                Region(BlockPos(-48, 20, -710), BlockPos(-42, 19, -716)),
            )),
            EAST to Structure(Region(BlockPos(-15, 40, -700), BlockPos(-51, 29, -682)), BlockPos(-45, 31, -691), Region.Set(
                Region(BlockPos(-37, 39, -694), BlockPos(-43, 38, -688)),
                Region(BlockPos(-26, 40, -694), BlockPos(-20, 39, -688)),
            )),
        )

        private inner class SoundEntry(val sound: SoundEvent, val volume: Float, val pitch: Float, val delay: Double)

        private val scoreSoundEntries = immutableListOf(
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 0.7937005F, 0.5),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 1.4142135F, 0.0),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 0.7937005F, 0.3),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 1.3348398F, 0.0),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 0.7937005F, 0.3),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 1.3348398F, 0.0),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 0.6674199F, 0.1),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 1.1892071F, 0.0),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 0.7937005F, 0.2),
            SoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 4.0F, 1.4142135F, 0.0),
        )

        override val team get() = GameTeam.YELLOW

        override fun checkUsable(isTryingToUse: Boolean): UnusableReason {
            val playerEntity = playerEntity ?: return UnusableReason.INVALID
            if (isTryingToUse) {
                if (playerEntity.hungerManager.foodLevel < 12) {
                    return UnusableReason.NOT_ENOUGH_FOOD
                }
            } else {
                if (playerEntity.hungerManager.foodLevel > 0) {
                    return UnusableReason.SUCCESS
                }
            }
            return UnusableReason.PASS
        }

        override fun activate() {
            consolePlayers.sendInfoRaw(text() + text("Yellicopter").teamYellow() + " deployed by " + player.displayName + "!")

            val playerEntity = playerEntity!!

            fun removeHunger(playerEntity: ServerPlayerEntity) {
                playerEntity.hungerManager.foodLevel = Math.max(playerEntity.hungerManager.foodLevel - 12, 0)
            }

            for ((_, targetEntity) in playerEntitiesIn) {
                if (targetEntity == playerEntity) continue
                if (targetEntity.squaredDistanceTo(playerEntity) > 30) continue
                removeHunger(targetEntity)
                targetEntity.sendInfo(text("You've been starved by ") + player.displayName + "'s conduit!")
            }

            removeHunger(playerEntity)

            Scheduler.delay(5.0) {
                if (isActive) {
                    isCancelled = true
                }
            }

            Async.go {
                val structure = structures[playerEntity.horizontalFacing]!!

                val offset = playerEntity.blockPos.subtract(structure.center)
                val region = structure.region.offset(offset)
                val center = structure.center.add(offset)
                val fans = Region.Set(structure.fans.map { it.offset(offset) })

                val blimp = Region.Set(buildList {
                    add(properties.regionBlimp)
                    addAll(properties.regionBlimpBalloons)
                })

                Editor.queue(world, region) {
                    val world = world
                    it.edit { _, x, y, z ->
                        if (blimp.contains(x, y, z)) return@edit null
                        val state = world.getBlockState(BlockPos(x - offset.x, y - offset.y, z - offset.z))
                        if (state.block === Blocks.RED_WOOL || state.block === Blocks.CAVE_AIR) {
                            Blocks.AIR.defaultState
                        } else {
                            if (state.isAir) null else state
                        }
                    }
                }
                    .await()

                context.specialBoosterService.addYellicopterTrackers(fans)

                val (centerX, centerY, centerZ) = center.toCenterPos()
                playerEntity.requestTeleport(centerX, centerY, centerZ)

                for (i in 0 until 200) {
                    Broadcast.sendParticles(ParticleTypes.POOF, 0.25F, 5, world, region.box.random())
                }

                val soundPositionSupplier = Broadcast.PositionSupplier { _, targetEntity ->
                    var (x, y, z) = targetEntity.pos
                    x = x.coerceAtLeast(region.box.minX).coerceAtMost(region.box.maxX)
                    y = y.coerceAtLeast(region.box.minY).coerceAtMost(region.box.maxY)
                    z = z.coerceAtLeast(region.box.minZ).coerceAtMost(region.box.maxZ)
                    val pos = Vec3d(x, y, z)
                    if (pos.squaredDistanceTo(targetEntity.pos) <= 16384) pos
                    else null
                }

                for (soundEntry in scoreSoundEntries) {
                    if (soundEntry.delay > 0.0) {
                        delay(soundEntry.delay)
                    }
                    Broadcast.sendSound(soundEntry.sound, SoundCategory.RECORDS, soundEntry.volume, soundEntry.pitch, soundPositionSupplier)
                }
            }
        }

        override fun deactivate() {
        }
    }

    private inner class BlueHandler(state: State) : Handler(state) {
        override val team get() = GameTeam.BLUE

        override fun checkUsable(isTryingToUse: Boolean): UnusableReason {
            return checkNotHungry()
        }

        override fun activate() {
            startOwnHunger(3.0)
            startOtherHunger(3.0)

            Async.go {
                delay(0.5)

                val blocks = mutableMapOf<BlockPos, Pair<Long, Int>>()
                var iteration = 0L

                while (isActive) {
                    delay()
                    iteration += 1

                    val playerEntity = playerEntity ?: continue

                    blocks.values.removeIf { (blockIteration) -> blockIteration < iteration - 20 }

                    for (blockPos in playerEntity.blockPos.around(2.0)) {
                        if (blockPos in properties.regionBlimp || blockPos in properties.regionBlimpBalloons) continue
                        val blockState = world.getBlockState(blockPos)
                        if (
                            blockState.block !in properties.unstealableBlocks && !blockState.isAir &&
                            blockPos.y >= playerEntity.blockPos.y &&
                            blockPos.toCenterPos().let {
                                playerEntity.pos.add(0.0, 0.5, 0.0).squaredDistanceTo(it) <= 2.75 ||
                                playerEntity.pos.add(0.0, 1.5, 0.0).squaredDistanceTo(it) <= 2.75
                            }
                        ) {
                            val blockProgress = blocks.get(blockPos)?.second ?: 0
                            if (blockProgress >= 9) {
                                world.breakBlock(blockPos, false)
                                blocks.remove(blockPos)
                            } else {
                                world.setBlockBreakingInfo(blockPos.hashCode(), blockPos, blockProgress + 4)
                                blocks.set(blockPos, iteration to blockProgress + 4)
                            }
                        }
                    }
                }
            }
        }

        override fun deactivate() {
        }
    }

    private inner class BlackHandler(state: State) : Handler(state) {
        override val team get() = GameTeam.BLACK

        override fun checkUsable(isTryingToUse: Boolean): UnusableReason {
            return checkNotHungry()
        }

        override fun activate() {
            startOwnHunger(3.0)

            context.glowService.broadcastNow()
        }

        override fun deactivate() {
        }
    }

    private inner class DuelistHandler(state: State) : Handler(state) {
        override val team get() = GameTeam.DUELIST

        override fun checkUsable(isTryingToUse: Boolean): UnusableReason {
            val playerEntity = playerEntity ?: return UnusableReason.INVALID
            if (isTryingToUse) {
                if (playerEntity.hungerManager.foodLevel < 10) {
                    return UnusableReason.NOT_ENOUGH_FOOD
                }
            } else {
                if (playerEntity.hungerManager.foodLevel > 0) {
                    return UnusableReason.SUCCESS
                }
            }
            return UnusableReason.PASS
        }

        override fun activate() {
            val playerEntity = playerEntity!!

            playerEntity.hungerManager.foodLevel = Math.max(playerEntity.hungerManager.foodLevel - 10, 0)

            Scheduler.delay(3.0) {
                if (isActive) {
                    isCancelled = true
                }
            }

            val tasks = mutableListOf<() -> Unit>()

            val inventory = playerEntity.inventory.asList()
            for ((i, stack) in inventory.withIndex()) {
                val nbt = stack.nbt ?: continue
                val currentSet = nbt["MsdToolDuelistSet"]?.toInt() ?: continue
                val currentIndex = nbt["MsdToolDuelistIndex"]?.toInt() ?: continue
                val list = if (currentSet == 1) GameItems.toolDuelistSet2 else GameItems.toolDuelistSet1
                tasks.add { inventory[i] = list[currentIndex].copy() }
            }

            Async.go {
                for (task in tasks) {
                    delay(0.5)
                    task()
                }
            }
        }

        override fun deactivate() {
        }
    }

    private val handlerFactories = enumMapOf<GameTeam, (State) -> Handler>(
        GameTeam.YELLOW to ::YellowHandler,
        GameTeam.BLUE to ::BlueHandler,
        GameTeam.BLACK to ::BlackHandler,
        GameTeam.DUELIST to ::DuelistHandler,
    )

    private inner class State(val player: GamePlayer) {
        var isActive = false
        var isCancelled = false

        var ticks = 0

        var handler: Handler? = null
        val team: GameTeam? get() = handler?.team

        fun use(player: GamePlayer, playerEntity: ServerPlayerEntity, stack: ItemStack) {
            require(player == this.player)

            if (isActive) return

            val team = try {
                stack.nbt!!.get("MsdConduit").toEnum<GameTeam>()
            } catch (ignored: Exception) {
                return
            }

            val handlerFactory = handlerFactories[team]
            if (handlerFactory == null) {
                logger.error("Conduit exists with invalid team $team")
                return
            }

            handler = handlerFactory.invoke(this)

            val reason = checkUsable(true)
            if (reason.isFailure) {
                playerEntity.sendInfo(reason.startingMessage)
                return
            }

            isActive = true

            logger.info("Conduit activating for {}", player.nameQuoted)

            playerEntity.sendInfo("You've activated your conduit!")
            playerEntity.play(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0, 1.0)
            playerEntity.particles(ParticleTypes.FLASH)

            handler!!.activate()
        }

        fun update() {
            if (isCancelled) {
                isCancelled = false
                deactivate(UnusableReason.CANCELLED)
            } else if (isActive) {
                val reason = checkUsable(false)
                if (reason.isFailure) {
                    deactivate(reason)
                } else {
                    player.entity?.particles(ParticleTypes.ENCHANT, Random.nextDouble(0.5, 2.0), 6) { it.add(0.0, Random.nextDouble(0.5, 2.0), 0.0) }
                }
            }
            ticks++
        }

        private fun checkUsable(isTryingToUse: Boolean): UnusableReason {
            if (game.state.isWaiting) {
                return UnusableReason.WRONG_STATE
            }
            val handler = handler
            if (handler != null && player.team !== handler.team) {
                return UnusableReason.WRONG_TEAM
            }
            if (!player.isAlive) {
                return UnusableReason.NOT_ALIVE
            }
            val playerEntity = player.entity
            if (playerEntity == null) {
                return UnusableReason.INVALID
            }
            if (handler != null) {
                val reason = handler.checkUsable(isTryingToUse)
                if (reason.isNotPass) {
                    return reason
                }
            }
            if (
                playerEntity.mainHandStack.item !== Items.CONDUIT &&
                playerEntity.offHandStack.item !== Items.CONDUIT
            ) {
                return UnusableReason.NOT_HOLDING_CONDUIT
            }
            return UnusableReason.SUCCESS
        }

        private fun deactivate(reason: UnusableReason) {
            isActive = false

            logger.info("Conduit deactivating for {}", player.nameQuoted)

            handler?.deactivate()

            player.entity?.sendInfo(reason.stoppingMessage)
            player.entity?.play(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0, 1.0)
            player.entity?.particles(ParticleTypes.FLASH)
        }
    }

    private val states = mutableMapOf<GamePlayer, State>()
    private fun stateFor(player: GamePlayer) = states.computeIfAbsent(player, ::State)

    override fun update() {
        states.values.forEach(State::update)
    }

    fun isConduitActive(player: GamePlayer): Boolean {
        return stateFor(player).isActive
    }
    fun getConduitTeam(player: GamePlayer): GameTeam? {
        return stateFor(player).team
    }

    fun handleConduitUse(player: GamePlayer, playerEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        stateFor(player).use(player, playerEntity, stack)
        return ActionResult.FAIL
    }

    fun shouldIgnoreDamage(player: GamePlayer, playerEntity: ServerPlayerEntity, source: DamageSource): Boolean {
        val state = stateFor(player)
        return state.isActive && state.team === GameTeam.YELLOW && !source.isOf(DamageTypes.STARVE)
    }

    fun shouldMakeCannonPlayersGlow(player: GamePlayer): Boolean {
        val state = states[player] ?: return false
        return state.isActive && state.team === GameTeam.BLACK
    }

}
