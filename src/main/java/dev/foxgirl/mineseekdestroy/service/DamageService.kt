package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameContext
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.toImmutableList
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Util
import java.util.*

class DamageService : Service() {

    private companion object {

        fun findEntity(uuid: UUID): Entity? {
            for (world in Game.getGame().server.worlds) {
                val entity = world.getEntity(uuid)
                if (entity != null) {
                    return entity
                }
            }
            return null
        }

        fun getDamageTypeRegistry(): Registry<DamageType> =
            Game.getGame().server.registryManager.get(RegistryKeys.DAMAGE_TYPE)

        fun damageTypeFromNbt(nbt: NbtElement): DamageType {
            return getDamageTypeRegistry().get(nbt.toIdentifier())!!
        }
        fun damageTypeToNbt(type: DamageType): NbtElement {
            return toNbt(getDamageTypeRegistry().getKey(type).get().value)
        }

        fun damageTypeGetEntry(type: DamageType): RegistryEntry<DamageType> {
            return getDamageTypeRegistry().getEntry(type)
        }

        fun damageSourceFromNbt(nbt: NbtCompound): DamageSource {
            try {
                val type = damageTypeFromNbt(nbt["Type"]!!)
                val typeEntry = damageTypeGetEntry(type)
                val source = nbt["Attacker"]?.toUUID()?.let(::findEntity)
                val attacker = nbt["Source"]?.toUUID()?.let(::findEntity)
                val position = nbt["Position"]?.toVec3d()
                return DamageSource(typeEntry, source, attacker, position)
            } catch (cause: Exception) {
                throw RuntimeException("Failed to deserialize DamageSource from NBT", cause)
            }
        }
        fun damageSourceToNbt(source: DamageSource): NbtCompound {
            try {
                val nbt = nbtCompound()
                nbt["Type"] = damageTypeToNbt(source.type)
                if (source.attacker != null) {
                    nbt["Attacker"] = source.attacker!!.uuid
                }
                if (source.source != null) {
                    nbt["Source"] = source.source!!.uuid
                }
                if (source.position != null) {
                    nbt["Position"] = toNbt(source.position!!)
                }
                return nbt
            } catch (cause: Exception) {
                throw RuntimeException("Failed to serialize DamageSource to NBT", cause)
            }
        }

    }

    data class DamageRecord(
        val attackerEntity: Entity?,
        val attacker: GamePlayer?,
        val victimEntity: Entity?,
        val victim: GamePlayer?,
        val source: DamageSource,
        val amount: Float,
        val timestamp: Long = System.currentTimeMillis(),
    ) {
        constructor(context: GameContext, nbt: NbtCompound) : this(
            findEntity(nbt["Attacker"].toUUID()),
            context.getPlayer(nbt["Attacker"].toUUID()),
            findEntity(nbt["Victim"].toUUID()),
            context.getPlayer(nbt["Victim"].toUUID()),
            damageSourceFromNbt(nbt["Source"].asCompound()),
            nbt["Amount"].toFloat(),
            nbt["Timestamp"].toLong(),
        )

        fun toNbt(): NbtCompound {
            return nbtCompoundOf(
                "Attacker" to (attacker?.uuid ?: attackerEntity?.uuid ?: Util.NIL_UUID),
                "Victim" to (victim?.uuid ?: victimEntity?.uuid ?: Util.NIL_UUID),
                "Source" to damageSourceToNbt(source),
                "Amount" to amount,
                "Timestamp" to timestamp,
            )
        }

        override fun toString(): String {
            val attackerName = attacker?.name ?: attackerEntity?.scoreboardName ?: "unknown"
            val victimName = victim?.name ?: victimEntity?.scoreboardName ?: "unknown"
            return "DamageRecord(attacker=\"$attackerName\", victim=\"$victimName\", source=\"${source.name}\", amount=${"%.2f".format(amount)})"
        }
    }

    private val damageRecords = mutableListOf<DamageRecord>()

    fun updateDamageRecords(action: (MutableList<DamageRecord>) -> Unit) {
        action(damageRecords)
    }

    fun copyDamageRecords(): List<DamageRecord> {
        return damageRecords.toImmutableList()
    }

    fun findDamageRecords(predicate: (DamageRecord) -> Boolean): List<DamageRecord> {
        return damageRecords.filter(predicate)
    }

    private fun getAttacker(source: DamageSource): Pair<Entity?, GamePlayer?> {
        val entities = arrayOf(source.source, source.attacker)
        val playerEntity = entities.find { it is PlayerEntity } as PlayerEntity?
        if (playerEntity != null) {
            return playerEntity to context.getPlayer(playerEntity)
        }
        val entity = entities.find { it != null }
        if (entity != null) {
            return entity to context.getPlayer(entity)
        }
        return null to null
    }

    fun handleDamage(victimEntity: LivingEntity, source: DamageSource, amount: Float) {
        val (attackerEntity, attacker) = getAttacker(source)
        val victim = context.getPlayer(victimEntity)
        val record = DamageRecord(attackerEntity, attacker, victimEntity, victim, source, amount)
        damageRecords.add(record)
    }

}
