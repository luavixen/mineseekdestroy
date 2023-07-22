package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theologies
import dev.foxgirl.mineseekdestroy.service.SpecialSummonsService.Theology.*
import dev.foxgirl.mineseekdestroy.util.*
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableMap
import dev.foxgirl.mineseekdestroy.util.collect.immutableListOf
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items.*
import kotlin.reflect.KProperty0

object GameItems {

    @JvmStatic
    val toolSkull = stackOf(
        SKELETON_SKULL, SKELETON_SKULL.name.copy().reset(),
        text("useless! useless! ghost dumb bitch!"),
    )
    @JvmStatic
    val toolAxe = stackOf(
        IRON_AXE, null,
        text("OWIE!"),
        text("affects ghosts").bold(),
    )
    @JvmStatic
    val toolSword = stackOf(
        IRON_SWORD, null,
        text("owie!"),
        text("affects ghosts").bold(),
    )
    @JvmStatic
    val toolShovel = stackOf(
        IRON_SHOVEL, null,
        text("best for mining ") + text("concrete powder").bold(),
    )
    @JvmStatic
    val toolPickaxe = stackOf(
        IRON_PICKAXE, null,
        text("best for mining ") + text("concrete").bold(),
    )
    @JvmStatic
    val toolCrossbow = stackOf(
        CROSSBOW, null,
        text("PEW!"),
        text("does not affect ghosts").bold(),
    )
    @JvmStatic
    val toolBow = stackOf(
        BOW, null,
        text("pew!"),
        text("does not affect ghosts").bold(),
    )
    @JvmStatic
    val toolTrident = stackOf(
        TRIDENT, null,
        text("have fun with this"),
        text("does not affect ghosts").bold(),
    )

    @JvmStatic
    val snowBlock = stackOf(
        SNOW_BLOCK, text("Snow Block").reset(),
        text("can be uncrafted into ") + text("4 snowballs").bold() + "!",
    )
    @JvmStatic
    val eggBlock = stackOf(
        BONE_BLOCK, text("Egg Block").reset(),
        text("can be uncrafted into ") + text("4 eggs").bold() + "!",
    )
    @JvmStatic
    val ectoplasm = stackOf(
        SLIME_BLOCK, text("Ectogasm"),
        text("its... its just normal slime i swear"),
    )

    @JvmStatic
    val potato = stackOf(
        POTATO, null,
        text("fun to eat!"),
        text("can be cooked at ") + text("shrines").bold(),
    )
    @JvmStatic
    val bakedPotato = stackOf(
        BAKED_POTATO, null,
        text("funner to eat!"),
        text("was cooked at a ") + text("shrine").bold(),
    )
    @JvmStatic
    val egg = stackOf(
        EGG, null,
        text("pulls players towards you!"),
        text("can be crafted in-inv into an ") + text("egg block").bold(),
        text("egg blocks").bold() + text(" are best mined with ") + text("pickaxes").bold(),
    )
    @JvmStatic
    val snowball = stackOf(
        SNOWBALL, null,
        text("pushes players away from you!"),
        text("can be crafted in-inv into a ") + text("snow block").bold(),
        text("snow blocks").bold() + text(" are best mined with ") + text("shovels").bold(),
    )
    @JvmStatic
    val spectralArrow = stackOf(
        SPECTRAL_ARROW, null,
        text("arrow... but glows..."),
        text("does not affect ghosts"),
    )

