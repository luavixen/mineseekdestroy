package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.util.Region
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
    val regionBlimpBalloons: Region.Set
    val regionBlimpFans: Region.Set
    val regionBarrierArenaTemplate: Region
    val regionBarrierArenaTarget: Region
    val regionBarrierBlimpTemplate: Region
    val regionBarrierBlimpTarget: Region
    val regionBarrierBlimpBalloonTemplate: Region
    val regionBarrierBlimpBalloonTargets: Region.Set
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
        override val regionBlimpBalloons = Region.Set()
        override val regionBlimpFans = Region.Set()
        override val regionBarrierArenaTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierArenaTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpBalloonTemplate = Region(BlockPos(1, -20, -532), BlockPos(-9, -17, -542))
        override val regionBarrierBlimpBalloonTargets = Region.Set()
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
                CHISELED_NETHER_BRICKS,
                COARSE_DIRT,
                CRYING_OBSIDIAN,
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
                RAW_GOLD_BLOCK,
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
        override val regionBlimpBalloons = Region.Set(
            Region(BlockPos(43, 13, 1), BlockPos(25, 32, -17)),
            Region(BlockPos(138, 13, -71), BlockPos(120, 32, -89)),
        )
        override val regionBlimpFans = Region.Set(
            Region(BlockPos(59, 20, -44), BlockPos(51, 18, -36)),
            Region(BlockPos(59, 20, -73), BlockPos(51, 18, -36)),
            Region(BlockPos(81, 20, -44), BlockPos(89, 18, -36)),
            Region(BlockPos(81, 20, -73), BlockPos(89, 18, -65)),
        )
        override val regionBarrierArenaTemplate = Region(BlockPos(48, -30, -605), BlockPos(92, -47, -539))
        override val regionBarrierArenaTarget = Region(BlockPos(48, -30, -89), BlockPos(92, -47, -23))
        override val regionBarrierBlimpTemplate = Region(BlockPos(63, 7, -558), BlockPos(77, -1, -583))
        override val regionBarrierBlimpTarget = Region(BlockPos(63, 17, -42), BlockPos(77, 9, -67))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Region.Set(
            Region(BlockPos(39, 13, -3), BlockPos(29, 16, -13)),
            Region(BlockPos(134, 13, -75), BlockPos(124, 16, -85)),
        )
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
        override val regionBlimpBalloons = Region.Set(
            Region(BlockPos(935, 58, -94), BlockPos(917, 77, -112)),
            Region(BlockPos(822, 58, -106), BlockPos(804, 77, -124)),
        )
        override val regionBlimpFans = Region.Set(
            Region(BlockPos(887, 64, -51), BlockPos(879, 62, -59)),
            Region(BlockPos(858, 64, -51), BlockPos(850, 62, -59)),
            Region(BlockPos(879, 64, -29), BlockPos(887, 62, -21)),
            Region(BlockPos(850, 64, -29), BlockPos(858, 62, -21)),
        )
        override val regionBarrierArenaTarget = Region(BlockPos(919, -1, -52), BlockPos(806, 35, -125))
        override val regionBarrierArenaTemplate = Region(BlockPos(121, -18, -663), BlockPos(8, 18, -736))
        override val regionBarrierBlimpTarget = Region(BlockPos(856, 61, -33), BlockPos(881, 53, -47))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Region.Set(
            Region(BlockPos(931, 58, -98), BlockPos(921, 61, -108)),
            Region(BlockPos(818, 58, -110), BlockPos(808, 61, -120)),
        )
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
        override val regionBlimpBalloons = Region.Set(
            Region(BlockPos(-1382, 117, 170), BlockPos(-1400, 136, 152)),
            Region(BlockPos(-1169, 117, 236), BlockPos(-1187, 136, 218)),
        )
        override val regionBlimpFans = Region.Set(
            Region(BlockPos(-1314, 123, 208), BlockPos(-1306, 121, 216)),
            Region(BlockPos(-1285, 123, 208), BlockPos(-1277, 121, 216)),
            Region(BlockPos(-1277, 123, 186), BlockPos(-1285, 121, 178)),
            Region(BlockPos(-1306, 123, 186), BlockPos(-1314, 121, 178)),
        )
        override val regionBarrierArenaTarget = Region(BlockPos(-1238, 71, 187), BlockPos(-1275, 63, 208))
        override val regionBarrierArenaTemplate = Region(BlockPos(88, 8, -618), BlockPos(51, 0, -597))
        override val regionBarrierBlimpTarget = Region(BlockPos(-1283, 112, 190), BlockPos(-1308, 120, 204))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Region.Set(
            Region(BlockPos(-1386, 117, 166), BlockPos(-1396, 120, 156)),
            Region(BlockPos(-1173, 117, 232), BlockPos(-1183, 120, 222)),
        )
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
        override val regionBlimpBalloons = Region.Set(
            Region(BlockPos(-151, -12, 875), BlockPos(-169, 7, 857)),
            Region(BlockPos(-62, -12, 867), BlockPos(-80, 7, 849)),
        )
        override val regionBlimpFans = Region.Set(
            Region(BlockPos(-103, -6, 785), BlockPos(-95, -8, 793)),
            Region(BlockPos(-132, -6, 785), BlockPos(-124, -8, 793)),
            Region(BlockPos(-103, -6, 763), BlockPos(-95, -8, 755)),
            Region(BlockPos(-132, -6, 763), BlockPos(-124, -8, 755)),
        )
        override val regionBarrierArenaTarget = Region(BlockPos(-96, -39, 840), BlockPos(-129, -54, 862))
        override val regionBarrierArenaTemplate = Region(BlockPos(41, 12, -574), BlockPos(8, -3, -552))
        override val regionBarrierBlimpTarget = Region(BlockPos(-101, -17, 767), BlockPos(-126, -9, 781))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Region.Set(
            Region(BlockPos(-155, -12, 871), BlockPos(-165, -9, 861)),
            Region(BlockPos(-66, -12, 863), BlockPos(-76, -9, 853)),
        )
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
        override val regionBlimpBalloons = Region.Set(
            Region(BlockPos(76, 51, -1236), BlockPos(58, 70, -1254)),
            Region(BlockPos(-100, 51, -1242), BlockPos(-118, 70, -1260)),
        )
        override val regionBlimpFans = Region.Set(
            Region(BlockPos(7, 58, -1159), BlockPos(-1, 56, -1167)),
            Region(BlockPos(-22, 58, -1159), BlockPos(-30, 56, -1167)),
            Region(BlockPos(-1, 58, -1137), BlockPos(7, 56, -1129)),
            Region(BlockPos(-30, 58, -1137), BlockPos(-22, 56, -1129)),
        )
        override val regionBarrierArenaTarget = Region(BlockPos(-47, 1, -1269), BlockPos(25, -3, -1235))
        override val regionBarrierArenaTemplate = Region(BlockPos(26, 9, -595), BlockPos(-46, 5, -629))
        override val regionBarrierBlimpTarget = Region(BlockPos(1, 47, -1155), BlockPos(-24, 55, -1141))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))
        override val regionBarrierBlimpBalloonTemplate = Empty.regionBarrierBlimpBalloonTemplate
        override val regionBarrierBlimpBalloonTargets = Region.Set(
            Region(BlockPos(72, 51, -1240), BlockPos(62, 54, -1250)),
            Region(BlockPos(-104, 51, -1246), BlockPos(-114, 54, -1256)),
        )
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

    object Horror : GameProperties {

        override val name = "horror"

        override val positionSpawn = BlockPos(-959, 112, -1080)

        override val positionBlimp = Vec3d(-958.5, 142.0, -1081.5)
        override val positionArena = Vec3d(-958.5, 112.0, -1079.5)
        override val positionDuel1 = Vec3d(-958.5, 112.0, -1051.5)
        override val positionDuel2 = Vec3d(-958.5, 112.0, -1107.5)
        override val positionHell = Vec3d(-958.5, -65536.0, -1079.5)

        override val templateInventory = BlockPos(-958, 142, -1066)
        override val templateLoottable = BlockPos(-958, 142, -1064)

        override val regionAll = Region(BlockPos(-1054, -64, -926), BlockPos(-850, 180, -1197))
        override val regionLegal = Region(BlockPos(-1054, -64, -926), BlockPos(-850, 65536, -1197))
        override val regionPlayable = Region(BlockPos(-1054, -64, -926), BlockPos(-850, 140, -1197))
        override val regionBlimp = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBlimpBalloons = Region.Set()
        override val regionBlimpFans = Region.Set()
        override val regionBarrierArenaTemplate = Region.EMPTY
        override val regionBarrierArenaTarget = Region.EMPTY
        override val regionBarrierBlimpTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpBalloonTemplate = Region(BlockPos(1, -20, -532), BlockPos(-9, -17, -542))
        override val regionBarrierBlimpBalloonTargets = Region.Set()
        override val regionFlood = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))

        override val borderSize = 200.0
        override val borderCenter = Vec3d(-958.5, 0.0, -1079.5)

        override val interactableBlocks = buildImmutableSet<Block> {
            addAll(Empty.interactableBlocks)
            addAll(Macander.interactableBlocks)
            addAll(Radiator.interactableBlocks)
            addAll(Realm.interactableBlocks)
            addAll(Lights.interactableBlocks)
            addAll(Station.interactableBlocks)
        }

        override val inflammableBlocks = Empty.interactableBlocks

        override val unstealableBlocks = emptySet<Block>()

    }

}
