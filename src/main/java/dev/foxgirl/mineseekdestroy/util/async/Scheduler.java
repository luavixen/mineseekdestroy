package dev.foxgirl.mineseekdestroy.util.async;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.PriorityQueue;

public final class Scheduler {

    private Scheduler() {
        throw new UnsupportedOperationException();
    }

    /**
     * Schedule represents a task that will execute in the future.
     */
    public interface Schedule {
        /**
         * Cancels this schedule, preventing the task from running.
         * @return
         *   True if the schedule was cancelled, false if the schedule was
         *   already cancelled or completed.
         */
        boolean cancel();
    }

    /**
     * Callback represents a task callback to be invoked as part of a task's
     * execution.
     */
    @FunctionalInterface
    public interface Callback {
        /**
         * Performs the task.
         * @param schedule Schedule corresponding to the task's execution.
         */
        void invoke(@NotNull Schedule schedule);
    }

    private static final long TIME_BASE = System.nanoTime();

    private static long timeNow() {
        return System.nanoTime() - TIME_BASE;
    }
    private static long timeConvert(double seconds) {
        if (!Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is not finite");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("Argument 'seconds' is negative");
        }
        var ns = (long) (seconds * 1e+9);
        if (ns < 0) {
            throw new IllegalStateException("Converted time value is invalid");
        }
        return ns;
    }

    private static final LinkedHashSet<Executable> EXECUTE_QUEUE = new LinkedHashSet<>(128);
    private static final PriorityQueue<Task> WAITING_QUEUE = new PriorityQueue<>(128);

    private static class Executable implements Schedule {
        private final Callback callback;

        private Executable(Callback callback) {
            this.callback = callback;
        }

        private void execute() {
            try {
                callback.invoke(this);
            } catch (Throwable cause) {
                cancel();
                Async.printThrowable(cause);
            }
        }

        Schedule schedule() {
            synchronized (EXECUTE_QUEUE) { EXECUTE_QUEUE.add(this); }
            return this;
        }

        @Override
        public boolean cancel() {
            synchronized (EXECUTE_QUEUE) { return EXECUTE_QUEUE.remove(this); }
        }

        @Override
        public String toString() {
            return "Executable{callback.getClass()=" + callback.getClass() + ", callback=" + callback + "}";
        }
    }

    private static final class Task extends Executable implements Comparable<Task> {
        private long time;
        private final long period;

        private Task(Callback callback, long time, long period) {
            super(callback);
            if (time < 0) throw new IllegalArgumentException("Invalid calculated time");
            if (period < -1) throw new IllegalArgumentException("Invalid calculated time period");
            this.time = time;
            this.period = period;
        }

        private boolean tryContinue() {
            // Note that tryContinue is only ever called while synchronized on WAITING_QUEUE

            // Ensure this task is ready to be executed
            if (time > timeNow()) {
                // Return false to indicate that this task is not ready, and we should stop traversing the queue
                return false;
            }

            // Remove this task from the queue
            WAITING_QUEUE.remove(this);

            // Check if this task should be rescheduled, and update & re-add it to the queue if so
            if (period >= 0) {
                // Update the next execution time
                time = Math.max(time + period, timeNow());
                // Re-add this task to the queue asynchronously (avoids an infinite loop with period 0)
                EXECUTE_QUEUE.add(new Executable((schedule) -> this.schedule()));
            }

            // Call Executable's schedule method to add this task to the execute-next queue
            super.schedule();

            // Return true to indicate that we should continue onto the next task in the queue
            return true;
        }

        @Override
        Schedule schedule() {
            synchronized (WAITING_QUEUE) { WAITING_QUEUE.offer(this); }
            return this;
        }

        @Override
        public boolean cancel() {
            var removed = super.cancel();
            synchronized (WAITING_QUEUE) {
                var removedWaiting = WAITING_QUEUE.remove(this);
                if (removedWaiting) removed = true;
            }
            return removed;
        }

        @Override
        public int compareTo(@NotNull Scheduler.Task other) {
            return Long.compare(this.time, other.time);
        }

        @Override
        public String toString() {
            return "Task{time=" + time + ", period=" + period + ", super=" + super.toString() + "}";
        }
    }

    /**
     * Updates this scheduler by executing all tasks that are ready to be
     * executed. Must be called from the main server thread only once per tick.
     * @throws IllegalStateException If invoked from the wrong thread.
     */
    public static void update() {
        if (!Game.getGame().getServer().isOnThread()) {
            throw new IllegalStateException("Scheduler execution started from the wrong thread");
        }

        synchronized (WAITING_QUEUE) {
            while (!WAITING_QUEUE.isEmpty()) {
                boolean shouldContinue = WAITING_QUEUE.peek().tryContinue();
                if (!shouldContinue) break;
            }
        }

        Executable[] executables;

        synchronized (EXECUTE_QUEUE) {
            executables = EXECUTE_QUEUE.toArray(new Executable[0]);
            EXECUTE_QUEUE.clear();
        }

        for (var executable : executables) {
            executable.execute();
        }
    }

    /**
     * Schedules a task to be run (once) as soon as possible.
     * @param callback Task callback to invoke.
     * @return Newly created task schedule.
     * @throws NullPointerException If the provided callback is null.
     */
    public static @NotNull Schedule now(@NotNull Callback callback) {
        if (callback == null) throw new NullPointerException("Argument 'callback'");
        return new Executable(callback).schedule();
    }

    /**
     * Schedules a task to be run (once) at some time in the future.
     * @param seconds Delay in seconds to wait before execution.
     * @param callback Task callback to invoke.
     * @return Newly created task schedule.
     * @throws IllegalArgumentException If the provided delay is invalid.
     * @throws NullPointerException If the provided callback is null.
     */
    public static @NotNull Schedule delay(double seconds, @NotNull Callback callback) {
        if (callback == null) throw new NullPointerException("Argument 'callback'");
        long ns = timeConvert(seconds);
        return new Task(callback, timeNow() + ns, -1).schedule();
    }
    /**
     * Schedules a task to be run multiple times on an interval.
     * @param seconds Delay in seconds to wait before and between executions.
     * @param callback Task callback to invoke.
     * @return Newly created task schedule.
     * @throws IllegalArgumentException If the provided delay is invalid.
     * @throws NullPointerException If the provided callback is null.
     */
    public static @NotNull Schedule interval(double seconds, @NotNull Callback callback) {
        if (callback == null) throw new NullPointerException("Argument 'callback'");
        long ns = timeConvert(seconds);
        return new Task(callback, timeNow() + ns, ns).schedule();
    }

}
