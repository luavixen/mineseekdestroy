package dev.foxgirl.mineseekdestroy.event

import dev.foxgirl.mineseekdestroy.*
import dev.foxgirl.mineseekdestroy.state.GameState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d
import java.util.*

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor(UUID::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

object PositionSerializer : KSerializer<Position> {
    override val descriptor = buildClassSerialDescriptor(Position::class.qualifiedName!!) {
        element<Double>("x")
        element<Double>("y")
        element<Double>("z")
    }

    override fun serialize(encoder: Encoder, value: Position) {
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.x)
            encodeDoubleElement(descriptor, 1, value.y)
            encodeDoubleElement(descriptor, 2, value.z)
        }
    }

    override fun deserialize(decoder: Decoder): Position {
        return decoder.decodeStructure(descriptor) {
            Vec3d(
                decodeDoubleElement(descriptor, 0),
                decodeDoubleElement(descriptor, 1),
                decodeDoubleElement(descriptor, 2),
            )
        }
    }
}

object StatusEffectInstanceSerializer : KSerializer<StatusEffectInstance> {
    override val descriptor = buildClassSerialDescriptor(StatusEffectInstance::class.qualifiedName!!) {
        element<String>("type")
        element<Int>("amplifier")
        element<Int>("duration")
    }

    override fun serialize(encoder: Encoder, value: StatusEffectInstance) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.effectType.translationKey)
            encodeIntElement(descriptor, 1, value.amplifier)
            encodeIntElement(descriptor, 2, value.duration)
        }
    }

    override fun deserialize(decoder: Decoder): StatusEffectInstance {
        throw UnsupportedOperationException()
    }
}

object ServerPlayerEntitySerializer : KSerializer<ServerPlayerEntity> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = buildClassSerialDescriptor(ServerPlayerEntity::class.qualifiedName!!) {
        element("position", PositionSerializer.descriptor)
        element<Float>("health")
        element<Float>("healthMax")
        element<Float>("healthAbsorption")
        element<Int>("hungerFoodLevel")
        element<Float>("hungerSaturationLevel")
        element<Float>("hungerExhaustion")
        element("effects", listSerialDescriptor(StatusEffectInstanceSerializer.descriptor))
    }

    override fun serialize(encoder: Encoder, value: ServerPlayerEntity) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, PositionSerializer, value.pos)
            encodeFloatElement(descriptor, 1, value.health)
            encodeFloatElement(descriptor, 2, value.maxHealth)
            encodeFloatElement(descriptor, 3, value.absorptionAmount)
            encodeIntElement(descriptor, 4, value.hungerManager.foodLevel)
            encodeFloatElement(descriptor, 5, value.hungerManager.saturationLevel)
            encodeFloatElement(descriptor, 6, value.hungerManager.exhaustion)
            encodeSerializableElement(descriptor, 7, ListSerializer(StatusEffectInstanceSerializer), value.activeStatusEffects.values.toList())
        }
    }

    override fun deserialize(decoder: Decoder): ServerPlayerEntity {
        throw UnsupportedOperationException()
    }
}

object GameTeamSerializer : KSerializer<GameTeam> {
    override val descriptor = PrimitiveSerialDescriptor(GameTeam::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GameTeam) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): GameTeam {
        return GameTeam.valueOf(decoder.decodeString())
    }
}

object GamePlayerSerializer : KSerializer<GamePlayer> {
    override val descriptor = buildClassSerialDescriptor(GamePlayer::class.qualifiedName!!) {
        element("uuid", UUIDSerializer.descriptor)
        element<String>("name")
        element<Boolean>("alive")
        element<GameTeam>("team")
        element<Int>("kills")
        element<Int>("deaths")
        element("entity", ServerPlayerEntitySerializer.descriptor.nullable)
    }

    override fun serialize(encoder: Encoder, value: GamePlayer) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, UUIDSerializer, value.uuid)
            encodeStringElement(descriptor, 1, value.name)
            encodeBooleanElement(descriptor, 2, value.isAlive)
            encodeSerializableElement(descriptor, 3, serializer(), value.team)
            encodeIntElement(descriptor, 4, value.kills)
            encodeIntElement(descriptor, 5, value.deaths)
            encodeSerializableElement(descriptor, 6, ServerPlayerEntitySerializer.nullable, value.entity)
        }
    }

    override fun deserialize(decoder: Decoder): GamePlayer {
        throw UnsupportedOperationException()
    }
}

object GamePropertiesSerializer : KSerializer<GameProperties> {
    override val descriptor = PrimitiveSerialDescriptor(GameProperties::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GameProperties) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): GameProperties {
        throw UnsupportedOperationException()
    }
}

object GameStateSerializer : KSerializer<GameState> {
    override val descriptor = PrimitiveSerialDescriptor(GameState::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GameState) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): GameState {
        throw UnsupportedOperationException()
    }
}

object GameContextSerializer : KSerializer<GameContext> {
    override val descriptor = buildClassSerialDescriptor(GameContext::class.qualifiedName!!) {
        element<List<GamePlayer>>("players")
    }

    override fun serialize(encoder: Encoder, value: GameContext) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, serializer<List<GamePlayer>>(), value.players)
        }
    }

    override fun deserialize(decoder: Decoder): GameContext {
        throw UnsupportedOperationException()
    }
}

object GameSerializer : KSerializer<Game> {
    override val descriptor = buildClassSerialDescriptor(Game::class.qualifiedName!!) {
        element<GameProperties>("properties")
        element<GameState>("state")
        element<GameContext?>("context")
        element<Map<String, String>>("rules")
    }

    override fun serialize(encoder: Encoder, value: Game) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, serializer(), value.properties)
            encodeSerializableElement(descriptor, 1, serializer(), value.state)
            encodeSerializableElement(descriptor, 2, serializer(), value.context)
            encodeSerializableElement(descriptor, 3, serializer(), value.server.gameRules.rules.map { (key, rule) -> key.name to rule.serialize() }.toMap())
        }
    }

    override fun deserialize(decoder: Decoder): Game {
        throw UnsupportedOperationException()
    }
}
