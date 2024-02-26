package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.util.*
import net.minecraft.entity.damage.DamageSource
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

class DamageService : Service() {

    companion object {

        @JvmStatic
        fun sumDamageRecords(records: Iterable<DamageRecord>): Float {
            var total = 0.0F; for (record in records) total += record.amount
            return total
        }

    }

    data class DamageRecord(
        val attackerUUID: UUID,
        val victimUUID: UUID,
        val amount: Float,
    ) {
        constructor(attacker: GamePlayer?, victim: GamePlayer?, amount: Float) : this(
            attacker?.uuid ?: nilUUID(),
            victim?.uuid ?: nilUUID(),
            amount,
        )

        constructor(nbt: NbtCompound) : this(
            nbt["Attacker"].toUUID(),
            nbt["Victim"].toUUID(),
            nbt["Amount"].toFloat(),
        )

        fun toNbt() = nbtCompoundOf(
            "Attacker" to attackerUUID,
            "Victim" to victimUUID,
            "Amount" to amount,
        )

        private val context get() = Game.getGame().context

        val attacker get() = context?.getPlayer(attackerUUID)
        val victim get() = context?.getPlayer(victimUUID)

        override fun toString() =
            "DamageRecord(attacker=${attacker?.nameQuoted ?: attackerUUID}, victim=${victim?.nameQuoted ?: victimUUID}, amount=$amount)"
    }

    private val records: MutableList<DamageRecord> = ArrayList(1024)

    private val attackerRecords: MutableMap<UUID, MutableList<DamageRecord>> = HashMap(32)
    private val victimRecords: MutableMap<UUID, MutableList<DamageRecord>> = HashMap(32)

    private fun appendRecordToMappings(record: DamageRecord) {
        if (!record.attackerUUID.isNil) attackerRecords.getOrPut(record.attackerUUID, ::mutableListOf).add(record)
        if (!record.victimUUID.isNil) victimRecords.getOrPut(record.victimUUID, ::mutableListOf).add(record)
    }

    private fun appendRecord(record: DamageRecord) {
        records.add(record)
        appendRecordToMappings(record)
    }

    fun addRecord(attacker: GamePlayer?, victim: GamePlayer?, amount: Float) {
        appendRecord(DamageRecord(attacker, victim, amount))
    }
    fun addRecord(attacker: GamePlayer?, amount: Float) {
        appendRecord(DamageRecord(attacker, null, amount))
    }

    val damageRecords: List<DamageRecord> = Collections.unmodifiableList(records)

    fun findRecordsForAttacker(attacker: GamePlayer) = findRecordsForAttacker(attacker.uuid)
    fun findRecordsForAttacker(attackerUUID: UUID): List<DamageRecord> {
        return attackerRecords[attackerUUID]?.let { Collections.unmodifiableList(it) } ?: emptyList()
    }
    fun findRecordsForVictim(victim: GamePlayer) = findRecordsForVictim(victim.uuid)
    fun findRecordsForVictim(victimUUID: UUID): List<DamageRecord> {
        return victimRecords[victimUUID]?.let { Collections.unmodifiableList(it) } ?: emptyList()
    }

    fun updateDamageRecords(action: (MutableList<DamageRecord>) -> Unit) {
        action(records)
        attackerRecords.clear()
        victimRecords.clear()
        for (record in records) appendRecordToMappings(record)
    }

    fun handleDamage(victimEntity: ServerPlayerEntity, source: DamageSource, amount: Float) {
        val attacker =
            if (source.source is ServerPlayerEntity) {
                context.getPlayer(source.source as ServerPlayerEntity)
            } else if (source.attacker is ServerPlayerEntity) {
                context.getPlayer(source.attacker as ServerPlayerEntity)
            } else {
                null
            }
        val victim = context.getPlayer(victimEntity)
        val record = DamageRecord(attacker?.uuid ?: nilUUID(), victim.uuid, amount)
        appendRecord(record)
    }

}
