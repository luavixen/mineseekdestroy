package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class Editor {

    private Editor() {
    }

    public record Result(@NotNull BlockPos pos, @NotNull BlockState state) {
    }

    @FunctionalInterface
    public interface Predicate {
        boolean test(@NotNull BlockState state);
    }

    @FunctionalInterface
    public interface Action {
        @Nullable BlockState apply(@NotNull BlockState state, int y, int x, int z);
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
                && world.equals(otherTarget.world)
                && region.equals(otherTarget.region);
        }

        @Override
        public int hashCode() {
            return 31 * world.hashCode() + region.hashCode();
        }
    }

    private interface Operation {
        Action action();
        void complete();
        void completeExceptionally(Throwable cause);
    }

    private final static class EditOperation implements Operation {
        private final CompletableFuture<Void> promise;
        private final Action action;

        private EditOperation(CompletableFuture<Void> promise, Action action) {
            this.promise = promise;
            this.action = action;
        }

        @Override
        public Action action() {
            return action;
        }

        @Override
        public void complete() {
            promise.complete(null);
        }

        @Override
        public void completeExceptionally(Throwable cause) {
            promise.completeExceptionally(cause);
        }
    }

    private final static class SearchOperation implements Operation, Action {
        private final CompletableFuture<List<Result>> promise;

        private final Predicate predicate;
        private final ArrayList<Result> results;

        private SearchOperation(CompletableFuture<List<Result>> promise, Predicate predicate) {
            this.promise = promise;
            this.predicate = predicate;
            this.results = new ArrayList<>(64);
        }

        @Override
        public @Nullable BlockState apply(@NotNull BlockState state, int y, int x, int z) {
            if (predicate.test(state)) {
                results.add(new Result(new BlockPos(x, y, z), state));
            }
            return null;
        }

        @Override
        public Action action() {
            return this;
        }

        @Override
        public void complete() {
            promise.complete(results);
        }

        @Override
        public void completeExceptionally(Throwable cause) {
            promise.completeExceptionally(cause);
        }
    }

    private final static class Task implements Runnable {
        private final Target target;
        private final Operation[] operations;

        public Task(Target target, Operation[] operations) {
            this.target = target;
            this.operations = operations;
        }

        private void performChunk(Chunk chunk, Region region, Action[] actions) {
            var cPos = chunk.getPos();
            int offsetX = cPos.x << 4;
            int offsetZ = cPos.z << 4;

            var bPosMin = region.getStart();
            var bPosMax = region.getEnd();
            int bPosMinY = bPosMin.getY(), bPosMaxY = bPosMax.getY();
            int bPosMinX = bPosMin.getX(), bPosMaxX = bPosMax.getX();
            int bPosMinZ = bPosMin.getZ(), bPosMaxZ = bPosMax.getZ();

            for (var section : chunk.getSectionArray()) {
                if (section.isEmpty()) continue;
                int offsetY = section.getYOffset();
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int posY = y + offsetY;
                            int posX = x + offsetX;
                            int posZ = z + offsetZ;
                            if (bPosMinY > posY || bPosMaxY < posY) continue;
                            if (bPosMinX > posX || bPosMaxX < posX) continue;
                            if (bPosMinZ > posZ || bPosMaxZ < posZ) continue;
                            var stateOld = section.getBlockState(x, y, z);
                            for (var action : actions) {
                                var stateNew = action.apply(stateOld, posY, posX, posZ);
                                if (stateNew != null) {
                                    stateOld = stateNew;
                                    section.setBlockState(x, y, z, stateNew);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void perform() {
            var target = this.target;
            var operations = this.operations;

            var region = target.region;
            var manager = target.world.getChunkManager();

            var cPosMin = region.getChunkStart();
            var cPosMax = region.getChunkEnd();
            int cPosMinX = cPosMin.x, cPosMaxX = cPosMax.x;
            int cPosMinZ = cPosMin.z, cPosMaxZ = cPosMax.z;

            var chunks = new ArrayList<Chunk>((cPosMaxX - cPosMinX + 1) * (cPosMaxZ - cPosMinZ + 1));

            for (int x = cPosMinX; x <= cPosMaxX; x++) {
                for (int z = cPosMinZ; z <= cPosMaxZ; z++) {
                    var chunk = manager.getChunk(x, z, ChunkStatus.FULL, true);
                    if (chunk != null) chunks.add(chunk);
                }
            }

            var actions = new Action[operations.length];
            for (int i = 0, length = operations.length; i < length; i++) {
                actions[i] = operations[i].action();
            }

            for (var chunk : chunks) {
                performChunk(chunk, region, actions);
            }

            for (var operation : operations) {
                operation.complete();
            }
        }

        @Override
        public void run() {
            try {
                perform();
            } catch (Throwable cause) {
                for (var operation : operations) {
                    try {
                        operation.completeExceptionally(cause);
                    } catch (Throwable ignored) {
                    }
                }
                throw cause;
            }
        }
    }

    private static final LinkedHashMap<Target, ArrayList<Operation>> operations = new LinkedHashMap<>();
    private static final Object operationsLock = new Object();

    private static void enqueue(World world, Region region, Operation operation) {
        synchronized (operationsLock) {
            operations
                .computeIfAbsent(new Target(world, region), (key) -> new ArrayList<>())
                .add(operation);
        }
    }

    /**
     * Executes all enqueued operations.
     */
    public static void update() {
        ArrayList<Task> tasks;

        synchronized (operationsLock) {
            if (operations.isEmpty()) {
                return;
            }

            tasks = new ArrayList<>(operations.size());

            for (var entry : operations.entrySet()) {
                var target = entry.getKey();
                var operations = entry.getValue().toArray(new Operation[0]);
                tasks.add(new Task(target, operations));
            }

            operations.clear();
        }

        tasks.sort(Comparator.comparingLong((task) -> task.target.region.size()));

        var server = Game.getGame().getServer();

        for (var task : tasks) {
            Game.LOGGER.info("Editor executing task for " + task.operations.length + " operations");
            server.execute(task);
        }
    }

    /**
     * Enqueues an edit operation in the given region.
     * @param world World to search in.
     * @param region Region to search in.
     * @param action Action to perform.
     * @return
     *   {@link CompletableFuture} that is resolved when the operation
     *   completes.
     * @throws NullPointerException If any of the provided arguments are null.
     */
    public static @NotNull CompletableFuture<@Nullable Void> edit(@NotNull World world, @NotNull Region region, @NotNull Action action) {
        Objects.requireNonNull(world, "Argument 'world'");
        Objects.requireNonNull(region, "Argument 'region'");
        Objects.requireNonNull(action, "Argument 'action'");

        CompletableFuture<Void> promise = new CompletableFuture<>();
        enqueue(world, region, new EditOperation(promise, action));
        return promise;
    }

    /**
     * Enqueues a search operation in the given region for specific blocks, filtered by the predicate.
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

        CompletableFuture<List<Result>> promise = new CompletableFuture<>();
        enqueue(world, region, new SearchOperation(promise, predicate));
        return promise;
    }

}
