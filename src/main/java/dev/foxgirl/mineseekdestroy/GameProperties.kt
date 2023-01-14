package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.util.Region
import net.minecraft.block.Block
import net.minecraft.block.Blocks.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d

sealed interface GameProperties {

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
    val regionBarrierArenaTarget: Region
    val regionBarrierArenaTemplate: Region
    val regionBarrierBlimpTarget: Region
    val regionBarrierBlimpTemplate: Region

    val interactableBlocks: Set<Block>

    object Empty : GameProperties {

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
        override val regionBarrierArenaTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierArenaTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))

        override val interactableBlocks = setOf<Block>(
            CHEST,
            BARREL,
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
        )

    }

    object Macander : GameProperties {

        override val positionBlimp = Vec3d(70.5, 1.0, -55.5)
        override val positionArena = Vec3d(70.5, -39.0, -55.5)
        override val positionDuel1 = Vec3d(70.5, -39.0, -71.5)
        override val positionDuel2 = Vec3d(70.5, -39.0, -39.5)
        override val positionHell = Vec3d(70.5, -65536.0, -55.5)

        override val templateInventory = BlockPos(69, 1, -72)
        override val templateLoottable = BlockPos(69, 1, -74)

        override val regionAll = Region(BlockPos(-24, 35, 51), BlockPos(175, -61, -169))
        override val regionLegal = Region(BlockPos(-24, 65536, 51), BlockPos(175, -56, -169))
        override val regionPlayable = Region(BlockPos(-24, -6, 51), BlockPos(175, -56, -169))
        override val regionBlimp = Region(BlockPos(91, -1, -102), BlockPos(49, 20, -32))
        override val regionBarrierArenaTarget = Region(BlockPos(48, -30, -89), BlockPos(92, -47, -23))
        override val regionBarrierArenaTemplate = Region(BlockPos(48, -30, -605), BlockPos(92, -47, -539))
        override val regionBarrierBlimpTarget = Region(BlockPos(63, 7, -42), BlockPos(77, -1, -67))
        override val regionBarrierBlimpTemplate = Region(BlockPos(63, 7, -558), BlockPos(77, -1, -583))

        override val interactableBlocks = Empty.interactableBlocks + setOf(
            ACACIA_DOOR,
            BIRCH_DOOR,
            DARK_OAK_DOOR,
            JUNGLE_DOOR,
            MANGROVE_DOOR,
            OAK_DOOR,
            SPRUCE_DOOR,
            WARPED_DOOR,
            MANGROVE_FENCE_GATE,
            BIRCH_TRAPDOOR,
        )

    }

    object Radiator : GameProperties {

        override val positionBlimp = Vec3d(870.5, 55.0, -40.5)
        override val positionArena = Vec3d(870.5, 5.0, -40.5)
        override val positionDuel1 = Vec3d(837.5, 44.0, 91.5)
        override val positionDuel2 = Vec3d(901.5, 44.0, 91.5)
        override val positionHell = Vec3d(870.5, -65536.0, -40.5)

        override val templateInventory = BlockPos(886, 55, -41)
        override val templateLoottable = BlockPos(888, 55, -41)

        override val regionAll = Region(BlockPos(788, 57, -170), BlockPos(974, -8, 30))
        override val regionLegal = Region(BlockPos(788, 65536, -170), BlockPos(974, -8, 30))
        override val regionPlayable = Region(BlockPos(788, 57, -170), BlockPos(974, -8, 30))
        override val regionBlimp = Region(BlockPos(918, 80, -16), BlockPos(835, 53, -62))
        override val regionBarrierArenaTarget = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierArenaTemplate = Region(BlockPos(0, 0, 0), BlockPos(0, 0, 0))
        override val regionBarrierBlimpTarget = Region(BlockPos(856, 61, -33), BlockPos(881, 53, -47))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))

        override val interactableBlocks = Empty.interactableBlocks + setOf(
        )

    }

    object Realm : GameProperties {

        override val positionBlimp = Vec3d(-1293.5, 114.0, 197.5)
        override val positionArena = Vec3d(-1293.5, 63.0, 197.5)
        override val positionDuel1 = Vec3d(-1240.5, 63.0, 190.5)
        override val positionDuel2 = Vec3d(-1271.5, 63.0, 205.5)
        override val positionHell = Vec3d(-1293.5, -65536.0, 197.5)

        override val templateInventory = BlockPos(-1278, 114, 192)
        override val templateLoottable = BlockPos(-1276, 114, 197)

        override val regionAll = Region(BlockPos(-1600, 112, 591), BlockPos(-945, -8, -192))
        override val regionLegal = Region(BlockPos(-1600, 65536, 591), BlockPos(-945, -64, -192))
        override val regionPlayable = Region(BlockPos(-1600, 112, 591), BlockPos(-945, -8, -192))
        override val regionBlimp = Region(BlockPos(-1326, 133, 177), BlockPos(-1249, 113, 216))
        override val regionBarrierArenaTarget = Region(BlockPos(-1238, 71, 187), BlockPos(-1275, 63, 208))
        override val regionBarrierArenaTemplate = Region(BlockPos(88, 8, -618), BlockPos(51, 0, -597))
        override val regionBarrierBlimpTarget = Region(BlockPos(-1283, 112, 190), BlockPos(-1308, 120, 204))
        override val regionBarrierBlimpTemplate = Region(BlockPos(82, 28, -578), BlockPos(57, 36, -564))

        override val interactableBlocks = Empty.interactableBlocks + setOf(
        )

    }

}
