package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.service.PagesService
import dev.foxgirl.mineseekdestroy.service.SummonsService.Prayer
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.buildImmutableMap
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import kotlin.reflect.KProperty0

object GameItems {

    val toolSkull = stackOf(
        SKELETON_SKULL, nbtCompoundOf("MsdTool" to "skull"),
        SKELETON_SKULL.name.copy().white(),
        text("useless! useless! ghost dumb bitch!"),
    )
    val toolAxe = stackOf(
        IRON_AXE, nbtCompoundOf("MsdTool" to "axe"),
        null,
        text("OWIE!"),
        text("affects ghosts").bold(),
    )
    val toolSword = stackOf(
        IRON_SWORD, nbtCompoundOf("MsdTool" to "sword"),
        null,
        text("owie!"),
        text("affects ghosts").bold(),
    )
    val toolShovel = stackOf(
        IRON_SHOVEL, nbtCompoundOf("MsdTool" to "shovel"),
        null,
        text("best for mining ") + text("concrete powder").bold(),
    )
    val toolPickaxe = stackOf(
        IRON_PICKAXE, nbtCompoundOf("MsdTool" to "pickaxe"),
        null,
        text("best for mining ") + text("concrete").bold(),
    )
    val toolHoe = stackOf(
        IRON_HOE, nbtCompoundOf("MsdTool" to "hoe"),
        null,
        text("just for fun, good luck using this"),
    )
    val toolCrossbow = stackOf(
        CROSSBOW, nbtCompoundOf("MsdTool" to "crossbow"),
        null,
        text("PEW!"),
        text("does not affect ghosts").bold(),
    )
    val toolBow = stackOf(
        BOW, nbtCompoundOf("MsdTool" to "bow"),
        null,
        text("pew!"),
        text("does not affect ghosts").bold(),
    )
    val toolTrident = stackOf(
        TRIDENT, nbtCompoundOf("MsdTool" to "trident"),
        null,
        text("have fun with this"),
        text("does not affect ghosts").bold(),
    ).apply {
        addEnchantment(Enchantments.LOYALTY, 3)
    }
    val toolRod = stackOf(
        FISHING_ROD, nbtCompoundOf("MsdTool" to "rod"),
        null,
        text("yoink!"),
        text("does not affect ghosts").bold(),
    )

    val toolYellowBow = toolBow.copy().apply {
        getOrCreateNbt()["MsdTool"] = "yellowBow"
        addEnchantment(Enchantments.PUNCH, 2)
    }
    val toolYellowSword = toolSword.copy().apply {
        getOrCreateNbt()["MsdTool"] = "yellowSword"
        addEnchantment(Enchantments.KNOCKBACK, 2)
        addEnchantment(Enchantments.SWEEPING, 2)
    }
    val toolBlueCrossbow = toolCrossbow.copy().apply {
        getOrCreateNbt()["MsdTool"] = "blueCrossbow"
        addEnchantment(Enchantments.PIERCING, 4)
        addEnchantment(Enchantments.QUICK_CHARGE, 2)
    }
    val toolBluePickaxe = toolPickaxe.copy().apply {
        getOrCreateNbt()["MsdTool"] = "bluePickaxe"
        addEnchantment(Enchantments.EFFICIENCY, 4)
    }

    val toolYellowConduit = stackOf(
        CONDUIT, nbtCompoundOf("MsdTool" to "yellowConduit", "MsdConduit" to GameTeam.YELLOW),
        text("super magical conduit of power").teamYellow(),
        text("right click to activate! ") + text("(only when full)").bold(),
        text("stop holding to deactivate! ") + text("(only when starving)").bold(),
        text("makes the user immune to all damage except starvation when active").teamYellow(),
        text("saps hunger of user AND nearby enemy players"),
        text("gain weakness 64 while active").bold(),
        text("gain mining fatigue 64 while active").bold(),
    )
    val toolBlueConduit = stackOf(
        CONDUIT, nbtCompoundOf("MsdTool" to "blueConduit", "MsdConduit" to GameTeam.BLUE),
        text("super duper conduit of power").teamBlue(),
        text("right click to activate! ") + text("(only when NOT starving)").bold(),
        text("stop holding to deactivate! ") + text("(any time)").bold(),
        text("removes all blocks around the user above ground level").teamBlue(),
        text("saps hunger of user AND nearby enemy players"),
    )

