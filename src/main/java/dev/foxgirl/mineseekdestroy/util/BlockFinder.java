package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class BlockFinder {

    private BlockFinder() {
    }

    public record Result(@NotNull BlockPos pos, @NotNull BlockState state) {
    }

    public interface Predicate {
        boolean test(@NotNull BlockState state);
    }

    private final static class Target {
        private final World world;
        private final Region region;

        private Target(World world, Region region) {
            this.world = world;
            this.region = region;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other){
                return true;
            }
            return other instanceof Target otherTarget
                && world == otherTarget.world
                && region.equals(otherTarget.region);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(world) + region.hashCode();
        }
    }

    private final static class Query {
        private final Predicate predicate;
        private final ArrayList<Result> results;

        private final CompletableFuture<List<Result>> promise;

        private Query(Predicate predicate, CompletableFuture<List<Result>> promise) {
            this.predicate = predicate;
            this.results = new ArrayList<>(64);

            this.promise = promise;
        }

        private void complete() {
            promise.complete(results);
        }
    }

    private final static class Searcher implements Runnable {
        private final Target target;
        private final Query[] queries;

        private Searcher(Target target, Query[] queries) {
            this.target = target;
            this.queries = queries;
        }

        private void perform() {
            var target = this.target;
            var queries = this.queries;

            var world = target.world;
            var region = target.region;

            var manager = world.getChunkManager();

            var posMin = region.getChunkStart();
            var posMax = region.getChunkEnd();
            int posXMin = posMin.x, posXMax = posMax.x;
            int posZMin = posMin.z, posZMax = posMax.z;

            var chunks = new ArrayList<Chunk>((posXMax - posXMin + 1) * (posZMax - posZMin + 1));

            for (int x = posXMin; x <= posXMax; x++) {
                for (int z = posZMin; z <= posZMax; z++) {
                    var chunk = manager.getChunk(x, z, ChunkStatus.FULL, true);
                    if (chunk != null) chunks.add(chunk);
                }
            }

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
                                if (state == null || state.isAir()) continue;
                                int posY = y + offsetY;
                                int posX = x + offsetX;
                                int posZ = z + offsetZ;
                                if (region.excludes(posX, posY, posZ)) continue;
                                for (var query : queries) {
                                    if (query.predicate.test(state)) {
                                        query.results.add(new Result(new BlockPos(posX, posY, posZ), state));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (var query : queries) {
                query.complete();
            }
        }

        @Override
        public void run() {
            try {
                perform();
            } catch (Throwable cause) {
                for (var query : queries) {
                    try {
                        query.promise.completeExceptionally(cause);
                    } catch (Throwable ignored) {
                    }
                }
                throw cause;
            }
        }
    }

    private static final LinkedHashMap<Target, ArrayList<Query>> queries = new LinkedHashMap<>();
    private static final Object queriesLock = new Object();

    /**
     * Performs all enqueued search operations.
     */
    public static void update() {
        ArrayList<Searcher> searches;

        synchronized (queriesLock) {
            if (queries.isEmpty()) {
                return;
            }

            searches = new ArrayList<>(queries.size());

            for (var entry : queries.entrySet()) {
                var target = entry.getKey();
                var queries = entry.getValue().toArray(new Query[0]);
                searches.add(new Searcher(target, queries));
            }

            queries.clear();
        }

        searches.sort(Comparator.comparingLong((searcher) -> searcher.target.region.size()));

        var server = Game.getGame().getServer();

        for (var searcher : searches) {
            server.execute(searcher);
            Game.LOGGER.info("BlockFinder executing search for " + searcher.queries.length + " queries");
        }
    }

    /**
     * Enqueues a search operation in the given region for blocks, filtered by the predicate.
     * @param world World to search in.
     * @param region Region to search in.
     * @param predicate Predicate to filter blocks by.
     * @return
     *   {@link CompletableFuture} that is resolved with a list of search
     *   results.
     * @throws NullPointerException If any of the provided arguments are null.
     */
    public static @NotNull CompletableFuture<@NotNull List<@NotNull Result>> search(@NotNull World world, @NotNull Region region, @NotNull Predicate predicate) {
        Objects.requireNonNull(world, "Argument 'world'");
        Objects.requireNonNull(region, "Argument 'region'");
        Objects.requireNonNull(predicate, "Argument 'predicate'");

        var target = new Target(world, region);
        var promise = new CompletableFuture<List<Result>>();

        synchronized (queriesLock) {
            queries
                .computeIfAbsent(target, (key) -> new ArrayList<>())
                .add(new Query(predicate, promise));
        }

        return promise;
    }

}
