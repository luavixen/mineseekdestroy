package dev.foxgirl.mineseekdestroy.service

import dev.foxgirl.mineseekdestroy.Game
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
import net.minecraft.text.Text
import net.minecraft.util.ActionResult

class PagesService : Service() {

    enum class Action { SUMMON, HEALTH, REGEN, AREA, BUSTED }

    private val books = enumMapOf<Theology, Book>()

    private abstract inner class Book(val theology: Theology, val stack: ItemStack) {
        init { check(books.putIfAbsent(theology, this) == null) { "Duplicate book" } }

        val pages = enumMapOf<Action, Page>()

        abstract inner class Page(val action: Action, name: Text, vararg lore: Text) {
            init { check(pages.putIfAbsent(action, this) == null) { "Duplicate page" } }

            val stack = stackOf(Items.PAPER, PageMeta(theology, action).toNbt(), name, lore.asList())

            open fun use(playerEntity: ServerPlayerEntity): ActionResult = ActionResult.PASS
            open fun attack(playerEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity): ActionResult = ActionResult.PASS
        }

        fun use(playerEntity: ServerPlayerEntity): ActionResult {
            Game.CONSOLE_PLAYERS.sendInfo(playerEntity.displayName, "used book", stack.name(), "with theology", theology)
            /*
            Async.run {
                val pages = pages.values.toList()
                delay(0.666)
                playerEntity.give(pages[1].stack)
                Game.CONSOLE_PLAYERS.sendInfo(playerEntity.displayName, "got page", pages[1].stack.name())
                delay(0.666)
                playerEntity.give(pages[2].stack)
                Game.CONSOLE_PLAYERS.sendInfo(playerEntity.displayName, "got page", pages[2].stack.name())
                delay(0.666)
                playerEntity.give(pages[0].stack)
                Game.CONSOLE_PLAYERS.sendInfo(playerEntity.displayName, "got page", pages[0].stack.name())
            }
            return ActionResult.SUCCESS
            */
            return ActionResult.PASS
        }
    }

    private fun bookFor(stack: ItemStack) =
        bookMetaFor(stack)?.let { (theology) -> books[theology] }
    private fun pageFor(stack: ItemStack) =
        pageMetaFor(stack)?.let { (theology, action) -> books[theology]?.let { it.pages[action] } }

    fun handleBookUse(playerEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val book = bookFor(stack) ?: return ActionResult.PASS
        return book.use(playerEntity).also { if (it.shouldIncrementStat()) stack.count-- }
    }
    fun handlePageUse(playerEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val page = pageFor(stack) ?: return ActionResult.PASS
        return page.use(playerEntity).also { if (it.shouldIncrementStat()) stack.count-- }
    }
    fun handlePageAttack(playerEntity: ServerPlayerEntity, victimEntity: ServerPlayerEntity, stack: ItemStack): ActionResult {
        val page = pageFor(stack) ?: return ActionResult.PASS
        return page.attack(playerEntity, victimEntity).also { if (it.shouldIncrementStat()) stack.count-- }
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
            ) {}

            object : Page(
                REGEN, text("Something Katara Read"),
                text("right-click to activate!"),
                text("gain regen for ") + text("15 seconds").bold() + "!",
                text("drown for ") + text("10 seconds").bold() + "!",
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

        } }

    }

}