    val snowBlock = stackOf(
        SNOW_BLOCK, text("Snow Block").white(),
        text("can be uncrafted into ") + text("4 snowballs").bold() + "!",
    )
    val eggBlock = stackOf(
        BONE_BLOCK, text("Egg Block").white(),
        text("can be uncrafted into ") + text("4 eggs").bold() + "!",
    )
    val ectoplasm = stackOf(
        SLIME_BLOCK, text("Ectogasm"),
        text("its... its just normal slime i swear"),
    )

    val potato = stackOf(
        POTATO, null,
        text("fun to eat!"),
    )
    val bakedPotato = stackOf(
        BAKED_POTATO, null,
        text("funner to eat!"),
        text("how'd you get this?"),
    )
    val egg = stackOf(
        EGG, null,
        text("pulls players towards you!"),
        text("can be crafted in-inv into an ") + text("egg block").bold(),
        text("egg blocks").bold() + text(" are best mined with ") + text("pickaxes").bold(),
    )
    val snowball = stackOf(
        SNOWBALL, null,
        text("pushes players away from you!"),
        text("can be crafted in-inv into a ") + text("snow block").bold(),
        text("snow blocks").bold() + text(" are best mined with ") + text("shovels").bold(),
    )
    val spectralArrow = stackOf(
        SPECTRAL_ARROW, null,
        text("arrow... but glows..."),
        text("does not affect ghosts"),
    )
    val tippedArrow = stackOf(
        TIPPED_ARROW, nbtCompoundOf("MsdYellowArrow" to true, "Potion" to identifier("harming")),
        text("Retaliation Arrow").teamYellow(),
        text("functions normally on yellow!").teamYellow(),
        text("cannot be shot by blue players").teamBlue(),
    )

