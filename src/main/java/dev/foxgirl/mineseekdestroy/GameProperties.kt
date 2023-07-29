package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.util.Region
import dev.foxgirl.mineseekdestroy.util.Regions
import dev.foxgirl.mineseekdestroy.util.collect.buildImmutableSet
import dev.foxgirl.mineseekdestroy.util.collect.immutableSetOf
import net.minecraft.block.Block
import net.minecraft.block.Blocks.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d

sealed interface GameProperties {

    val name: String

    val positionSpawn: BlockPos

    val positionBlimp: Position
    val positionArena: Position
    val positionDuel1: Position
    val positionDuel2: Position
    val positionHell: Position

    val templateInventory: BlockPos
    val templateLoottable: BlockPos

    val regionAll: Region
    val regionLegal: Region
    val regionPlayable: Region
    val regionBlimp: Region
    val regionBlimpBalloons: Regions
    val regionBarrierArenaTemplate: Region
    val regionBarrierArenaTarget: Region
    val regionBarrierBlimpTemplate: Region
    val regionBarrierBlimpTarget: Region
    val regionBarrierBlimpBalloonTemplate: Region
    val regionBarrierBlimpBalloonTargets: Regions
    val regionFlood: Region

    val borderSize: Double
    val borderCenter: Position

    val interactableBlocks: Set<Block>
    val inflammableBlocks: Set<Block>
    val unstealableBlocks: Set<Block>

    fun setup(context: GameContext) {}

    object Empty : GameProperties {

        override val name = "none"

        override val positionSpawn = BlockPos(0, 0, 0)

        override val positionBlimp = Vec3d(0.0, 0.0, 0.0)
        override val positionArena = Vec3d(0.0, 0.0, 0.0)
        override val positionDuel1 = Vec3d(0.0, 0.0, 0.0)
        override val positionDuel2 = Vec3d(0.0, 0.0, 0.0)
        override val positionHell = Vec3d(0.0, 0.0, 0.0)

        override val templateInventory = BlockPos(0, 0, 0)
        override val templateLoottable = BlockPos(0, 0, 0)

