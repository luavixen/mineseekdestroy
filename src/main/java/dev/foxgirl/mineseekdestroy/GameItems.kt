package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.service.SummonsService
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theologies
import dev.foxgirl.mineseekdestroy.service.SummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.buildImmutableMap
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import net.minecraft.text.Text
import kotlin.reflect.KProperty0

object GameItems {

    val toolSkull = stackOf(
        SKELETON_SKULL, SKELETON_SKULL.name.copy().white(),
        text("useless! useless! ghost dumb bitch!"),
    )
    val toolAxe = stackOf(
        IRON_AXE, null,
        text("OWIE!"),
        text("affects ghosts").bold(),
    )
    val toolSword = stackOf(
        IRON_SWORD, null,
        text("owie!"),
        text("affects ghosts").bold(),
    )
    val toolShovel = stackOf(
        IRON_SHOVEL, null,
        text("best for mining ") + text("concrete powder").bold(),
    )
    val toolPickaxe = stackOf(
        IRON_PICKAXE, null,
        text("best for mining ") + text("concrete").bold(),
    )
    val toolCrossbow = stackOf(
        CROSSBOW, null,
        text("PEW!"),
        text("does not affect ghosts").bold(),
    )
    val toolBow = stackOf(
        BOW, null,
        text("pew!"),
        text("does not affect ghosts").bold(),
    )
    val toolTrident = stackOf(
        TRIDENT, null,
        text("have fun with this"),
        text("does not affect ghosts").bold(),
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
        text("can be cooked at ") + text("shrines").bold(),
    )
    val bakedPotato = stackOf(
        BAKED_POTATO, null,
        text("funner to eat!"),
        text("was cooked at a ") + text("shrine").bold(),
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

    val flintAndSteel = stackOf(
        FLINT_AND_STEEL, FLINT_AND_STEEL.name(),
        text("FFFFIIIIIIREEEEEEE!"),
    )
    val enderPearl = stackOf(
        ENDER_PEARL, ENDER_PEARL.name(),
        text("if it lands near a player, it will teleport them to you!").yellow(),
        text("functions normally for blue!").blue(),
        text("functions normally for black and duels!").teamBlack(),
    )
    val familyGuyBlock = stackOf(
        TARGET, text("Family Guy Block"),
        text("instantly spawns a concrete structure!"),
        text("contains tnt; can be lit & blown up"),
    )
    val shield = stackOf(
        SHIELD, nbtDecode("{BlockEntityTag:{id:\"minecraft:banner\",Base:11}}").asCompound(),
        SHIELD.name().blue(),
        text("will not function for yellow!").yellow(),
        text("functions normally for blue!").blue(),
        text("functions normally for black and duels!").teamBlack(),
    )
    val fireworkRocket = stackOf(
        FIREWORK_ROCKET, nbtDecode("{Fireworks:{Explosions:[{Colors:[I;14602026],Flicker:1b,Trail:1b,Type:4b}],Flight:2b}}").asCompound(),
        text("Crossbow Rocket").yellow(),
        text("can be shot out of yellow’s crossbows!").yellow(),
        text("will damage blue if used to fly!").blue(),
    )
    val splashPotionSlowness = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_slowness")),
        null,
        text("break their legs!"),
        text("affects ghosts").bold(),
    )
    val splashPotionPoison = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_poison")),
        null,
        text("will whittle your opponent down to half a heart!"),
        text("affects ghosts").bold(),
    )
    val splashPotionHarming = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_harming")),
        null,
        text("OOOWWUH"),
        text("affects ghosts").bold(),
    )

    val bookDeep = stackOf(
        WRITTEN_BOOK, nbtCompoundOf("MsdBook" to DEEP),
        text("Sunken Scroll") * DEEP.color,
        text("right-click to receive 3 random ") + text("deep pages").format(DEEP.color) + "!",
    )
    val bookOccult = stackOf(
        WRITTEN_BOOK, nbtCompoundOf("MsdBook" to OCCULT),
        text("Hymnal") * OCCULT.color,
        text("right-click to receive 3 random ") + text("occult pages").format(OCCULT.color) + "!",
    )
    val bookCosmos = stackOf(
        WRITTEN_BOOK, nbtCompoundOf("MsdBook" to COSMOS),
        text("Golden Disc") * COSMOS.color,
        text("right-click to receive 3 random ") + text("cosmos pages").format(COSMOS.color) + "!",
    )
    val bookBarter = stackOf(
        WRITTEN_BOOK, nbtCompoundOf("MsdBook" to BARTER),
        text("File Folder") * BARTER.color,
        text("right-click to receive 3 random ") + text("barter pages").format(BARTER.color) + "!",
    )
    val bookFlame = stackOf(
        WRITTEN_BOOK, nbtCompoundOf("MsdBook" to FLAME),
        text("VHS") * FLAME.color,
        text("right-click to receive 3 random ") + text("flame pages").format(FLAME.color) + "!",
    )

    val pagesDeep: List<ItemStack>
    val pagesOccult: List<ItemStack>
    val pagesCosmos: List<ItemStack>
    val pagesBarter: List<ItemStack>
    val pagesFlame: List<ItemStack>

    init {
        fun page(theology: SummonsService.Theology, action: String, name: Text?, vararg lore: Text) =
            stackOf(PAPER, nbtCompoundOf("MsdPage" to theology, "MsdPageAction" to action), name, lore.asList())

        fun textSummonDeep() = text("combine with a ") + text("deep summon page").format(DEEP.color) + " to "
        fun textSummonOccult() = text("combine with an ") + text("occult summon page").format(OCCULT.color) + " to "
        fun textSummonCosmos() = text("combine with a ") + text("cosmos summon page").format(COSMOS.color) + " to "
        fun textSummonBarter() = text("combine with a ") + text("barter summon page").format(BARTER.color) + " to "
        fun textSummonFlame() = text("combine with a ") + text("flame summon page").format(FLAME.color) + " to "

        pagesDeep = immutableListOf(
            page(
                DEEP, "summon", text("Deep Summon Page") * DEEP.color,
                textSummonDeep() + text("flood the map").format(DEEP.color) + "! (" + text("requires soul").bold().italic() + ")",
                textSummonOccult() + text("receive a ") + text("player-tracking compass").bold() + "!",
                textSummonCosmos() + text("summon acid rain!"),
                textSummonBarter() + text("poison all water!"),
                textSummonFlame() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
            ),
            page(
                DEEP, "health", text("Ambrosia Recipe: Deep"),
                text("right-click to gain ") + text("1 heart").bold() + " of health!",
                text("left-click on an opponent to deal ") + text("1 heart").bold() + " of damage!",
            ),
            page(
                DEEP, "regen", text("Something Katara Read"),
                text("right-click to activate!"),
                text("gain regen for ") + text("15 seconds").bold() + "!",
                text("drown for ") + text("10 seconds").bold() + "!",
            ),
        )
        pagesOccult = immutableListOf(
            page(
                OCCULT, "summon", text("Occult Summon Page") * OCCULT.color,
                textSummonDeep() + text("receive a ") + text("player-tracking compass").bold() + "!",
                textSummonOccult() + text("nearly kill your opps and save all black players").format(OCCULT.color) + "! (" + text("requires soul").bold().italic() + ")",
                textSummonCosmos() + text("majora the storm's center"),
                textSummonBarter() + text("receive an OP sword!"),
                textSummonFlame() + text("spawn ") + text("3 ghasts").bold() + "!",
            ),
        )
        pagesCosmos = immutableListOf(
            page(
                COSMOS, "summon", text("Cosmos Summon Page") * COSMOS.color,
                textSummonDeep() + text("poison all water!"),
                textSummonOccult() + text("receive an OP sword!"),
                textSummonCosmos() + text("receive ") + text("8 steak").bold() + "!",
                textSummonBarter() + text("destroy all special items").formatted(BARTER.color) + "! (" + text("requires soul").bold().italic() + ")",
                textSummonFlame() + text("receive a stack of ") + text("blue ice").bold() + "!",
            ),
        )
        pagesBarter = immutableListOf(
            page(
                BARTER, "summon", text("Barter Summon Page") * BARTER.color,
                textSummonDeep() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
                textSummonOccult() + text("spawn ") + text("3 ghasts").bold() + "!",
                textSummonCosmos() + text("spawn fire at the storm's center!"),
                textSummonBarter() + text("receive a stack of ") + text("blue ice").bold() + "!",
                textSummonFlame() + text("make every block flammable").format(FLAME.color) + "! (" + text("requires soul").bold().italic() + ")",
            ),
        )
        pagesFlame = immutableListOf(
            page(
                FLAME, "summon", text("Flame Summon Page") * FLAME.color,
                textSummonDeep() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
                textSummonOccult() + text("spawn ") + text("3 ghasts").bold() + "!",
                textSummonCosmos() + text("spawn fire at the storm's center!"),
                textSummonBarter() + text("receive a stack of ") + text("blue ice").bold() + "!",
                textSummonFlame() + text("make every block flammable").format(FLAME.color) + "! (" + text("requires soul").bold().italic() + ")",
            ),
        )
    }

    val summonSteak = stackOf(
        COOKED_BEEF, null,
        text("funnest to eat!"),
        text("was gained through ") + Theologies(COSMOS, BARTER).displayName,
    )
    val summonGoldenSword = stackOf(
        GOLDEN_SWORD, null,
        text("will mostly kill someone!"),
        text("was gained through ") + Theologies(OCCULT, BARTER).displayName,
    ).apply {
        addEnchantment(Enchantments.SHARPNESS, 15)
        setDamage(32)
    }
    val summonBlueIce = stackOf(
        BLUE_ICE, null,
        text("its... its just normal ice i swear"),
        text("was gained through ") + Theologies(FLAME, BARTER).displayName,
    )
    val summonWaterBucket = stackOf(
        WATER_BUCKET, null,
        text("wotor"),
        text("was gained through ") + Theologies(DEEP, FLAME).displayName,
    )
    val summonChippedAnvil = stackOf(
        CHIPPED_ANVIL, null,
        text("forge this block onto the HEADS OF YOUR ENEMIES!"),
        text("was gained through ") + Theologies(DEEP, FLAME).displayName,
    )
    val summonCompass = stackOf(
        COMPASS, null,
        text("this item randomly selects an opp. player and tracks em!"),
        text("every compass currently in play tracks a different player!"),
        text("was gained through ") + Theologies(DEEP, OCCULT).displayName,
    )

    val replacements = buildImmutableMap<Item, ItemStack> {
        for (stack in listOf(
            snowBlock, eggBlock, ectoplasm,
            potato, bakedPotato, egg, snowball, spectralArrow,
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
        ::spectralArrow, ::flintAndSteel, ::enderPearl, ::familyGuyBlock,
        ::shield, ::fireworkRocket, ::splashPotionSlowness,
        ::splashPotionPoison, ::splashPotionHarming, ::bookDeep,
        ::bookOccult, ::bookCosmos, ::bookBarter, ::bookFlame,
        ::summonSteak, ::summonGoldenSword, ::summonBlueIce,
        ::summonWaterBucket, ::summonChippedAnvil, ::summonCompass,
    )

}
