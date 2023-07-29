package dev.foxgirl.mineseekdestroy.util

import dev.foxgirl.mineseekdestroy.util.collect.ImmutableSet
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

class Region : Selection {

    val start: BlockPos
    val end: BlockPos

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
    }

    val chunkStart get() = ChunkPos(start)
    val chunkEnd get() = ChunkPos(end)

    val center get() =
        Vec3d(
            (start.x.toLong() + end.x.toLong()).toDouble() / 2.0 + 0.5,
            (start.y.toLong() + end.y.toLong()).toDouble() / 2.0 + 0.5,
            (start.z.toLong() + end.z.toLong()).toDouble() / 2.0 + 0.5,
        )

    val size get() =
        (end.x - start.x).toLong() *
        (end.y - start.y).toLong() *
        (end.z - start.z).toLong()

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

    class Set private constructor(private val regions: ImmutableSet<Region>) : kotlin.collections.Set<Region> by regions, Selection {

        constructor(vararg elements: Region) : this(ImmutableSet.copyOf(elements))
        constructor(collection: Collection<Region>) : this(ImmutableSet.copyOf(collection))

        override fun contains(x: Int, y: Int, z: Int) = regions.any { it.contains(x, y, z) }

        override fun equals(other: Any?) =
            other === this || (other is kotlin.collections.Set<*> && size == other.size && containsAll(other))

        override fun hashCode() = regions.hashCode()
        override fun toString() = regions.toString()

    }

}