        override val regionAll = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionLegal = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionPlayable = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBlimp = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBlimpBalloons = Regions()
        override val regionBarrierArenaTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierArenaTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpBalloonTemplate = Region(BlockPos(1, -20, -532), BlockPos(-9, -17, -542))
        override val regionBarrierBlimpBalloonTargets = Regions()
        override val regionFlood = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))

        override val borderSize = 0.0
        override val borderCenter = Vec3d(0.0, 0.0, 0.0)

        override val interactableBlocks = immutableSetOf<Block>(
            CHEST,
            BARREL,
            SMOKER,
            SHULKER_BOX,
            WHITE_SHULKER_BOX,
            ORANGE_SHULKER_BOX,
            MAGENTA_SHULKER_BOX,
            LIGHT_BLUE_SHULKER_BOX,
            YELLOW_SHULKER_BOX,
            LIME_SHULKER_BOX,
            PINK_SHULKER_BOX,
            GRAY_SHULKER_BOX,
            LIGHT_GRAY_SHULKER_BOX,
            CYAN_SHULKER_BOX,
            PURPLE_SHULKER_BOX,
            BLUE_SHULKER_BOX,
            BROWN_SHULKER_BOX,
            GREEN_SHULKER_BOX,
            RED_SHULKER_BOX,
            BLACK_SHULKER_BOX,
            FLETCHING_TABLE,
            QUARTZ_SLAB,
            LEVER,
        )

        override val inflammableBlocks = buildImmutableSet<Block> {
            addAll(interactableBlocks)
            addAll(listOf(
                FIRE, AIR, CAVE_AIR,
                ACACIA_TRAPDOOR,
                BEDROCK,
                BLACKSTONE_WALL,
                CHISELED_NETHER_BRICKS,
                COARSE_DIRT,
                CRYING_OBSIDIAN,
                DARK_OAK_FENCE,
                DARK_OAK_SLAB,
                DARK_OAK_STAIRS,
                DARK_OAK_TRAPDOOR,
                DIRT,
                DIRT_PATH,
                FARMLAND,
                FLETCHING_TABLE,
                GOLD_BLOCK,
                GRASS_BLOCK,
                GRAVEL,
                IRON_BARS,
                LIME_STAINED_GLASS,
                LOOM,
                NETHER_BRICKS,
                NETHER_BRICK_FENCE,
                NETHER_BRICK_WALL,
                OBSIDIAN,
                PODZOL,
                PRISMARINE_STAIRS,
                PRISMARINE_WALL,
                QUARTZ_SLAB,
                RAW_GOLD_BLOCK,
                REDSTONE_BLOCK,
                REDSTONE_LAMP,
                ROOTED_DIRT,
                SAND,
                SMOKER,
                STONE,
                WARPED_FENCE,
                WARPED_PLANKS,
                WARPED_STAIRS,
                WAXED_CUT_COPPER,
                WAXED_CUT_COPPER_STAIRS,
            ))
        }

        override val unstealableBlocks = buildImmutableSet<Block> {
            addAll(interactableBlocks)
            addAll(listOf(
                BEACON,
                DARK_OAK_TRAPDOOR,
                DIAMOND_BLOCK,
                IRON_BLOCK,
                NETHER_BRICKS,
                OBSIDIAN,
                WARPED_PLANKS,
                WAXED_CUT_COPPER,
            ))
        }

    }

    object Macander : GameProperties {

        override val name = "macander"

        override val positionSpawn = BlockPos(144, -39, -55)

        override val positionBlimp = Vec3d(70.5, 11.0, -55.5)
        override val positionArena = Vec3d(70.5, -39.0, -55.5)
        override val positionDuel1 = Vec3d(70.5, -39.0, -71.5)
        override val positionDuel2 = Vec3d(70.5, -39.0, -39.5)
        override val positionHell = Vec3d(70.5, -65536.0, -55.5)

        override val templateInventory = BlockPos(69, 11, -72)
        override val templateLoottable = BlockPos(69, 11, -74)

        override val regionAll = Region(BlockPos(-24, 35, 51), BlockPos(175, -61, -169))
        override val regionLegal = Region(BlockPos(-24, 65536, 51), BlockPos(175, -56, -169))
        override val regionPlayable = Region(BlockPos(-24, 3, 51), BlockPos(175, -56, -169))
        override val regionBlimp = Region(BlockPos(50, 10, -24), BlockPos(90, 29, -101))
        override val regionBlimpBalloons = Regions()
        override val regionBarrierArenaTemplate = Region(BlockPos(48, -30, -605), BlockPos(92, -47, -539))
        override val regionBarrierArenaTarget = Region(BlockPos(48, -30, -89), BlockPos(92, -47, -23))
        override val regionBarrierBlimpTemplate = Region(BlockPos(63, 7, -558), BlockPos(77, -1, -583))
        override val regionBarrierBlimpTarget = Region(BlockPos(63, 17, -42), BlockPos(77, 9, -67))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Regions()
        override val regionFlood = Region(BlockPos(-24, -50, 51), BlockPos(175, -30, -169))

        override val borderSize = 200.0
        override val borderCenter = Vec3d(70.5, 0.0, -55.5)

        override val interactableBlocks = buildImmutableSet<Block> {
            addAll(Empty.interactableBlocks)
            addAll(listOf(
                ACACIA_DOOR,
                BIRCH_DOOR,
                DARK_OAK_DOOR,
                JUNGLE_DOOR,
                MANGROVE_DOOR,
                OAK_DOOR,
                SPRUCE_DOOR,
                WARPED_DOOR,
                BIRCH_TRAPDOOR,
                MANGROVE_FENCE_GATE,
            ))
        }

        override val inflammableBlocks = buildImmutableSet<Block> {
            addAll(Empty.inflammableBlocks)
            addAll(listOf(
                BARREL,
                BRICKS,
                SAND,
            ))
        }

        override val unstealableBlocks = Empty.unstealableBlocks

    }

    object Radiator : GameProperties {

        override val name = "radiator"

        override val positionSpawn = BlockPos(870, 4, -46)

        override val positionBlimp = Vec3d(870.5, 55.0, -40.5)
        override val positionArena = Vec3d(870.5, 5.0, -40.5)
        override val positionDuel1 = Vec3d(837.5, 44.0, -91.5)
        override val positionDuel2 = Vec3d(901.5, 44.0, -91.5)
        override val positionHell = Vec3d(870.5, -65536.0, -40.5)

        override val templateInventory = BlockPos(886, 55, -41)
        override val templateLoottable = BlockPos(888, 55, -41)

        override val regionAll = Region(BlockPos(788, 57, -170), BlockPos(974, -7, 30))
        override val regionLegal = Region(BlockPos(788, 65536, -170), BlockPos(974, -7, 30))
        override val regionPlayable = Region(BlockPos(788, 75, -170), BlockPos(974, -7, 30))
        override val regionBlimp = Region(BlockPos(918, 80, -16), BlockPos(835, 53, -62))
        override val regionBlimpBalloons = Regions()
        override val regionBarrierArenaTarget = Region(BlockPos(919, -1, -52), BlockPos(806, 35, -125))
        override val regionBarrierArenaTemplate = Region(BlockPos(121, -18, -663), BlockPos(8, 18, -736))
        override val regionBarrierBlimpTarget = Region(BlockPos(856, 61, -33), BlockPos(881, 53, -47))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Regions()
        override val regionFlood = Region(BlockPos(788, -4, -170), BlockPos(974, 13, 30))

        override val borderSize = 250.0
        override val borderCenter = Vec3d(870.0, 0.0, -65.0)

        override val interactableBlocks = buildImmutableSet<Block> {
            addAll(Empty.interactableBlocks)
            addAll(listOf(
                ACACIA_DOOR,
                BIRCH_DOOR,
                DARK_OAK_DOOR,
                JUNGLE_DOOR,
                MANGROVE_DOOR,
                OAK_DOOR,
                SPRUCE_DOOR,
                CRIMSON_DOOR,
                WARPED_DOOR,
                DARK_OAK_TRAPDOOR,
            ))
        }

        override val inflammableBlocks = buildImmutableSet<Block> {
            addAll(Empty.inflammableBlocks)
            addAll(listOf(
                DEEPSLATE_TILE_SLAB,
                PODZOL,
                SANDSTONE_WALL,
            ))
        }

        override val unstealableBlocks = buildImmutableSet<Block> {
            addAll(Empty.unstealableBlocks)
            add(DEEPSLATE_TILE_SLAB)
        }

    }

    object Realm : GameProperties {

        override val name = "realm"

        override val positionSpawn = BlockPos(-1249, 63, 197)

        override val positionBlimp = Vec3d(-1293.5, 114.0, 197.5)
        override val positionArena = Vec3d(-1293.5, 63.0, 197.5)
        override val positionDuel1 = Vec3d(-1240.5, 63.0, 190.5)
        override val positionDuel2 = Vec3d(-1271.5, 63.0, 205.5)
        override val positionHell = Vec3d(-1293.5, -65536.0, 197.5)

        override val templateInventory = BlockPos(-1278, 114, 196)
        override val templateLoottable = BlockPos(-1276, 114, 196)

        override val regionAll = Region(BlockPos(-1430, -16, 350), BlockPos(-1125, 165, 15))
        override val regionLegal = Region(BlockPos(-1600, 65536, 591), BlockPos(-945, -64, -192))
        override val regionPlayable = Region(BlockPos(-1600, 112, 591), BlockPos(-945, -8, -192))
        override val regionBlimp = Region(BlockPos(-1326, 133, 177), BlockPos(-1249, 113, 216))
        override val regionBlimpBalloons = Regions()
        override val regionBarrierArenaTarget = Region(BlockPos(-1238, 71, 187), BlockPos(-1275, 63, 208))
        override val regionBarrierArenaTemplate = Region(BlockPos(88, 8, -618), BlockPos(51, 0, -597))
        override val regionBarrierBlimpTarget = Region(BlockPos(-1283, 112, 190), BlockPos(-1308, 120, 204))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Regions()
        override val regionFlood = Region(BlockPos(-1125, -16, 20), BlockPos(-1440, 67, 355))

        override val borderSize = 333.0
        override val borderCenter = Region(BlockPos(-1280, 62, 172), BlockPos(-1309, 62, 145)).center

        override val interactableBlocks = buildImmutableSet<Block> {
            addAll(Empty.interactableBlocks)
            addAll(listOf(
                ACACIA_DOOR,
                BIRCH_DOOR,
                DARK_OAK_DOOR,
                JUNGLE_DOOR,
                MANGROVE_DOOR,
                OAK_DOOR,
                SPRUCE_DOOR,
                CRIMSON_DOOR,
                WARPED_DOOR,
                OAK_FENCE_GATE,
                CRIMSON_FENCE_GATE,
                WARPED_FENCE_GATE,
            ))
        }

        override val inflammableBlocks = buildImmutableSet<Block> {
            addAll(Empty.inflammableBlocks)
            addAll(listOf(
                ANDESITE,
                CAMPFIRE,
                COBBLESTONE,
                NETHERRACK,
                OAK_LOG,
            ))
        }

        override val unstealableBlocks = Empty.unstealableBlocks

    }

    object Lights : GameProperties {

        override val name = "lights"

        override val positionSpawn = BlockPos(-110, -56, 760)

        override val positionBlimp = Vec3d(-111.5, -15.0, 774.5)
        override val positionArena = Vec3d(-111.5, -56.0, 774.5)
        override val positionDuel1 = Vec3d(-125.5, -52.0, 851.5)
        override val positionDuel2 = Vec3d(-100.5, -52.0, 851.5)
        override val positionHell = Vec3d(-111.5, -65536.0, 774.5)

        override val templateInventory = BlockPos(-96, -15, 773)
        override val templateLoottable = BlockPos(-94, -15, 773)

        override val regionAll = Region(BlockPos(-46, -66, 724), BlockPos(-181, -26, 897))
        override val regionLegal = Region(BlockPos(-46, -66, 724), BlockPos(-181, 65536, 897))
        override val regionPlayable = Region(BlockPos(-46, -66, 724), BlockPos(-181, -26, 897))
        override val regionBlimp = Region(BlockPos(-67, 4, 754), BlockPos(-144, -16, 794))
        override val regionBlimpBalloons = Regions()
        override val regionBarrierArenaTarget = Region(BlockPos(-96, -39, 840), BlockPos(-129, -54, 862))
        override val regionBarrierArenaTemplate = Region(BlockPos(41, 12, -574), BlockPos(8, -3, -552))
        override val regionBarrierBlimpTarget = Region(BlockPos(-101, -17, 767), BlockPos(-126, -9, 781))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Regions()
        override val regionFlood = Region(BlockPos(-18, -64, 701), BlockPos(-206, -52, 913))

        override val borderSize = 200.0
        override val borderCenter = Vec3d(-112.0, -52.0, 828.5)

        override val interactableBlocks = buildImmutableSet<Block> {
            addAll(Empty.interactableBlocks)
            addAll(listOf(
                ACACIA_DOOR,
                BIRCH_DOOR,
                DARK_OAK_DOOR,
                JUNGLE_DOOR,
                MANGROVE_DOOR,
                OAK_DOOR,
                SPRUCE_DOOR,
                CRIMSON_DOOR,
                WARPED_DOOR,
                OAK_FENCE_GATE,
                CRIMSON_FENCE_GATE,
                WARPED_FENCE_GATE,
            ))
        }

        override val inflammableBlocks = buildImmutableSet<Block> {
            addAll(Empty.inflammableBlocks)
            addAll(listOf(
                GRAVEL, COARSE_DIRT, PODZOL,
                STONE, STONE_SLAB, STONE_STAIRS,
                ANDESITE, ANDESITE_SLAB, ANDESITE_STAIRS,
            ))
        }

        override val unstealableBlocks = Empty.unstealableBlocks

    }

    object Station : GameProperties {

        override val name = "station"

        override val positionSpawn = BlockPos(67, -3, -1149)

        override val positionBlimp = Vec3d(-9.5, 49.0, -1147.5)
        override val positionArena = Vec3d(67.5, -3.0, -1148.5)
        override val positionDuel1 = Vec3d(15.5, -3.0, -1251.5)
        override val positionDuel2 = Vec3d(-36.5, -3.0, -1251.5)
        override val positionHell = Vec3d(67.5, -65536.0, -1148.5)

        override val templateInventory = BlockPos(6, 49, -1149)
        override val templateLoottable = BlockPos(8, 49, -1149)

        override val regionAll = Region(BlockPos(159, -30, -1328), BlockPos(-152, 100, -1065))
        override val regionLegal = Region(BlockPos(159, -30, -1328), BlockPos(-152, 65536, -1065))
        override val regionPlayable = Region(BlockPos(159, -30, -1328), BlockPos(-152, 100, -1065))
        override val regionBlimp = Region(BlockPos(-42, 68, -1168), BlockPos(35, 48, -1129))
        override val regionBlimpBalloons = Regions()
        override val regionBarrierArenaTarget = Region(BlockPos(-47, 1, -1269), BlockPos(25, -3, -1235))
        override val regionBarrierArenaTemplate = Region(BlockPos(26, 9, -595), BlockPos(-46, 5, -629))
        override val regionBarrierBlimpTarget = Region(BlockPos(1, 47, -1155), BlockPos(-24, 55, -1141))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Regions()
        override val regionFlood = Region(BlockPos(159, -16, -1328), BlockPos(-152, -2, -1065))

        override val borderSize = 275.0
        override val borderCenter = Vec3d(4.5, 17.0, -1193.5)

        override val interactableBlocks = buildImmutableSet<Block> {
            addAll(Empty.interactableBlocks)
            addAll(listOf(
                ACACIA_DOOR,
                BAMBOO_DOOR,
                BIRCH_DOOR,
                CHERRY_DOOR,
                CRIMSON_DOOR,
                DARK_OAK_DOOR,
                JUNGLE_DOOR,
                MANGROVE_DOOR,
                OAK_DOOR,
                SPRUCE_DOOR,
                WARPED_DOOR,
            ))
        }

        override val inflammableBlocks = buildImmutableSet<Block> {
            addAll(Empty.inflammableBlocks)
            addAll(listOf(
                SMOOTH_STONE, STONE, ANDESITE, COBBLESTONE,
                WHITE_CONCRETE, DARK_PRISMARINE, BLACKSTONE,
                WHITE_TERRACOTTA, LIME_TERRACOTTA, BLUE_TERRACOTTA, MAGENTA_TERRACOTTA,
            ))
        }

        override val unstealableBlocks = buildImmutableSet<Block> {
            addAll(Empty.unstealableBlocks)
            addAll(listOf(
                PINK_GLAZED_TERRACOTTA,
                GREEN_GLAZED_TERRACOTTA,
            ))
        }

    }

}
