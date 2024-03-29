package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameProperties
import dev.foxgirl.mineseekdestroy.util.Editor
import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import dev.foxgirl.mineseekdestroy.util.async.terminate
import dev.foxgirl.mineseekdestroy.util.stackOf
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.Items.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.random.Random

class SpecialGhoulService : Service() {

    private val region = Region(BlockPos(90, -40, -24), BlockPos(50, -40, -88))

    private val spawnEnabled get() = game.getRuleBoolean(Game.RULE_GHOULS_ENABLED)
    private val spawnDelayMin get() = game.getRuleDouble(Game.RULE_GHOULS_SPAWN_DELAY_MIN)
    private val spawnDelayMax get() = game.getRuleDouble(Game.RULE_GHOULS_SPAWN_DELAY_MAX)

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
        val pos = spawnPositions.random()
        entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0F, 0F)
        entity.equipStack(EquipmentSlot.MAINHAND, stackOf(IRON_SWORD))
        entity.equipStack(EquipmentSlot.HEAD, stackOf(LEATHER_HELMET))
        entity.equipStack(EquipmentSlot.CHEST, stackOf(CHAINMAIL_CHESTPLATE))
        entity.equipStack(EquipmentSlot.LEGS, stackOf(CHAINMAIL_LEGGINGS) { it.addEnchantment(Enchantments.SWIFT_SNEAK, 3) })
        entity.equipStack(EquipmentSlot.FEET, stackOf(LEATHER_BOOTS))
        world.spawnEntity(entity)
    }

    private fun spawnGhost() {
        val entity = EntityType.VEX.create(world) ?: return
        entity.setPosition(spawnPositions.random())
        entity.equipStack(EquipmentSlot.MAINHAND, stackOf())
        entity.equipStack(EquipmentSlot.OFFHAND, stackOf())
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
        Editor
            .queue(world, region)
            .search { it.block === Blocks.OCHRE_FROGLIGHT }
            .thenApply { results ->
                logger.info("SpecialGhoulService search for spawn positions returned ${results.size} result(s)")
                spawnPositions = results.map { it.pos.let { Vec3d(it.x + 0.5, it.y + 1.0, it.z + 0.5) } }
            }
            .terminate()
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
