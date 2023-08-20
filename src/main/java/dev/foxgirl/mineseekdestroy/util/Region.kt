package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.util.collect.ImmutableSet
import net.minecraft.util.math.*

class Region : Selection {

    val start: BlockPos
    val end: BlockPos

    val chunkStart: ChunkPos
    val chunkEnd: ChunkPos

    val box: Box

    constructor(a: Vec3i, b: Vec3i) {
        start = BlockPos(
            Math.min(a.x, b.x),
            Math.min(a.y, b.y),
            Math.min(a.z, b.z),
        )
        end = BlockPos(
            Math.max(a.x, b.x),
            Math.max(a.y, b.y),
            Math.max(a.z, b.z),
        )
        chunkStart = ChunkPos(start)
        chunkEnd = ChunkPos(end)
        box = Box(start, end.add(1, 1, 1))
    }

    val center get() =
        Vec3d(
            (start.x.toLong() + end.x.toLong()).toDouble() / 2.0 + 0.5,
            (start.y.toLong() + end.y.toLong()).toDouble() / 2.0 + 0.5,
            (start.z.toLong() + end.z.toLong()).toDouble() / 2.0 + 0.5,
        )

    val blockCount get() =
        (end.x - start.x + 1).toLong() *
        (end.y - start.y + 1).toLong() *
        (end.z - start.z + 1).toLong()

    val chunkCount get() =
        (chunkEnd.x - chunkStart.x + 1).toLong() *
        (chunkEnd.z - chunkStart.z + 1).toLong()

    operator fun component1() = start
    operator fun component2() = end

    override fun contains(x: Int, y: Int, z: Int) =
        start.x <= x && end.x >= x &&
        start.y <= y && end.y >= y &&
        start.z <= z && end.z >= z

    fun offset(x: Int, y: Int, z: Int) = Region(start.add(x, y, z), end.add(x, y, z))
    fun offset(pos: Vec3i) = offset(pos.x, pos.y, pos.z)

    override fun equals(other: Any?) =
        other === this || (other is Region && start == other.start && end == other.end)

    override fun hashCode() =
        31 * start.hashCode() + end.hashCode()

    override fun toString() =
        "Region{start=$start, end=$end}"

    class Set : AbstractSet<Region>, Selection {

        private val regions: Array<Region>

        constructor(vararg elements: Region) {
            regions = ImmutableSet.copyOf(elements).toTypedArray()
        }
        constructor(collection: Collection<Region>) {
            regions = ImmutableSet.copyOf(collection).toTypedArray()
        }

        override fun contains(x: Int, y: Int, z: Int) = regions.any { it.contains(x, y, z) }

        override val size get() = regions.size
        override fun iterator() = regions.iterator()

    }

}