    @JvmStatic
    val flintAndSteel = stackOf(
        FLINT_AND_STEEL, FLINT_AND_STEEL.name(),
        text("FFFFIIIIIIREEEEEEE!"),
    )
    @JvmStatic
    val enderPearl = stackOf(
        ENDER_PEARL, ENDER_PEARL.name(),
        text("if it lands near a player, it will teleport them to you!").yellow(),
        text("functions normally for blue!").blue(),
        text("functions normally for black and duels!").teamBlack(),
    )
    @JvmStatic
    val familyGuyBlock = stackOf(
        TARGET, text("Family Guy Block"),
        text("instantly spawns a concrete structure!"),
        text("contains tnt; can be lit & blown up"),
    )
    @JvmStatic
    val shield = stackOf(
        SHIELD,
        nbtCompoundOf("BlockEntityTag" to nbtCompoundOf("id" to identifier("banner"), "Base" to 11)),
        SHIELD.name().blue(),
        text("will not function for yellow!").yellow(),
        text("functions normally for blue!").blue(),
        text("functions normally for black and duels!").teamBlack(),
    )
    @JvmStatic
    val fireworkRocket = stackOf(
        FIREWORK_ROCKET,
        nbtCompoundOf("Fireworks" to nbtDecode("{Explosions:[{Colors:[I;14602026],Flicker:1b,Trail:1b,Type:4b}],Flight:2b}")),
        text("Crossbow Rocket").yellow(),
        text("can be shot out of yellow’s crossbows!").yellow(),
        text("will damage blue if used to fly!").blue(),
    )
    @JvmStatic
    val splashPotionSlowness = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_slowness")),
        null,
        text("break their legs!"),
        text("affects ghosts").bold(),
    )
    @JvmStatic
    val splashPotionPoison = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_poison")),
        null,
        text("will whittle your opponent down to half a heart!"),
        text("affects ghosts").bold(),
    )
    @JvmStatic
    val splashPotionHarming = stackOf(
        SPLASH_POTION, nbtCompoundOf("Potion" to identifier("strong_harming")),
        null,
        text("OOOWWUH"),
        text("affects ghosts").bold(),
    )

    @JvmStatic val arrowDeep: ItemStack
    @JvmStatic val arrowOccult: ItemStack
    @JvmStatic val arrowCosmos: ItemStack
    @JvmStatic val arrowBarter: ItemStack
    @JvmStatic val arrowFlame: ItemStack

    init {
        fun withDeep() = text("combine with a ") + text("deep arrow").format(DEEP.color) + " to "
        fun withOccult() = text("combine with an ") + text("occult arrow").format(OCCULT.color) + " to "
        fun withCosmos() = text("combine with a ") + text("cosmos arrow").format(COSMOS.color) + " to "
        fun withBarter() = text("combine with a ") + text("barter arrow").format(BARTER.color) + " to "
        fun withFlame() = text("combine with a ") + text("flame arrow").format(FLAME.color) + " to "

        arrowDeep = stackOf(
            TIPPED_ARROW, nbtCompoundOf("Potion" to identifier("long_water_breathing")),
            text("Arrow of the ") + DEEP.displayName,
            withDeep() + text("flood the map").format(DEEP.color) + "!",
            withOccult() + text("receive a ") + text("player-tracking compass").bold() + "!",
            withCosmos() + text("summon acid rain!"),
            withBarter() + text("poison all water!"),
            withFlame() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
        )
        arrowOccult = stackOf(
            TIPPED_ARROW, nbtCompoundOf("Potion" to identifier("strong_healing")),
            text("Arrow of the ") + OCCULT.displayName,
            withDeep() + text("receive a ") + text("player-tracking compass").bold() + "!",
            withOccult() + text("nearly kill your opps and save all black players").format(OCCULT.color) + "!",
            withCosmos() + text("gain night vision and blind your opponents!"),
            withBarter() + text("receive an OP sword!"),
            withFlame() + text("spawn ") + text("3 ghasts").bold() + "!",
        )
        arrowCosmos = stackOf(
            TIPPED_ARROW, nbtCompoundOf("Potion" to identifier("long_invisibility")),
            text("Arrow of the ") + COSMOS.displayName,
            withDeep() + text("summon acid rain!"),
            withOccult() + text("gain night vision and blind your opponents!"),
            withCosmos() + text("reduce gravity").format(COSMOS.color) + "!",
            withBarter() + text("receive ") + text("8 steak").bold() + "!",
            withFlame() + text("get an absorption heart!"),
        )
        arrowBarter = stackOf(
            TIPPED_ARROW, nbtCompoundOf("Potion" to identifier("strong_strength")),
            text("Arrow of the ") + BARTER.displayName,
            withDeep() + text("poison all water!"),
            withOccult() + text("receive an OP sword!"),
            withCosmos() + text("receive ") + text("8 steak").bold() + "!",
            withBarter() + text("destroy all special items").formatted(BARTER.color) + "!",
            withFlame() + text("receive a stack of ") + text("blue ice").bold() + "!",
        )
        arrowFlame = stackOf(
            TIPPED_ARROW, nbtCompoundOf("Potion" to identifier("long_fire_resistance")),
            text("Arrow of the ") + FLAME.displayName,
            withDeep() + text("receive an ") + text("anvil").bold() + text(" & ") + text("water bucket").bold() + "!",
            withOccult() + text("spawn ") + text("3 ghasts").bold() + "!",
            withCosmos() + text("get an absorption heart!"),
            withBarter() + text("receive a stack of ") + text("blue ice").bold() + "!",
            withFlame() + text("make every block flammable").format(FLAME.color) + "!",
        )
    }

    @JvmStatic
    val summonSteak = stackOf(
        COOKED_BEEF, null,
        text("funnest to eat!"),
        text("was gained through ") + Theologies(COSMOS, BARTER).displayName,
    )
    @JvmStatic
    val summonGoldenSword = stackOf(
        GOLDEN_SWORD, null,
        text("will mostly kill someone!"),
        text("was gained through ") + Theologies(OCCULT, BARTER).displayName,
    ).apply {
        addEnchantment(Enchantments.SHARPNESS, 15)
        setDamage(32)
    }
    @JvmStatic
    val summonBlueIce = stackOf(
        BLUE_ICE, null,
        text("its... its just normal ice i swear"),
        text("was gained through ") + Theologies(FLAME, BARTER).displayName,
    )
    @JvmStatic
    val summonWaterBucket = stackOf(
        WATER_BUCKET, null,
        text("wotor"),
        text("was gained through ") + Theologies(DEEP, FLAME).displayName,
    )
    @JvmStatic
    val summonChippedAnvil = stackOf(
        CHIPPED_ANVIL, null,
        text("forge this block onto the HEADS OF YOUR ENEMIES!"),
        text("was gained through ") + Theologies(DEEP, FLAME).displayName,
    )
    @JvmStatic
    val summonCompass = stackOf(
        COMPASS, null,
        text("this item randomly selects an opp. player and tracks em!"),
        text("every compass currently in play tracks a different player!"),
        text("was gained through ") + Theologies(DEEP, OCCULT).displayName,
    )

    @JvmStatic
    val replacements: Map<Item, ItemStack>
    init {
        val builder = ImmutableMap.builder<Item, ItemStack>(32)
        for (stack in listOf<ItemStack>(
            snowBlock, eggBlock, ectoplasm,
            potato, bakedPotato, egg, snowball, spectralArrow,
            flintAndSteel, enderPearl, familyGuyBlock, fireworkRocket,
        )) {
            builder.put(stack.item, stack)
        }
        replacements = builder.build()
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
    @JvmStatic
    fun replaceCopy(stack: ItemStack): ItemStack {
        return stack.copy().also(::replace)
    }

    val properties = immutableListOf<KProperty0<ItemStack>>(
        ::toolSkull, ::toolAxe, ::toolSword, ::toolShovel, ::toolPickaxe,
        ::toolCrossbow, ::toolBow, ::toolTrident, ::snowBlock, ::eggBlock,
        ::ectoplasm, ::potato, ::bakedPotato, ::egg, ::snowball,
        ::spectralArrow, ::flintAndSteel, ::enderPearl, ::familyGuyBlock,
        ::shield, ::fireworkRocket, ::splashPotionSlowness,
        ::splashPotionPoison, ::splashPotionHarming, ::arrowDeep,
        ::arrowOccult, ::arrowCosmos, ::arrowBarter, ::arrowFlame,
        ::summonSteak, ::summonGoldenSword, ::summonBlueIce,
        ::summonWaterBucket, ::summonChippedAnvil, ::summonCompass,
    )

}
