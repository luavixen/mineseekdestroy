package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.GameItems
import dev.foxgirl.mineseekdestroy.service.PagesService.Action.*
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.enumMapOf
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import kotlin.random.Random

class PagesService : Service() {

    enum class Action { SUMMON, HEALTH, REGEN, AREA, BUSTED }

    private val books = enumMapOf<Theology, Book>()

    private abstract inner class Book(val theology: Theology, val stack: ItemStack) {
        init { check(books.putIfAbsent(theology, this) == null) { "Duplicate book" } }

        val pages = enumMapOf<Action, Page>()

        abstract inner class Page(val action: Action, name: Text, vararg lore: Text) {
            init { check(pages.putIfAbsent(action, this) == null) { "Duplicate page" } }

            val stack = stackOf(Items.PAPER, PageMeta(theology, action).toNbt(), name, lore.asList())

            open fun use(userEntity: ServerPlayerEntity): ActionResult = ActionResult.PASS
            open fun attack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult = ActionResult.PASS
        }

        fun use(userEntity: ServerPlayerEntity): ActionResult {
            fun randomAction(exclude: Array<Action?>, times: Int = 1): Action {
                if (Random.nextDouble() <= 0.10 && BUSTED !in exclude) return BUSTED
                if (Random.nextDouble() <= 0.20 && AREA !in exclude) return AREA
                if (Random.nextDouble() <= 0.25 && REGEN !in exclude) return REGEN
                if (Random.nextDouble() <= 0.33 && HEALTH !in exclude) return HEALTH
                if (Random.nextDouble() <= 0.50 && SUMMON !in exclude) return SUMMON
                if (times >= 5) return SUMMON
                return randomAction(exclude, times + 1)
            }
            Async.run {
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

    private fun bookFor(stack: ItemStack) =
        bookMetaFor(stack)?.let { (theology) -> books[theology] }
    private fun pageFor(stack: ItemStack) =
        pageMetaFor(stack)?.let { (theology, action) -> books[theology]?.let { it.pages[action] } }

    fun handleBookUse(userEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val book = bookFor(stack) ?: return ActionResult.PASS
        return book.use(userEntity).also { if (it.shouldIncrementStat()) stack.count-- }
    }
    fun handlePageUse(userEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val page = pageFor(stack) ?: return ActionResult.PASS
        return page.use(userEntity).also { if (it.shouldIncrementStat()) stack.count-- }
    }
    fun handlePageAttack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val page = pageFor(stack) ?: return ActionResult.PASS
        return page.attack(userEntity, victimEntity).also { if (it.shouldIncrementStat()) stack.count-- }
    }

    data class BookMeta(val theology: Theology) {
        fun toNbt() = nbtCompoundOf("MsdBook" to true, "MsdBookTheology" to theology)
    }
    data class PageMeta(val theology: Theology, val action: Action) {
        fun toNbt() = nbtCompoundOf("MsdPage" to true, "MsdPageTheology" to theology, "MsdPageAction" to action)
    }

    companion object {
        fun bookMetaFor(stack: ItemStack) = bookMetaFor(stack.nbt)
        fun bookMetaFor(nbt: NbtCompound?): BookMeta? =
            try { BookMeta(nbt!!["MsdBookTheology"].toEnum()) }
            catch (ignored : RuntimeException) { null }

        fun pageMetaFor(stack: ItemStack) = pageMetaFor(stack.nbt)
        fun pageMetaFor(nbt: NbtCompound?): PageMeta? =
            try { PageMeta(nbt!!["MsdPageTheology"].toEnum(), nbt["MsdPageAction"].toEnum()) }
            catch (ignored : RuntimeException) { null }

        private fun loreWithDeep() = text("combine with a ") + text("deep summon page").format(DEEP.color) + " to "
        private fun loreWithOccult() = text("combine with an ") + text("occult summon page").format(OCCULT.color) + " to "
        private fun loreWithCosmos() = text("combine with a ") + text("cosmos summon page").format(COSMOS.color) + " to "
        private fun loreWithBarter() = text("combine with a ") + text("barter summon page").format(BARTER.color) + " to "
        private fun loreWithFlame() = text("combine with a ") + text("flame summon page").format(FLAME.color) + " to "
    }

    init {

        object : Book(DEEP, GameItems.bookDeep) { init {

            object : Page(
                SUMMON, text("deep summon page") * DEEP.color,
                loreWithDeep() + text("flood the map").format(DEEP.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithOccult() + text("receive a ") + text("player-tracking compass").bold() + "!",
                loreWithCosmos() + text("summon acid rain!"),
                loreWithBarter() + text("poison all water!"),
                loreWithFlame() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Deep"),
                text("right-click to gain ") + text("1 heart").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("1 heart").bold() + " of damage!",
            ) {
                override fun use(userEntity: ServerPlayerEntity): ActionResult {
                    if (userEntity.healHearts(1.0)) {
                        userEntity.play(SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS)
                        return ActionResult.SUCCESS
                    }
                    return ActionResult.PASS
                }
                override fun attack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
                    if (victimEntity.hurtHearts(1.0) { it.playerAttack(userEntity) }) {
                        userEntity.play(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS)
                        return ActionResult.SUCCESS
                    }
                    return ActionResult.PASS
                }
            }

            object : Page(
                REGEN, text("Something Katara Read"),
                text("right-click to activate!"),
                text("gain regen for ") + text("15 seconds").bold() + "!",
                text("drown for ") + text("10 seconds").bold() + "!",
            ) {}

            object : Page(
                AREA, text("Claustrophilia: Deep"),
                text("right-click to activate!"),
                text("turn all blocks in a ") + text("3 block radius").bold() + " into water!",
            ) {}

            object : Page(
                BUSTED, text("The Ocean; in Writing"),
                text("right-click to activate!"),
                text("allows the user to swim in air!"),
                text("once activated, the user will begin to drown").bold() + "!",
            ) {}

        } }

        object : Book(OCCULT, GameItems.bookOccult) { init {

            object : Page(
                SUMMON, text("occult summon page") * OCCULT.color,
                loreWithDeep() + text("receive a ") + text("player-tracking compass").bold() + "!",
                loreWithOccult() + text("nearly kill your opps and save all black players").format(OCCULT.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithCosmos() + text("majora the storm's center"),
                loreWithBarter() + text("receive an OP sword!"),
                loreWithFlame() + text("spawn ") + text("3 ghasts").bold() + "!",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Occult"),
                text("right-click to gain ") + text("1 heart").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("4 hearts").bold() + " of damage!",
            ) {
                override fun use(userEntity: ServerPlayerEntity): ActionResult {
                    userEntity.healHearts(1.0)
                    return ActionResult.SUCCESS
                }
                override fun attack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
                    victimEntity.hurtHearts(4.0) { it.playerAttack(userEntity) }
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                REGEN, text("Death's Love Song"),
                text("right-click to activate!"),
                text("for ") + text("10 seconds").bold() + ", gain " + text("1.5 hearts").bold() + " upon taking damage!",
            ) {}

            object : Page(
                AREA, text("Claustrophilia: Occult"),
                text("right-click to activate!"),
                text("cause all players in a ") + text("5 block radius").bold() + " to freeze in place!",
                text("lasts ") + text("10 seconds!").bold() + "!",
            ) {}

            object : Page(
                BUSTED, text("Gruesome Gospel"),
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
            ) {}

        } }

        object : Book(COSMOS, GameItems.bookCosmos) { init {

            object : Page(
                SUMMON, text("cosmos summon page") * COSMOS.color,
                loreWithDeep() + text("poison all water!"),
                loreWithOccult() + text("receive an OP sword!"),
                loreWithCosmos() + text("receive ") + text("8 steak").bold() + "!",
                loreWithBarter() + text("destroy all special items").formatted(BARTER.color) + "! (" + text("requires soul").bold().italic() + ")",
                loreWithFlame() + text("receive a stack of ") + text("blue ice").bold() + "!",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Cosmos"),
                text("right-click to gain ") + text("2.5 hearts").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("2.5 hearts").bold() + " of damage!",
            ) {
                override fun use(userEntity: ServerPlayerEntity): ActionResult {
                    userEntity.healHearts(2.5)
                    return ActionResult.SUCCESS
                }
                override fun attack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
                    victimEntity.hurtHearts(2.5) { it.playerAttack(userEntity) }
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                REGEN, text("Found Superman"),
                text("right-click to activate!"),
                text("gain ") + text("4 absorption hearts").bold() + "!",
                text("gain ") + text("absorption") + "!",
                text("lose the ability to heal for the rest of the round") + "!",
            ) {}

            object : Page(
                AREA, text("Claustrophilia: Cosmos"),
                text("right-click to activate!"),
                text("SUCK all players in a ") + text("5 block radius").bold() + "!",
                text("will not teleport players through walls!"),
            ) {}

            object : Page(
                BUSTED, text("Gruesome Gospel"),
                text("right-click to activate!"),
                text("all teammates gain night vision for the remainder of the round!"),
                text("all opponents gain blindness for the remainder of the round!"),
                text("receive ") + text("10 family guy blocks").bold() + "!",
                text("receive ") + text("5 flint & steel").bold() + "!",
                text("user dies upon activation").bold() + "!",
            ) {}

        } }

        object : Book(BARTER, GameItems.bookBarter) { init {

            object : Page(
                SUMMON, text("barter summon page") * BARTER.color,
                loreWithDeep() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
                loreWithOccult() + text("spawn ") + text("3 ghasts").bold() + "!",
                loreWithCosmos() + text("spawn fire at the storm's center!"),
                loreWithBarter() + text("receive a stack of ") + text("blue ice").bold() + "!",
                loreWithFlame() + text("make every block flammable").format(FLAME.color) + "! (" + text("requires soul").bold().italic() + ")",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Barter"),
                text("right-click to gain ") + text("2.5 hearts").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("2.5 hearts").bold() + " of damage!",
                text("both effects have a 25% chance to backfire").bold() + "!",
            ) {
                override fun use(userEntity: ServerPlayerEntity): ActionResult {
                    // TODO: Backfire
                    userEntity.healHearts(2.5)
                    return ActionResult.SUCCESS
                }
                override fun attack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
                    // TODO: Backfire
                    victimEntity.hurtHearts(2.5) { it.playerAttack(userEntity) }
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                REGEN, text("Das Stoks Baybee"),
                text("right-click to activate!"),
                text("heal ") + text("1 heart").bold() + " for every connecting full-swing hit!",
                text("take ") + text("1 heart of damage").bold() + " for every connecting incomplete hit!",
                text("take ") + text("1 heart of damage").bold() + " for every disconnecting hit!",
                text("lasts ") + text("30 seconds").bold() + "!",
            ) {}

            object : Page(
                AREA, text("Claustrophilia: Barter"),
                text("right-click to activate!"),
                text("launch all players in a ") + text("5 block radius").bold() + "!",
            ) {}

            object : Page(
                BUSTED, text("Midas’ Records"),
                text("right-click to activate!"),
                text("ghostable blocks disappear upon contact!"),
                text("user becomes a ghost upon death or round end").bold() + "!",
            ) {}

        } }

        object : Book(FLAME, GameItems.bookFlame) { init {

            object : Page(
                SUMMON, text("flame summon page") * FLAME.color,
                loreWithDeep() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
                loreWithOccult() + text("spawn ") + text("3 ghasts").bold() + "!",
                loreWithCosmos() + text("spawn fire at the storm's center!"),
                loreWithBarter() + text("receive a stack of ") + text("blue ice").bold() + "!",
                loreWithFlame() + text("make every block flammable").format(FLAME.color) + "! (" + text("requires soul").bold().italic() + ")",
            ) {}

            object : Page(
                HEALTH, text("Ambrosia Recipe: Flame"),
                text("right-click to gain ") + text("4 hearts").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("1 heart").bold() + " of damage!",
            ) {
                override fun use(userEntity: ServerPlayerEntity): ActionResult {
                    userEntity.healHearts(4.0)
                    return ActionResult.SUCCESS
                }
                override fun attack(userEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult {
                    victimEntity.hurtHearts(1.0) { it.playerAttack(userEntity) }
                    return ActionResult.SUCCESS
                }
            }

            object : Page(
                REGEN, text("Sun's At-Home Workout"),
                text("right-click to activate!"),
                text("gain regen for ") + text("15 seconds").bold() + "!",
                text("burn for ") + text("10 seconds").bold() + "!",
            ) {}

            object : Page(
                AREA, text("Claustrophilia: Flame"),
                text("right-click to activate!"),
                text("turn all blocks in a ") + text("4 block radius").bold() + " into fire!",
            ) {}

            object : Page(
                BUSTED, text("Burning Infomercial"),
                text("right-click to activate!"),
                text("gain ") + "speed 4" + "!",
                text("gain ") + "haste 2" + "!",
                text("gain ") + "strength 2" + "!",
                text("gain ") + "jump boost 2" + "!",
                text("gain ") + "resistance 1" + "!",
                text("gain ") + "night vision" + "!",
                text("gain permanent wither until death").bold() + "!",
            ) {}

        } }

    }

}