    val flintAndSteel = stackOf(
        FLINT_AND_STEEL, FLINT_AND_STEEL.name(),
        text("FFFFIIIIIIREEEEEEE!"),
    )
    val enderPearl = stackOf(
        ENDER_PEARL, ENDER_PEARL.name(),
        text("if it lands near a player, it will teleport them to you!").teamYellow(),
        text("functions normally for blue!").teamBlue(),
        text("functions normally for black and duels!").teamBlack(),
    )
    val familyGuyBlock = stackOf(
        TARGET, text("Family Guy Block"),
        text("instantly spawns a concrete structure!"),
        text("contains tnt; can be lit & blown up"),
    )
    val shield = stackOf(
        SHIELD, nbtDecode("{BlockEntityTag:{id:\"minecraft:banner\",Base:4}}").asCompound(),
        SHIELD.name().teamYellow(),
        text("functions normally for yellow!").teamYellow(),
        text("will not function for blue!").teamBlue(),
        text("functions normally for black and duels!").teamBlack(),
    )
    val fireworkRocket = stackOf(
        FIREWORK_ROCKET, nbtDecode("{Fireworks:{Explosions:[{Colors:[I;2437522],Flicker:1b,Trail:1b,Type:4b}],Flight:2b}}").asCompound(),
        text("Crossbow Rocket").teamBlue(),
        text("can be shot out of blue's crossbows!").teamBlue(),
        text("can be used by blue to fly, but will damage you!").teamBlue(),
    )
    val potionSlowness = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_slowness")),
        null,
        text("break their legs!"),
        text("affects ghosts").bold(),
    )
    val potionPoison = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_poison")),
        null,
        text("will whittle your opponent down to half a heart!"),
        text("affects ghosts").bold(),
    )
    val potionHarming = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_harming")),
        null,
        text("OOOWWUH"),
        text("affects ghosts").bold(),
    )

    val bookDeep = stackOf(
        WRITTEN_BOOK, PagesService.BookType(DEEP).toNbt(),
        text("Sunken Scroll") * DEEP.color,
        text("right-click to receive 3 random ") + text("deep pages").format(DEEP.color) + "!",
        text("4 random pages of ANY TYPE can be crafted into a ") + text("cobbled book").bold() + "!",
    )
    val bookOccult = stackOf(
        WRITTEN_BOOK, PagesService.BookType(OCCULT).toNbt(),
        text("Hymnal") * OCCULT.color,
        text("right-click to receive 3 random ") + text("occult pages").format(OCCULT.color) + "!",
        text("4 random pages of ANY TYPE can be crafted into a ") + text("cobbled book").bold() + "!",
    )
    val bookCosmos = stackOf(
        WRITTEN_BOOK, PagesService.BookType(COSMOS).toNbt(),
        text("Golden Disc") * COSMOS.color,
        text("right-click to receive 3 random ") + text("cosmos pages").format(COSMOS.color) + "!",
        text("4 random pages of ANY TYPE can be crafted into a ") + text("cobbled book").bold() + "!",
    )
    val bookBarter = stackOf(
        WRITTEN_BOOK, PagesService.BookType(BARTER).toNbt(),
        text("File Folder") * BARTER.color,
        text("right-click to receive 3 random ") + text("barter pages").format(BARTER.color) + "!",
        text("4 random pages of ANY TYPE can be crafted into a ") + text("cobbled book").bold() + "!",
    )
    val bookFlame = stackOf(
        WRITTEN_BOOK, PagesService.BookType(FLAME).toNbt(),
        text("VHS") * FLAME.color,
        text("right-click to receive 3 random ") + text("flame pages").format(FLAME.color) + "!",
        text("4 random pages of ANY TYPE can be crafted into a ") + text("cobbled book").bold() + "!",
    )
    val bookCobbled = stackOf(
        WRITTEN_BOOK, PagesService.BookType(OPERATOR).toNbt() + nbtCompoundOf("MsdBookCobbled" to true),
        text("Cobbled Book"),
        text("if placed in a chest, this book will be replaced with another random book!"),
        text("this only happens the moment a round starts - stash me away asap!"),
    )

    val summonSteak = stackOf(
        COOKED_BEEF, null,
        text("funnest to eat!"),
        text("was gained through ") + Prayer(COSMOS, BARTER).displayName,
    )
    val summonGoldenSword = stackOf(
        GOLDEN_SWORD, null,
        text("will mostly kill someone!"),
        text("was gained through ") + Prayer(OCCULT, BARTER).displayName,
    ).apply {
        addEnchantment(Enchantments.SHARPNESS, 15)
        setDamage(32)
    }
    val summonBlueIce = stackOf(
        BLUE_ICE, null,
        text("its... its just normal ice i swear"),
        text("was gained through ") + Prayer(FLAME, BARTER).displayName,
    )
    val summonWaterBucket = stackOf(
        WATER_BUCKET, null,
        text("wotor"),
        text("was gained through ") + Prayer(DEEP, FLAME).displayName,
    )
    val summonChippedAnvil = stackOf(
        CHIPPED_ANVIL, null,
        text("forge this block onto the HEADS OF YOUR ENEMIES!"),
        text("was gained through ") + Prayer(DEEP, FLAME).displayName,
    )
    val summonCompass = stackOf(
        COMPASS, null,
        text("this item randomly selects an opp. player and tracks em!"),
        text("every compass currently in play tracks a different player!"),
        text("was gained through ") + Prayer(DEEP, OCCULT).displayName,
    )

    val replacements = buildImmutableMap<Item, ItemStack> {
        for (stack in listOf(
            snowBlock, eggBlock, ectoplasm,
            potato, bakedPotato, egg, snowball, spectralArrow, tippedArrow,
            flintAndSteel, enderPearl, familyGuyBlock, fireworkRocket,
        )) {
            put(stack.item, stack)
        }
    }

    @JvmStatic
    fun replace(stack: ItemStack) {
        val replacement = replacements[stack.item]
        if (replacement != null) {
            val replacementNbt = replacement.nbt
            if (replacementNbt != null && replacementNbt != stack.nbt) {
                stack.nbt = replacementNbt.copy()
            }
        }
    }

    val properties = immutableListOf<KProperty0<ItemStack>>(
        ::toolSkull, ::toolAxe, ::toolSword, ::toolShovel, ::toolPickaxe,
        ::toolCrossbow, ::toolBow, ::toolTrident, ::snowBlock, ::eggBlock,
        ::ectoplasm, ::potato, ::bakedPotato, ::egg, ::snowball,
        ::spectralArrow, ::tippedArrow, ::flintAndSteel, ::enderPearl,
        ::familyGuyBlock, ::shield, ::fireworkRocket, ::potionSlowness,
        ::potionPoison, ::potionHarming, ::bookDeep, ::bookOccult,
        ::bookCosmos, ::bookBarter, ::bookFlame, ::summonSteak,
        ::summonGoldenSword, ::summonBlueIce, ::summonWaterBucket,
        ::summonChippedAnvil, ::summonCompass,
    )

}
