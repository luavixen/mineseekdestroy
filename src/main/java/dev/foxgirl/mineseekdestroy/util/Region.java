package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Region {

    private final BlockPos start;
    private final BlockPos end;

    public Region(@NotNull Vec3i a, @NotNull Vec3i b) {
        Objects.requireNonNull(a, "Argument 'a'");
        Objects.requireNonNull(b, "Argument 'b'");
        start = new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
        end = new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }

    public long size() {
        return ((long) (end.getX() - start.getX()))
             * ((long) (end.getY() - start.getY()))
             * ((long) (end.getZ() - start.getZ()));
    }

    public @NotNull BlockPos component1() {
        return start;
    }

    public @NotNull BlockPos component2() {
        return end;
    }

    public @NotNull BlockPos getStart() {
        return start;
    }

    public @NotNull BlockPos getEnd() {
        return end;
    }

    public @NotNull ChunkPos getChunkStart() {
        return new ChunkPos(start);
    }

    public @NotNull ChunkPos getChunkEnd() {
        return new ChunkPos(end);
    }

    public @NotNull Vec3d getCenter() {
        return new Vec3d(
            (double) ((long) start.getX() + (long) end.getX()) / 2.0 + 0.5,
            (double) ((long) start.getY() + (long) end.getY()) / 2.0 + 0.5,
            (double) ((long) start.getZ() + (long) end.getZ()) / 2.0 + 0.5
        );
    }

    public boolean contains(int x, int y, int z) {
        return start.getX() <= x && end.getX() >= x
            && start.getY() <= y && end.getY() >= y
            && start.getZ() <= z && end.getZ() >= z;
    }

    public boolean contains(@NotNull Vec3i pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(@NotNull Entity entity) {
        return contains(entity.getBlockPos());
    }

    public boolean excludes(int x, int y, int z) {
        return !contains(x, y, z);
    }

    public boolean excludes(@NotNull Vec3i pos) {
        return !contains(pos);
    }

    public boolean excludes(@NotNull Entity entity) {
        return !contains(entity);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Region other) {
            return start.equals(other.start)
                && end.equals(other.end);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * start.hashCode() + end.hashCode();
    }

    @Override
    public String toString() {
        return "Region{start=" + start + ", end=" + end + "}";
    }

}
