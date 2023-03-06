package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameProperties
import dev.foxgirl.mineseekdestroy.util.BlockFinder
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.Scheduler
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.random.Random

class SpecialGhostService : Service() {

    private val region = Region(BlockPos(90, -40, -24), BlockPos(50, -40, -88))

    private val spawnEnabled get() = game.getRuleBoolean(Game.RULE_GHOSTS_ENABLED)
    private val spawnDelayMin get() = game.getRuleDouble(Game.RULE_GHOSTS_SPAWN_DELAY_MIN)
    private val spawnDelayMax get() = game.getRuleDouble(Game.RULE_GHOSTS_SPAWN_DELAY_MAX)

    private fun spawnDelay(): Double {
        return try {
            Random.nextDouble(spawnDelayMin, spawnDelayMax)
        } catch (err: IllegalArgumentException) {
            30.0
        }
    }

    private var spawnPositions = listOf<Vec3d>()

    private fun spawnGhoul() {
        val entityType = if (Random.nextBoolean()) EntityType.SKELETON else EntityType.HUSK
        val entity = entityType.create(world) ?: return
        entity.setPosition(spawnPositions.random())
        entity.equipStack(EquipmentSlot.MAINHAND, ItemStack(IRON_SWORD))
        entity.equipStack(EquipmentSlot.HEAD, ItemStack(LEATHER_HELMET))
        entity.equipStack(EquipmentSlot.CHEST, ItemStack(CHAINMAIL_CHESTPLATE))
        entity.equipStack(EquipmentSlot.LEGS, ItemStack(CHAINMAIL_LEGGINGS).apply { addEnchantment(Enchantments.SWIFT_SNEAK, 3) })
        entity.equipStack(EquipmentSlot.FEET, ItemStack(LEATHER_BOOTS))
        world.spawnEntity(entity)
    }

    private fun spawnGhost() {
        val entity = EntityType.VEX.create(world) ?: return
        entity.setPosition(spawnPositions.random())
        entity.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY)
        entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY)
        entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)!!.baseValue = 0.0
        world.spawnEntity(entity)
    }

    private fun spawn() {
        if (!spawnEnabled) return
        if (!world.isNight) return
        if (Random.nextDouble() < 0.33) {
            spawnGhost()
        } else {
            spawnGhoul()
        }
    }

    override fun setup() {
        if (properties != GameProperties.Macander) return
        BlockFinder
            .search(world, region) { it.block === Blocks.OCHRE_FROGLIGHT }
            .handle { results, err ->
                if (err != null) {
                    logger.error("SpecialGhostService search for spawn positions failed", err)
                } else {
                    logger.info("SpecialGhostService search for spawn positions returned ${results.size} result(s)")
                    spawnPositions = results.map { it.pos.let { Vec3d(it.x + 0.5, it.y + 1.0, it.z + 0.5) } }
                }
            }
    }

    private var schedule: Scheduler.Schedule? = null

    override fun update() {
        if (properties != GameProperties.Macander) return
        if (schedule == null && spawnEnabled) {
            schedule = Scheduler.delay(spawnDelay()) {
                schedule = null
                spawn()
            }
        }
    }

}
