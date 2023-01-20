package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
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
            ((double) start.getX() + (double) end.getX()) / 2.0 + 0.5,
            ((double) start.getY() + (double) end.getY()) / 2.0 + 0.5,
            ((double) start.getZ() + (double) end.getZ()) / 2.0 + 0.5
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

    public boolean contains(@NotNull Position pos) {
        return contains((int) pos.getX(), (int) pos.getY(), (int) pos.getZ());
    }

    public boolean contains(@NotNull Entity entity) {
        return contains(entity.getBlockPos());
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
