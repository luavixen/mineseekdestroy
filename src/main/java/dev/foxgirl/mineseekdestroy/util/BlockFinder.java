package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class BlockFinder {

    private BlockFinder() {
    }

    public record Result(@NotNull BlockPos pos, @NotNull BlockState state) {
    }

    public interface Predicate {
        boolean test(@NotNull BlockState state);
    }

    private final static class Searcher implements Supplier<List<Result>> {
        private final World world;
        private final Region region;
        private final Predicate predicate;

        private Searcher(World world, Region region, Predicate predicate) {
            this.world = world;
            this.region = region;
            this.predicate = predicate;
        }

        @Override
        public List<Result> get() {
            var manager = world.getChunkManager();

            var posMin = region.getChunkStart();
            var posMax = region.getChunkEnd();
            int posXMin = posMin.x, posXMax = posMax.x;
            int posZMin = posMin.z, posZMax = posMax.z;

            var chunks = new ArrayList<Chunk>((posXMax - posXMin + 1) * (posZMax - posZMin + 1));

            for (int x = posXMin; x <= posXMax; x++) {
                for (int z = posZMin; z <= posZMax; z++) {
                    var chunk = manager.getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk != null) chunks.add(chunk);
                }
            }

            var results = new ArrayList<Result>(64);

            var predicate = this.predicate;
            var region = this.region;

            for (var chunk : chunks) {
                var chunkPos = chunk.getPos();
                int offsetX = chunkPos.x << 4;
                int offsetZ = chunkPos.z << 4;
                for (var section : chunk.getSectionArray()) {
                    if (section.isEmpty()) continue;
                    int offsetY = section.getYOffset();
                    for (int y = 0; y < 16; y++) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                var state = section.getBlockState(x, y, z);
                                if (state == null || state.isAir() || !predicate.test(state)) continue;
                                int posY = y + offsetY;
                                int posX = x + offsetX;
                                int posZ = z + offsetZ;
                                if (!region.contains(posX, posY, posZ)) continue;
                                results.add(new Result(new BlockPos(posX, posY, posZ), state));
                            }
                        }
                    }
                }
            }

            return results;
        }
    }

    public static @NotNull CompletableFuture<@NotNull List<@NotNull Result>> search(@NotNull World world, @NotNull Region region, @NotNull Predicate predicate) {
        Objects.requireNonNull(world, "Argument 'world'");
        Objects.requireNonNull(region, "Argument 'region'");
        Objects.requireNonNull(predicate, "Argument 'predicate'");
        return CompletableFuture.supplyAsync(new Searcher(world, region, predicate), Game.getGame().getServer());
    }

}
