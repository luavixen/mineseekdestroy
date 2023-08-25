package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableList;
import dev.foxgirl.mineseekdestroy.util.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
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

    private static final class Target {
        private final ServerWorld world;
        private final Region region;

        private Target(ServerWorld world, Region region) {
            this.world = world;
            this.region = region;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
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

    private static final class EditOperation implements Operation {
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

    private static final class SearchOperation implements Operation, Action {
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

    private static final class Task {
        private final Target target;
        private final Operation[] operations;

        public Task(Target target, Operation[] operations) {
            this.target = target;
            this.operations = operations;
        }

        private boolean performChunk(WorldChunk chunk, Region region, ServerWorld world, Action[] actions) {
            var cPos = chunk.getPos();
            int offsetX = cPos.x << 4;
            int offsetZ = cPos.z << 4;

            var posMin = region.getStart();
            var posMax = region.getEnd();
            int posMinY = posMin.getY(), posMaxY = posMax.getY();
            int posMinX = posMin.getX(), posMaxX = posMax.getX();
            int posMinZ = posMin.getZ(), posMaxZ = posMax.getZ();

            int bottomY = world.getBottomY();

            boolean mutated = false;

            var sections = chunk.getSectionArray();

            for (int i = 0, length = sections.length; i < length; i++) {
                var section = sections[i];
                int offsetY = (i << 4) + bottomY;
                for (int y = 0; y < 16; y++) {
                    int posY = y + offsetY;
                    if (posMinY > posY || posMaxY < posY) continue;
                    for (int x = 0; x < 16; x++) {
                        int posX = x + offsetX;
                        if (posMinX > posX || posMaxX < posX) continue;
                        for (int z = 0; z < 16; z++) {
                            int posZ = z + offsetZ;
                            if (posMinZ > posZ || posMaxZ < posZ) continue;
                            var stateOld = section.getBlockState(x, y, z);
                            for (var action : actions) {
                                var stateNew = action.apply(stateOld, posY, posX, posZ);
                                if (stateNew != null) {
                                    section.setBlockState(x, y, z, stateNew);
                                    stateOld = stateNew;
                                    mutated = true;
                                }
                            }
                        }
                    }
                }
            }

            return mutated;
        }

        private List<WorldChunk> performTask() {
            Target target = this.target;
            Operation[] operations = this.operations;

            Region region = target.region;
            ServerWorld world = target.world;

            ServerChunkManager manager = world.getChunkManager();

            int chunksCount = (int) region.getChunkCount();

            var chunks = new ArrayList<WorldChunk>(chunksCount);
            var chunksMutated = new ArrayList<WorldChunk>(chunksCount);

            var cPosMin = region.getChunkStart();
            var cPosMax = region.getChunkEnd();
            int cPosMinX = cPosMin.x, cPosMaxX = cPosMax.x;
            int cPosMinZ = cPosMin.z, cPosMaxZ = cPosMax.z;

            for (int x = cPosMinX; x <= cPosMaxX; x++) {
                for (int z = cPosMinZ; z <= cPosMaxZ; z++) {
                    var chunk = manager.getChunk(x, z, ChunkStatus.FULL, true);
                    if (chunk != null) chunks.add((WorldChunk) chunk);
                }
            }

            var actions = new Action[operations.length];
            for (int i = 0, length = operations.length; i < length; i++) {
                actions[i] = operations[i].action();
            }

            for (WorldChunk chunk : chunks) {
                var mutated = performChunk(chunk, region, world, actions);
                if (mutated) chunksMutated.add(chunk);
            }

            for (Operation operation : operations) {
                operation.complete();
            }

            return chunksMutated;
        }

        private List<WorldChunk> perform() {
            var start = System.nanoTime();
            var success = true;
            try {
                return performTask();
            } catch (Throwable cause) {
                for (Operation operation : operations) {
                    try {
                        operation.completeExceptionally(cause);
                    } catch (Throwable ignored) {
                    }
                }
                success = false;
            } finally {
                var message = new StringBuilder(64);
                message.append("Editor performed task (");
                message.append(success ? "success" : "failure");
                message.append(") for ");
                message.append(operations.length);
                message.append(" operation(s) in ");
                message.append(new DecimalFormat("#.##").format((double) (System.nanoTime() - start) * 1e-6D));
                message.append("ms");
                if (success) Game.LOGGER.info(message.toString());
                else Game.LOGGER.warn(message.toString());
            }
            return ImmutableList.of();
        }
    }

    private static final LinkedHashMap<Target, ArrayList<Operation>> operations = new LinkedHashMap<>();
    private static final Object lock = new Object();

    private static void enqueue(ServerWorld world, Region region, Operation operation) {
        synchronized (lock) {
            operations
                .computeIfAbsent(new Target(world, region), (key) -> new ArrayList<>())
                .add(operation);
        }
    }

    /**
     * Executes all enqueued operations.
     */
    public static void update() {
        var server = Game.getGame().getServer();

        if (!server.isOnThread()) {
            throw new IllegalStateException("Editor update started from wrong thread");
        }

        ArrayList<Task> tasks;

        synchronized (lock) {
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

        tasks.sort(Comparator.comparingLong((task) -> task.target.region.getBlockCount()));

        var chunkLists = new ArrayList<List<WorldChunk>>(tasks.size());
        var chunkCount = 0;

        for (Task task : tasks) {
            var chunks = task.perform();
            if (chunks.isEmpty()) continue;
            chunkLists.add(chunks);
            chunkCount = Math.max(chunkCount, (int) task.target.region.getChunkCount());
        }

        if (chunkLists.isEmpty()) return;

        var chunks = ImmutableSet.<WorldChunk>builder(chunkCount);
        chunkLists.forEach(chunks::addAll);

        var distance = server.getPlayerManager().getViewDistance() + 4;

        for (WorldChunk chunk : chunks.build()) {
            var world = (ServerWorld) chunk.getWorld();
            var packet = new ChunkDataS2CPacket(chunk, world.getChunkManager().getLightingProvider(), null, null);
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.getChunkPos().getChebyshevDistance(chunk.getPos()) > distance) continue;
                player.networkHandler.sendPacket(packet);
            }
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
        enqueue((ServerWorld) world, region, new EditOperation(promise, action));
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
        enqueue((ServerWorld) world, region, new SearchOperation(promise, predicate));
        return promise;
    }

}
