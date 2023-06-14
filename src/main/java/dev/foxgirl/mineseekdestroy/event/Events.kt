package dev.foxgirl.mineseekdestroy.event

import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.state.GameState
import kotlinx.serialization.Serializable

@Serializable
sealed class Event(val name: String)

@Serializable
class MessageEvent(val message: String) : Event("message")

@Serializable
class UpdateEvent(val game: Game) : Event("update")

@Serializable
class StateChangeEvent(val oldState: GameState, val newState: GameState) : Event("stateChange")

@Serializable
class AliveChangeEvent(val player: GamePlayer, val oldAlive: Boolean, val newAlive: Boolean) : Event("aliveChange")

@Serializable
class TeamChangeEvent(val player: GamePlayer, val oldTeam: GameTeam, val newTeam: GameTeam) : Event("teamChange")

@Serializable
class DamageEvent(val player: GamePlayer, val amount: Float) : Event("damage")

@Serializable
class DeathEvent(val player: GamePlayer, val attacker: GamePlayer?) : Event("death")
