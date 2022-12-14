package dev.foxgirl.mineseekdestroy

import dev.foxgirl.mineseekdestroy.util.Region
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

    }

    object Radiator : GameProperties {

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

    }

}
