package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageTypes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import kotlin.random.Random

class ConduitService : Service() {

    private enum class Team {
        YELLOW {
            override val value get() = GameTeam.PLAYER_YELLOW
        },
        BLUE {
            override val value get() = GameTeam.PLAYER_BLUE
        };

        abstract val value: GameTeam
    }

    private enum class UnusableReason {
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
    }

    private inner class State(val player: GamePlayer) {
        var isActive: Boolean = false
        var team: Team = Team.YELLOW

        var ticks = 0

        private fun startOwnHunger(hungerPerSecond: Double) {
            Async.go {
                while (isActive) {
                    delay(1.0 / hungerPerSecond)
                    val playerEntity = player.entity ?: continue
                    if (playerEntity.hungerManager.foodLevel > 0) {
                        playerEntity.hungerManager.foodLevel--
                    }
                }
            }
        }
        private fun startOtherHunger(hungerPerSecond: Double) {
            Async.go {
                val targets = mutableMapOf<GamePlayer, Int>()
                while (isActive) {
                    delay(1.0 / hungerPerSecond)
                    val playerEntity = player.entity ?: continue
                    for ((target, targetEntity) in playerEntitiesIn) {
                        if (player == target) continue
                        if (player.team === target.team) continue
                        if (playerEntity.squaredDistanceTo(targetEntity) <= 2.5) {
                            if (targetEntity.hungerManager.foodLevel > 0) {
                                targetEntity.hungerManager.foodLevel--
                            }
                            val ticksLast = targets.put(target, ticks)
                            if (ticksLast == null || ticksLast < ticks - 100) {
                                targetEntity.sendMessage(Console.formatInfo(text("You're being starved by ") + player.displayName + "!"))
                            }
                        }
                    }
                }
            }
        }

        private fun startYellow() {
            startOwnHunger(2.0)
            startOtherHunger(3.0)
            Async.go {
                while (isActive) {
                    delay(0.5)
                    val playerEntity = player.entity ?: continue
                    playerEntity.addEffect(StatusEffects.WEAKNESS, 15.75, 64)
                    playerEntity.addEffect(StatusEffects.MINING_FATIGUE, 15.75, 64)
                }
            }
        }

        private fun startBlue() {
            startOwnHunger(3.0)
            startOtherHunger(3.0)
            Async.go {
                val blocks = mutableMapOf<BlockPos, Pair<Long, Int>>()
                var iteration = 0L

                while (isActive) {
                    delay()
                    iteration += 1

                    val playerEntity = player.entity ?: continue

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

        private fun checkUnusable(isTryingToUse: Boolean): UnusableReason? {
            if (game.state.isWaiting) {
                return UnusableReason.WRONG_STATE
            }
            if (player.team !== team.value) {
                return UnusableReason.WRONG_TEAM
            }
            if (!player.isAlive) {
                return UnusableReason.NOT_ALIVE
            }
            val playerEntity = player.entity
            if (playerEntity == null) {
                return UnusableReason.INVALID
            }
            if (isTryingToUse) {
                if (team == Team.YELLOW && playerEntity.hungerManager.foodLevel < 17) {
                    return UnusableReason.NOT_ENOUGH_FOOD
                }
            } else {
                if (team == Team.YELLOW && playerEntity.hungerManager.foodLevel > 0) {
                    return null
                }
            }
            if (
                playerEntity.mainHandStack.item !== Items.CONDUIT &&
                playerEntity.offHandStack.item !== Items.CONDUIT
            ) {
                return UnusableReason.NOT_HOLDING_CONDUIT
            }
            return null
        }

        private fun deactivate(unusableReason: UnusableReason) {
            isActive = false

            logger.info("Conduit deactivating for '{}' because ''", player.name, lazyString { unusableReason.stoppingMessage.string })

            val playerEntity = player.entity
            if (playerEntity != null) {
                playerEntity.sendMessage(Console.formatInfo(unusableReason.stoppingMessage))
                playerEntity.play(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0, 1.0)
                if (team == Team.YELLOW) {
                    playerEntity.removeEffect(StatusEffects.WEAKNESS)
                    playerEntity.removeEffect(StatusEffects.MINING_FATIGUE)
                }
            }
        }

        fun update() {
            if (isActive) {
                val unusableReason = checkUnusable(false)
                if (unusableReason != null) {
                    deactivate(unusableReason)
                } else {
                    player.entity?.particles(ParticleTypes.ENCHANT, Random.nextDouble(0.5, 2.0), 2)
                }
            }
            ticks++
        }

        fun use(player: GamePlayer, playerEntity: ServerPlayerEntity, stack: ItemStack) {
            require(player == this.player)

            if (isActive) return

            team = try {
                when (stack.nbt!!.get("MsdConduit").toEnum<GameTeam>()) {
                    GameTeam.PLAYER_YELLOW -> Team.YELLOW
                    GameTeam.PLAYER_BLUE -> Team.BLUE
                    else -> return
                }
            } catch (ignored: Exception) {
                return
            }

            val unusableReason = checkUnusable(true)
            if (unusableReason != null) {
                logger.debug("Conduit not activating for '{}' because '{}'", player.name, lazyString { unusableReason.startingMessage.string })
                playerEntity.sendMessage(Console.formatInfo(unusableReason.startingMessage))
                return
            }

            isActive = true

            logger.info("Conduit activating for '{}'", player.name)

            playerEntity.sendMessage(Console.formatInfo("You've activated your conduit!"))
            playerEntity.play(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0, 1.0)
            playerEntity.particles(ParticleTypes.FLASH)

            when (team) {
                Team.YELLOW -> startYellow()
                Team.BLUE -> startBlue()
            }
        }
    }

    private val states = mutableMapOf<GamePlayer, State>()
    private fun stateFor(player: GamePlayer) = states.getOrPut(player) { State(player) }

    override fun update() {
        states.values.forEach(State::update)
    }

    fun handleConduitUse(player: GamePlayer, playerEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        stateFor(player).use(player, playerEntity, stack)
        return ActionResult.FAIL
    }

    fun shouldIgnoreDamage(player: GamePlayer, playerEntity: ServerPlayerEntity, source: DamageSource): Boolean {
        val state = stateFor(player)
        return state.isActive && state.team == Team.YELLOW && !source.isOf(DamageTypes.STARVE)
    }

}
