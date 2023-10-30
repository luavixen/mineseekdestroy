package dev.foxgirl.mineseekdestroy.service

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.foxgirl.mineseekdestroy.Game
import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.GamePlayer
import dev.foxgirl.mineseekdestroy.GameTeam
import dev.foxgirl.mineseekdestroy.service.PagesService.Action.*
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.async.Async
import dev.foxgirl.mineseekdestroy.util.async.Scheduler
import dev.foxgirl.mineseekdestroy.util.async.await
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import dev.foxgirl.mineseekdestroy.util.collect.immutableMapOf
import net.minecraft.block.Blocks
import net.minecraft.entity.effect.StatusEffects.*
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Direction
import net.minecraft.world.chunk.ChunkStatus
import net.minecraft.world.chunk.WorldChunk
import java.util.*
import kotlin.random.Random

class PagesService : Service() {

    enum class Action { SUMMON, HEALTH, REGEN, AREA, BUSTED }

    private val books = enumMapOf<Theology, Book>()

    private abstract inner class Book(val theology: Theology, val stack: ItemStack) {
        init { check(books.putIfAbsent(theology, this) == null) { "Duplicate book" } }

        val type = BookType(theology)
        val pages = enumMapOf<Action, Page>()

        abstract inner class Page(val action: Action, val name: Text, vararg lore: Text) {
            init { check(pages.putIfAbsent(action, this) == null) { "Duplicate page" } }

            val type = PageType(theology, action)
            val stack = stackOf(Items.WHITE_BANNER, type.toNbt() + pageBannerNbt[type]!!, name, lore.asList())

            protected fun lockDelay(user: GamePlayer, seconds: Double) {
                lock(user)
                Scheduler.delay(seconds) { unlock(user) }
            }
            protected fun lock(user: GamePlayer) = user.activePages().add(type)
            protected fun unlock(user: GamePlayer) = user.activePages().remove(type)

            open fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult = ActionResult.PASS
            open fun attack(user: GamePlayer, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult = ActionResult.PASS
        }

        fun use(userEntity: ServerPlayerEntity): ActionResult {
            fun randomAction(exclude: Array<Action?>, times: Int = 1): Action {
                when (Random.nextInt(0, 100)) {
                    in 0..<4 -> if (BUSTED !in exclude) return BUSTED
                    in 4..<13 -> if (AREA !in exclude) return AREA
                    in 13..<30 -> if (REGEN !in exclude) return REGEN
                    in 30..<53 -> if (HEALTH !in exclude) return HEALTH
                    in 53..<100 -> if (SUMMON !in exclude) return SUMMON
                }
                return if (times >= 5) SUMMON else randomAction(exclude, times + 1)
            }
            Async.go {
                val actions = arrayOfNulls<Action>(3).also {
                    it[0] = randomAction(it)
                    it[1] = randomAction(it)
                    it[2] = randomAction(it)
                }
                for (action in actions) {
                    delay(0.666)
                    userEntity.give(pages[action]!!.stack.copy())
                    userEntity.play(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0, Random.nextDouble(0.9, 1.1))
                }
            }
            return ActionResult.SUCCESS
        }
    }

    fun randomBook(): ItemStack = books.values.random().stack.copy()

    private fun bookFor(stack: ItemStack) =
        bookTypeFor(stack)?.let { (theology) -> books[theology] }
    private fun pageFor(stack: ItemStack) =
        pageTypeFor(stack)?.let { (theology, action) -> books[theology]?.let { it.pages[action] } }

    private fun pageStackPairFor(stack: ItemStack): Pair<Book.Page, ItemStack>? =
        pageFor(stack)?.let { it to stack }
    private fun pageStackPairFor(userEntity: ServerPlayerEntity): Pair<Book.Page, ItemStack>? =
        pageStackPairFor(userEntity.mainHandStack) ?: pageStackPairFor(userEntity.offHandStack)

    private val lockedPlayers = mutableSetOf<UUID>()

    private val activePages = mutableMapOf<GamePlayer, MutableSet<PageType>>()
    private fun GamePlayer.activePages(): MutableSet<PageType> = activePages.getOrPut(this, ::mutableSetOf)

    private fun resultApplyToStack(userEntity: ServerPlayerEntity, stack: ItemStack, action: () -> ActionResult): ActionResult {
        if (lockedPlayers.contains(userEntity.uuid)) { return ActionResult.FAIL }; lockedPlayers.add(userEntity.uuid)
        val result = action()
        if (result.shouldIncrementStat()) {
            val inventory = userEntity.inventory
            val slot = inventory.indexOf(stack)
            if (slot >= 0) {
                inventory.removeStack(slot, 1)
            } else {
                stack.decrement(1)
            }
        }
        Scheduler.now { userEntity.playerScreenHandler.syncState() }
        return result
    }

    private fun isPageUsageBlocked(userEntity: ServerPlayerEntity, page: Book.Page): Boolean {
        val user = context.getPlayer(userEntity)
        if (user.isOperator) return true
        if (state.isWaiting) {
            userEntity.sendMessage(text("Cannot use page while waiting for next round").red())
            return true
        }
        if (
            properties.regionBlimp.contains(userEntity) ||
            properties.regionBlimpBalloons.contains(userEntity) ||
            properties.regionPlayable.excludes(userEntity)
        ) {
            userEntity.sendMessage(text("Cannot use page while in the blimp or out of bounds").red())
            return true
        }
        if (userEntity.world !== world) {
            userEntity.sendMessage(text("Cannot use page while in another dimension").red())
            return true
        }
        if (!user.isAlive || !user.isPlaying) {
            userEntity.sendMessage(text("Cannot use page while dead/ghost/etc.").red())
            return true
        }
        if (page.type in user.activePages()) {
            userEntity.sendMessage(text("Cannot use page, effect already active").red())
            return true
        }
        return false
    }

    fun handleBookUse(userEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val book = bookFor(stack) ?: return ActionResult.FAIL
        return resultApplyToStack(userEntity, stack) { book.use(userEntity) }
    }

    private data class ValueGenericUse(val user: GamePlayer, val userEntity: ServerPlayerEntity)
    private data class ValueGenericAttack(val user: GamePlayer, val userEntity: ServerPlayerEntity, val victimEntity: ServerPlayerEntity)
    private val eventGenericUse = Event<ValueGenericUse>()
    private val eventGenericAttack = Event<ValueGenericAttack>()

    fun handleGenericUse(userEntity: ServerPlayerEntity): ActionResult {
        val user = context.getPlayer(userEntity)
        eventGenericUse.publish(ValueGenericUse(user, userEntity))
        val (page, stack) = pageStackPairFor(userEntity) ?: return ActionResult.PASS
        val result = resultApplyToStack(userEntity, stack) {
            if (isPageUsageBlocked(userEntity, page)) ActionResult.FAIL
            else page.use(user, userEntity)
        }
        if (result.shouldIncrementStat()) {
            Game.CONSOLE_PLAYERS.sendInfo(
                text(user, "used page").styleParent { it.withColor(Formatting.WHITE) },
                text(page.name).style { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(page.stack))) },
            )
        }
        return result
    }
    fun handleGenericAttack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
        val user = context.getPlayer(userEntity)
        eventGenericAttack.publish(ValueGenericAttack(user, userEntity, victimEntity))
        val (page, stack) = pageStackPairFor(userEntity) ?: return ActionResult.PASS
        val result = resultApplyToStack(userEntity, stack) {
            if (isPageUsageBlocked(userEntity, page)) ActionResult.FAIL
            else page.attack(user, userEntity, victimEntity)
        }
        return result
    }

    data class BookType(val theology: Theology) {
        fun toNbt() = nbtCompoundOf("MsdBook" to true, "MsdBookTheology" to theology)
    }
    data class PageType(val theology: Theology, val action: Action) {
        fun toNbt() = nbtCompoundOf("MsdPage" to true, "MsdPageTheology" to theology, "MsdPageAction" to action)
    }

    fun executeBookGive(console: Console, players: List<GamePlayer>, type: BookType) {
        val stack = books[type.theology]!!.stack
        players.forEach { it.entity?.give(stack.copy()) }
        console.sendInfo("Gave book", stack.name(), "to ${players.size} player(s)")
    }
    fun executePageGive(console: Console, players: List<GamePlayer>, type: PageType) {
        val stack = books[type.theology]!!.pages[type.action]!!.stack
        players.forEach { it.entity?.give(stack.copy()) }
        console.sendInfo("Gave page", stack.name(), "to ${players.size} player(s)")
    }

    override fun update() {
        for ((_, playerEntity) in playerEntitiesIn) {
            if (playerEntity.air < -20) {
                playerEntity.air = 0
                playerEntity.hurtHearts(1.0) { it.drown() }
                if (playerEntity.isSubmergedInWater) {
                    for (i in 0..<4) {
                        playerEntity.particles(ParticleTypes.BUBBLE, 1.0, 2) {
                            it.add(
                                Random.nextDouble(-0.5, 0.5),
                                Random.nextDouble(1.5, 2.0),
                                Random.nextDouble(-0.5, 0.5),
                            )
                        }
                    }
                } else {
                    for (i in 0..<8) {
                        playerEntity.particles(ParticleTypes.FALLING_WATER, 1.0, 2) {
                            it.add(
                                Random.nextDouble(-0.3, 0.3),
                                Random.nextDouble(1.5, 1.75),
                                Random.nextDouble(-0.3, 0.3),
                            )
                        }
                    }
                }
            }
        }
        lockedPlayers.clear()
    }

    companion object {
        fun bookTypeFor(stack: ItemStack): BookType? {
            if (stack.isEmpty) return null
            val nbt = stack.nbt ?: return null
            val nbtTheology = nbt["MsdBookTheology"] ?: return null
            return try { BookType(nbtTheology.toEnum()) } catch (ignored : RuntimeException) { null }
        }
        fun pageTypeFor(stack: ItemStack): PageType? {
            if (stack.isEmpty) return null
            val nbt = stack.nbt ?: return null
            val nbtTheology = nbt["MsdPageTheology"] ?: return null
            val nbtAction = nbt["MsdPageAction"] ?: return null
            return try { PageType(nbtTheology.toEnum(), nbtAction.toEnum()) } catch (ignored : RuntimeException) { null }
        }

        private var pageBannerNbt = emptyMap<PageType, NbtCompound>()
        init {
            try {
                pageBannerNbt = immutableMapOf(
                    // Deep
                    PageType(DEEP, SUMMON) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:9,Pattern:\"gru\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(DEEP, HEALTH) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:9,Pattern:\"sc\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(DEEP, REGEN) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:9,Pattern:\"moj\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(DEEP, AREA) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:9,Pattern:\"cbo\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(DEEP, BUSTED) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:9,Pattern:\"gra\"},{Color:9,Pattern:\"gru\"},{Color:0,Pattern:\"sku\"}],id:\"minecraft:banner\"}}").asCompound(),
                    // Occult
                    PageType(OCCULT, SUMMON) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:2,Pattern:\"gru\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(OCCULT, HEALTH) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:2,Pattern:\"sc\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(OCCULT, REGEN) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:2,Pattern:\"moj\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(OCCULT, AREA) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:2,Pattern:\"cbo\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(OCCULT, BUSTED) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:2,Pattern:\"gra\"},{Color:2,Pattern:\"gru\"},{Color:0,Pattern:\"sku\"}],id:\"minecraft:banner\"}}").asCompound(),
                    // Cosmos
                    PageType(COSMOS, SUMMON) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:11,Pattern:\"gru\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(COSMOS, HEALTH) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:11,Pattern:\"sc\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(COSMOS, REGEN) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:11,Pattern:\"moj\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(COSMOS, AREA) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:11,Pattern:\"cbo\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(COSMOS, BUSTED) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:11,Pattern:\"gra\"},{Color:11,Pattern:\"gru\"},{Color:0,Pattern:\"sku\"}],id:\"minecraft:banner\"}}").asCompound(),
                    // Barter
                    PageType(BARTER, SUMMON) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:4,Pattern:\"gru\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(BARTER, HEALTH) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:4,Pattern:\"sc\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(BARTER, REGEN) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:4,Pattern:\"moj\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(BARTER, AREA) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:4,Pattern:\"cbo\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(BARTER, BUSTED) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:4,Pattern:\"gra\"},{Color:4,Pattern:\"gru\"},{Color:0,Pattern:\"sku\"}],id:\"minecraft:banner\"}}").asCompound(),
                    // Flame
                    PageType(FLAME, SUMMON) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:14,Pattern:\"gru\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(FLAME, HEALTH) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:14,Pattern:\"sc\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(FLAME, REGEN) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:14,Pattern:\"moj\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(FLAME, AREA) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:14,Pattern:\"cbo\"}],id:\"minecraft:banner\"}}").asCompound(),
                    PageType(FLAME, BUSTED) to nbtDecode("{BlockEntityTag:{Patterns:[{Color:15,Pattern:\"bri\"},{Color:0,Pattern:\"bo\"},{Color:14,Pattern:\"gra\"},{Color:14,Pattern:\"gru\"},{Color:0,Pattern:\"sku\"}],id:\"minecraft:banner\"}}").asCompound(),
                )
            } catch (cause: CommandSyntaxException) {
                if (cause.stackTrace.isNullOrEmpty()) cause.fillInStackTrace()
                Game.LOGGER.error("PagesService failed to decode NBT data for page banners", cause)
            }
        }

        private fun loreWithDeep() = text("combine with a ") + text("deep summon page").format(DEEP.color) + " to "
        private fun loreWithOccult() = text("combine with an ") + text("occult summon page").format(OCCULT.color) + " to "
        private fun loreWithCosmos() = text("combine with a ") + text("cosmos summon page").format(COSMOS.color) + " to "
        private fun loreWithBarter() = text("combine with a ") + text("barter summon page").format(BARTER.color) + " to "
        private fun loreWithFlame() = text("combine with a ") + text("flame summon page").format(FLAME.color) + " to "

        private fun pageUnimplemented(userEntity: ServerPlayerEntity): ActionResult {
            userEntity.sendMessage(text("Sorry, this page is unimplemented").red())
            userEntity.play(SoundEvents.BLOCK_GLASS_BREAK)
            return ActionResult.SUCCESS
        }

        private fun pageAmbrosiaUse(hearts: Double, userEntity: ServerPlayerEntity): ActionResult {
            if (userEntity.healHearts(hearts)) {
                userEntity.sparkles()
                return ActionResult.SUCCESS
            }
            return ActionResult.FAIL
        }
        private fun pageAmbrosiaAttack(hearts: Double, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
            victimEntity.hurtHearts(hearts) { it.playerAttack(userEntity) }
            userEntity.sparkles(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, false)
            return ActionResult.SUCCESS
        }

        internal fun ServerPlayerEntity.sparkles(sound: SoundEvent = SoundEvents.ITEM_TOTEM_USE, effects: Boolean = true) {
            val pos = pos.offset(Direction.UP, 1.25)
            if (effects) {
                addEffect(GLOWING, 8.0)
                Broadcast.sendParticles(ParticleTypes.FLASH, 1.0F, 1, world, pos)
            }
            play(sound, SoundCategory.PLAYERS, 1.0, Random.nextDouble(0.9, 1.1))
        }
    }

    init {

        object : Book(DEEP, GameItems.bookDeep) { init {

            object : Page(
                SUMMON, text("Deep Summon Page") * DEEP.color,
                loreWithDeep() + text("flood the map").format(DEEP.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithOccult() + text("receive a ") + text("player-tracking compass").bold() + "!",
                loreWithCosmos() + text("summon acid rain!"),
                loreWithBarter() + text("poison all water!"),
                loreWithFlame() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Deep") * DEEP.color,
                text("right-click to gain ") + text("1 heart").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("1 heart").bold() + " of damage!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity) = pageAmbrosiaUse(1.0, userEntity)
                override fun attack(user: GamePlayer, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity) = pageAmbrosiaAttack(1.0, userEntity, victimEntity)
            }

            object : Page(
                REGEN, text("Something Katara Read") * DEEP.color,
                text("right-click to activate!"),
                text("gain regen 3 for ") + text("15 seconds").bold() + "!",
                text("drown for ") + text("10 seconds").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        var running = true
                        go { delay(10.0); running = false }
                        go { until { state.isWaiting || !user.isAlive || !running }; running = false }
                        while (true) {
                            delay(0.5); if (!running) break
                            user.entity?.air = -10
                            delay(0.5); if (!running) break
                            user.entity?.let { if (it.air > -20) it.air = -40 }
                        }
                        user.entity?.air = 0
                        unlock(user)
                    }
                    userEntity.addEffect(REGENERATION, 15.0, 3)
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                AREA, text("Claustrophilia: Deep") * DEEP.color,
                text("right-click to activate!"),
                text("turn all blocks in a ") + text("3 block radius").bold() + " into water!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    userEntity.blockPos.up().around(2.0).forEach { world.setBlockState(it, Blocks.WATER.defaultState) }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                BUSTED, text("The Ocean; in Writing") * DEEP.color,
                text("right-click to activate!"),
                text("allows the user to swim in air!"),
                text("once activated, the user will begin to drown").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        val blockPositions = HashSet<BlockPos>()
                        val chunkPositions = HashSet<ChunkPos>()

                        while (true) {
                            delay(0.1)

                            if (state.isWaiting || !user.isAlive) break
                            val userEntity = user.entity ?: continue
                            if (userEntity.world !== world) continue

                            val newPositions = userEntity.blockPos
                                .around(5.5)
                                .filter { pos ->
                                    if (properties.regionPlayable.excludes(pos)) return@filter false
                                    if (properties.regionBlimp.contains(pos) || properties.regionBlimpBalloons.contains(pos)) return@filter false
                                    world.getBlockState(pos).isAir
                                }
                                .toList()

                            for (pos in newPositions) {
                                userEntity.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, Blocks.WATER.defaultState))
                                blockPositions.add(pos)
                                chunkPositions.add(ChunkPos(pos))
                            }

                            val oldPositionIterator = blockPositions.iterator()
                            while (oldPositionIterator.hasNext()) {
                                val pos = oldPositionIterator.next()
                                if (pos in newPositions) continue
                                oldPositionIterator.remove()
                                userEntity.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, world.getBlockState(pos)))
                            }

                            if (userEntity.health > 1.0F) {
                                userEntity.air -= 21
                            }

                            userEntity.addEffect(LEVITATION, 10.5)
                            userEntity.particles(ParticleTypes.FALLING_WATER, 1.0, 5) {
                                it.add(
                                    Random.nextDouble(-0.4, 0.4),
                                    Random.nextDouble(0.5, 2.25),
                                    Random.nextDouble(-0.4, 0.4),
                                )
                            }
                        }

                        val userEntity = user.entity
                        if (userEntity != null && userEntity.world == world) {
                            val viewDistance = server.playerManager.viewDistance + 4
                            for (chunkPos in chunkPositions.sortedWith { a, b -> a.toLong() compareTo b.toLong() }) {
                                val viewPos = userEntity.chunkPos
                                if (viewPos.getChebyshevDistance(chunkPos) > viewDistance) continue
                                val chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true) as WorldChunk
                                val chunkPacket = ChunkDataS2CPacket(chunk, world.lightingProvider, null, null)
                                userEntity.networkHandler.sendPacket(chunkPacket)
                            }
                        }

                        unlock(user)
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

        } }

        object : Book(OCCULT, GameItems.bookOccult) { init {

            object : Page(
                SUMMON, text("Occult Summon Page") * OCCULT.color,
                loreWithDeep() + text("receive a ") + text("player-tracking compass").bold() + "!",
                loreWithOccult() + text("nearly kill your opps and save all black players").format(OCCULT.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithCosmos() + text("majora the storm's center"),
                loreWithBarter() + text("receive an OP sword!"),
                loreWithFlame() + text("spawn ") + text("3 ghasts").bold() + "!",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Occult") * OCCULT.color,
                text("right-click to gain ") + text("1 heart").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("4 hearts").bold() + " of damage!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity) = pageAmbrosiaUse(1.0, userEntity)
                override fun attack(user: GamePlayer, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity) = pageAmbrosiaAttack(4.0, userEntity, victimEntity)
            }

            object : Page(
                REGEN, text("Death's Love Song") * OCCULT.color,
                text("right-click to activate!"),
                text("for ") + text("10 seconds").bold() + ", gain " + text("1.5 hearts").bold() + " upon taking damage!",
            )  {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        var running = true
                        go { delay(10.0); running = false }
                        go { until { state.isWaiting || !user.isAlive || !running }; running = false }
                        var previousHealth = userEntity.health
                        while (running) {
                            delay()
                            val userEntity = user.entity ?: continue
                            if (userEntity.health < previousHealth) {
                                userEntity.healHearts(1.5)
                                userEntity.particles(ParticleTypes.NOTE, 1.0, 6)
                                userEntity.play(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0, 0.5)
                            }
                            previousHealth = userEntity.health
                        }
                        unlock(user)
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                AREA, text("Claustrophilia: Occult") * OCCULT.color,
                text("right-click to activate!"),
                text("cause all players in a ") + text("5 block radius").bold() + " to freeze in place!",
                text("lasts ") + text("5 seconds!").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lockDelay(user, 5.0)
                    for ((_, playerEntity) in playerEntitiesNormal) {
                        if (playerEntity != userEntity && playerEntity.squaredDistanceTo(userEntity) <= 42.0) {
                            playerEntity.addEffect(SLOWNESS, 5.0, 7)
                            playerEntity.removeEffect(JUMP_BOOST)
                        }
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                BUSTED, text("Gruesome Gospel") * OCCULT.color,
                text("right-click to activate!"),
                text("gain ") + "speed 4" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "haste 2" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "strength 2" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "jump boost 2" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "resistance 1" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "night vision" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "fire resistance" + " for " + text("15 seconds").bold() + "!",
                text("gain ") + "water breathing" + " for " + text("15 seconds").bold() + "!",
                text("all living teammates instantly die").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lockDelay(user, 15.0)
                    for ((player, playerEntity) in playerEntitiesIn) {
                        if (player.team === user.team && player != user) {
                            playerEntity.hurtHearts(500.0) { it.indirectMagic(userEntity, userEntity) }
                        }
                    }
                    userEntity.addEffect(SPEED, 15.0, 4)
                    userEntity.addEffect(HASTE, 15.0, 2)
                    userEntity.addEffect(STRENGTH, 15.0, 2)
                    userEntity.addEffect(JUMP_BOOST, 15.0, 2)
                    userEntity.addEffect(RESISTANCE, 15.0)
                    userEntity.addEffect(NIGHT_VISION, 15.0)
                    userEntity.addEffect(FIRE_RESISTANCE, 15.0)
                    userEntity.addEffect(WATER_BREATHING, 15.0)
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

        } }

        object : Book(COSMOS, GameItems.bookCosmos) { init {

            object : Page(
                SUMMON, text("Cosmos Summon Page") * COSMOS.color,
                loreWithDeep() + text("summon acid rain!"),
                loreWithOccult() + text("majora the storm's center"),
                loreWithCosmos() + text("reduce gravity").format(COSMOS.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithBarter() + text("receive ") + text("8 steak").bold() + "!",
                loreWithFlame() + text("spawn fire at the storm's center!"),
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Cosmos") * COSMOS.color,
                text("right-click to gain ") + text("2.5 hearts").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("2.5 hearts").bold() + " of damage!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity) = pageAmbrosiaUse(2.5, userEntity)
                override fun attack(user: GamePlayer, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity) = pageAmbrosiaAttack(2.5, userEntity, victimEntity)
            }

            object : Page(
                REGEN, text("Found Superman") * COSMOS.color,
                text("right-click to activate!"),
                text("gain ") + text("4 absorption hearts").bold() + "!",
                text("gain ") + text("absorption") + "!",
                text("lose the ability to heal for the rest of the round") + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        delay(1.0)
                        var minimumHealth = user.entity?.health ?: 5000.0F
                        while (true) {
                            delay()
                            if (state.isWaiting || !user.isAlive) break
                            val userEntity = user.entity ?: continue
                            val currentHealth = userEntity.health
                            if (currentHealth > minimumHealth) {
                                userEntity.damage(userEntity.damageSources.magic(), (currentHealth - minimumHealth) + 0.05F)
                            } else if (currentHealth < minimumHealth) {
                                minimumHealth = currentHealth
                            }
                        }
                        user.entity?.removeEffect(ABSORPTION)
                        unlock(user)
                    }
                    userEntity.addEffect(ABSORPTION, Double.MAX_VALUE, 2)
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                AREA, text("Claustrophilia: Cosmos") * COSMOS.color,
                text("right-click to activate!"),
                text("SUCK all players in a ") + text("5 block radius").bold() + "!",
                text("will not teleport players through walls!"),
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    for ((_, playerEntity) in playerEntitiesNormal) {
                        if (playerEntity != userEntity && playerEntity.squaredDistanceTo(userEntity) <= 64.0) {
                            playerEntity.applyKnockback(2.0, playerEntity.x - userEntity.x, playerEntity.z - userEntity.z, true)
                            playerEntity.play(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP)
                        }
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                BUSTED, text("From The Stars") * COSMOS.color,
                text("right-click to activate!"),
                text("all teammates gain night vision for the remainder of the round!"),
                text("all opponents gain blindness for the remainder of the round!"),
                text("receive ") + text("10 family guy blocks").bold() + "!",
                text("receive ") + text("5 flint & steel").bold() + "!",
                text("user dies upon activation").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    val team = user.team
                    Scheduler.interval(0.5) { schedule ->
                        if (state.isPlaying) {
                            for ((player, playerEntity) in playerEntitiesNormal) {
                                if (player.isAlive) {
                                    if (player.team === team) {
                                        playerEntity.addEffect(NIGHT_VISION, 4.0)
                                        playerEntity.removeEffect(BLINDNESS)
                                    } else {
                                        playerEntity.addEffect(BLINDNESS, 4.0)
                                        playerEntity.removeEffect(NIGHT_VISION)
                                    }
                                } else {
                                    playerEntity.removeEffect(NIGHT_VISION)
                                    playerEntity.removeEffect(BLINDNESS)
                                }
                            }
                        } else {
                            for ((_, playerEntity) in playerEntitiesNormal) {
                                playerEntity.removeEffect(NIGHT_VISION)
                                playerEntity.removeEffect(BLINDNESS)
                            }
                            schedule.cancel()
                        }
                    }
                    userEntity.give(GameItems.familyGuyBlock.copyWithCount(10))
                    userEntity.give(GameItems.flintAndSteel.copyWithCount(5))
                    userEntity.hurtHearts(500.0) { it.magic() }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

        } }

        object : Book(BARTER, GameItems.bookBarter) { init {

            object : Page(
                SUMMON, text("Barter Summon Page") * BARTER.color,
                loreWithDeep() + text("poison all water!"),
                loreWithOccult() + text("receive an OP sword!"),
                loreWithCosmos() + text("receive ") + text("8 steak").bold() + "!",
                loreWithBarter() + text("destroy all special items").formatted(BARTER.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithFlame() + text("receive a stack of ") + text("blue ice").bold() + "!",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Barter") * BARTER.color,
                text("right-click to gain ") + text("2.5 hearts").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("2.5 hearts").bold() + " of damage!",
                text("both effects have a 25% chance to backfire").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    if (Random.nextDouble() <= 0.25) {
                        userEntity.hurtHearts(2.5) { it.playerAttack(userEntity) }
                        userEntity.sparkles(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, false)
                        return ActionResult.SUCCESS
                    }
                    return pageAmbrosiaUse(2.5, userEntity)
                }
                override fun attack(user: GamePlayer, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
                    if (Random.nextDouble() <= 0.25) {
                        victimEntity.healHearts(2.5)
                        userEntity.sparkles()
                        return ActionResult.SUCCESS
                    }
                    return pageAmbrosiaAttack(1.0, userEntity, victimEntity)
                }
            }

            object : Page(
                REGEN, text("Das Stoks Baybee") * BARTER.color,
                text("right-click to activate!"),
                text("heal ") + text("1 heart").bold() + " for every connecting full-swing hit!",
                text("take ") + text("1 heart of damage").bold() + " for every connecting incomplete hit!",
                text("lasts ") + text("30 seconds").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        val subscription = eventGenericAttack.subscribe { (evUser, evUserEntity, evVictimEntity) ->
                            if (context.getPlayer(evVictimEntity).let { !it.isAlive || !it.isPlayingOrGhost }) return@subscribe
                            if (evUser != user) return@subscribe
                            if (evUserEntity.getAttackCooldownProgress(0.0F) > 0.95F) {
                                evUserEntity.healHearts(1.0)
                                Scheduler.delay(0.1) {
                                    evUserEntity.play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 0.333, 0.75)
                                }
                            } else {
                                evUserEntity.hurtHearts(1.0) { it.magic() }
                                Scheduler.delay(0.1) {
                                    evUserEntity.play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 0.333, 0.25)
                                }
                            }
                        }
                        awaitAny(
                            execute { delay(30.0) },
                            execute { until(1.0) { state.isWaiting || !user.isAlive } },
                        )
                        subscription.unsubscribe()
                        unlock(user)
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                AREA, text("Claustrophilia: Barter") * BARTER.color,
                text("right-click to activate!"),
                text("launch all players in a ") + text("5 block radius").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    for ((_, playerEntity) in playerEntitiesNormal) {
                        if (playerEntity != userEntity && playerEntity.squaredDistanceTo(userEntity) <= 64.0) {
                            playerEntity.applyKnockback(2.0, userEntity.x - playerEntity.x, userEntity.z - playerEntity.z, true)
                            playerEntity.play(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP)
                        }
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                BUSTED, text("Midas’ Records") * BARTER.color,
                text("right-click to activate!"),
                text("ghostable blocks disappear upon contact!"),
                text("user becomes a ghost upon death or round end").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        val blocks = mutableMapOf<BlockPos, Pair<Long, Int>>()
                        var iteration = 0L
                        while (true) {
                            delay()
                            iteration += 1

                            if (state.isWaiting || !user.isAlive) break
                            val userEntity = user.entity ?: continue

                            blocks.values.removeIf { (blockIteration) -> blockIteration < iteration - 20 }

                            for (blockPos in userEntity.blockPos.around(2.0)) {
                                if (blockPos in properties.regionBlimp || blockPos in properties.regionBlimpBalloons) continue
                                val blockState = world.getBlockState(blockPos)
                                if (
                                    blockState.block !in properties.unstealableBlocks && !blockState.isAir &&
                                    blockPos.toCenterPos().let {
                                        userEntity.pos.add(0.0, 0.5, 0.0).squaredDistanceTo(it) <= 2.75 ||
                                        userEntity.pos.add(0.0, 1.5, 0.0).squaredDistanceTo(it) <= 2.75
                                    }
                                ) {
                                    val blockProgress = blocks.get(blockPos)?.second ?: 0
                                    if (blockProgress >= 9) {
                                        world.setBlockState(blockPos, Blocks.GOLD_BLOCK.defaultState)
                                        world.breakBlock(blockPos, true)
                                        blocks.remove(blockPos)
                                    } else {
                                        world.setBlockBreakingInfo(blockPos.hashCode(), blockPos, blockProgress + 1)
                                        blocks.set(blockPos, iteration to blockProgress + 1)
                                    }
                                }
                            }
                        }
                        user.team = GameTeam.GHOST
                        unlock(user)
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

        } }

        object : Book(FLAME, GameItems.bookFlame) { init {

            object : Page(
                SUMMON, text("Flame Summon Page") * FLAME.color,
                loreWithDeep() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
                loreWithOccult() + text("spawn ") + text("3 ghasts").bold() + "!",
                loreWithCosmos() + text("spawn fire at the storm's center!"),
                loreWithBarter() + text("receive a stack of ") + text("blue ice").bold() + "!",
                loreWithFlame() + text("make every block flammable").format(FLAME.color) + "! (" + text("requires soul").bold().italic() + ")",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Flame") * FLAME.color,
                text("right-click to gain ") + text("4 hearts").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("1 heart").bold() + " of damage!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity) = pageAmbrosiaUse(4.0, userEntity)
                override fun attack(user: GamePlayer, userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity) = pageAmbrosiaAttack(1.0, userEntity, victimEntity)
            }

            object : Page(
                REGEN, text("Sun's At-Home Workout") * FLAME.color,
                text("right-click to activate!"),
                text("gain regen 3 for ") + text("15 seconds").bold() + "!",
                text("burn for ") + text("10 seconds").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lockDelay(user, 15.0)
                    userEntity.setOnFireFor(10)
                    userEntity.addEffect(REGENERATION, 15.0, 3)
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                AREA, text("Claustrophilia: Flame") * FLAME.color,
                text("right-click to activate!"),
                text("turn all blocks in a ") + text("4 block radius").bold() + " into fire!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    Async.go {
                        var running = true; go { delay(3.0); running = false }
                        val center = userEntity.blockPos.up()
                        val region = center.let {
                            Region(
                                it.add(+3, +3, +3),
                                it.add(-3, -3, -3),
                            )
                        }
                        val positions = center.around(3.0).toMutableSet()
                        while (running) {
                            Editor
                                .queue(world, region)
                                .edit { _, x, y, z -> if (BlockPos(x, y, z) in positions) Blocks.FIRE.defaultState else null }
                                .await()
                            delay()
                        }
                        world.setBlockState(center, Blocks.AIR.defaultState)
                    }
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                BUSTED, text("Burning Infomercial") * FLAME.color,
                text("right-click to activate!"),
                text("gain ") + "speed 4" + "!",
                text("gain ") + "haste 2" + "!",
                text("gain ") + "strength 2" + "!",
                text("gain ") + "jump boost 2" + "!",
                text("gain ") + "resistance 1" + "!",
                text("gain ") + "night vision" + "!",
                text("gain permanent wither until death").bold() + "!",
            ) {
                override fun use(user: GamePlayer, userEntity: ServerPlayerEntity): ActionResult {
                    lock(user)
                    Async.go {
                        until { state.isWaiting || !user.isAlive }
                        user.entity?.let {
                            it.removeEffect(SPEED)
                            it.removeEffect(HASTE)
                            it.removeEffect(STRENGTH)
                            it.removeEffect(JUMP_BOOST)
                            it.removeEffect(RESISTANCE)
                            it.removeEffect(NIGHT_VISION)
                            it.removeEffect(WITHER)
                        }
                        unlock(user)
                    }
                    userEntity.addEffect(SPEED, Double.MAX_VALUE, 4)
                    userEntity.addEffect(HASTE, Double.MAX_VALUE, 2)
                    userEntity.addEffect(STRENGTH, Double.MAX_VALUE, 2)
                    userEntity.addEffect(JUMP_BOOST, Double.MAX_VALUE, 2)
                    userEntity.addEffect(RESISTANCE, Double.MAX_VALUE)
                    userEntity.addEffect(NIGHT_VISION, Double.MAX_VALUE)
                    userEntity.addEffect(WITHER, Double.MAX_VALUE)
                    userEntity.sparkles()
                    return ActionResult.SUCCESS
                }
            }

        } }

    }

}
