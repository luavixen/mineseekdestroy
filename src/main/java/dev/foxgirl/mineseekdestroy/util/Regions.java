package dev.foxgirl.mineseekdestroy.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class Regions extends AbstractList<Region> implements RandomAccess {

    public static final @NotNull Regions EMPTY = new Regions();

    private final Region[] regions;

    public Regions(@NotNull Region @NotNull ... regions) {
        Objects.requireNonNull(regions, "Argument 'regions'");
        this.regions = regions.clone();
    }

    public Regions(@NotNull Collection<? extends Region> collection) {
        Objects.requireNonNull(collection, "Argument 'collection'");
        this.regions = collection.toArray(EMPTY.regions).clone();
    }

    public boolean contains(int x, int y, int z) {
        for (var region : regions) if (region.contains(x, y, z)) return true;
        return false;
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
    public int size() {
        return regions.length;
    }

    @Override
    public Region get(int index) {
        return regions[index];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(regions);
    }

    @Override
    public String toString() {
        return Arrays.toString(regions);
    }

}
