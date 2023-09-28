package dev.foxgirl.mineseekdestroy.util.async;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler provides a facility for scheduling tasks to be run on the main
 * server thread at a later date. Tasks may be scheduled for one-time
 * execution, or for repeated execution at regular intervals. It is similar in
 * functionality to {@link java.util.Timer}.
 */
public final class Scheduler {

    private Scheduler() {
        throw new UnsupportedOperationException();
    }

    /**
     * Schedule represents a task that will execute in the future.
     */
    public interface Schedule {
        /**
         * Cancels this schedule.
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

    private static final class Task implements Comparable<Task>, Schedule {
        private long time;
        private final long period;
        private final Callback callback;

        private Task(long time, long period, Callback callback) {
            this.time = time;
            this.period = period;
            this.callback = callback;
        }

        private void execute() {
            try {
                this.callback.invoke(this);
            } catch (Throwable cause) {
                Game.LOGGER.error("Unhandled exception in scheduled task", cause);
                cancel();
            }
        }

        @Override
        public int compareTo(Task other) {
            return Long.compare(this.time, other.time);
        }

        @Override
        public boolean cancel() {
            return remove(this);
        }
    }

    private static final long timeBase = System.nanoTime();
    private static long timeNow() {
        return System.nanoTime() - timeBase;
    }
    private static long timeConvert(double seconds) {
        if (!Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is not finite");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("Argument 'seconds' is negative");
        }
        long ns = (long) (seconds * 1e+9);
        if (ns < 0) {
            throw new IllegalStateException("Converted time value is invalid");
        }
        return ns;
    }

    private static final Object LOCK = new Object();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    private static final PriorityQueue<Task> QUEUE = new PriorityQueue<>(128);

    /**
     * Executes this scheduler's currently scheduled tasks that are ready to
     * be executed. Must be called from the main server thread.
     * @throws IllegalStateException If invoked from the wrong thread.
     */
    public static void update() {
        if (!Game.getGame().getServer().isOnThread()) {
            throw new IllegalStateException("Scheduler execution started from the wrong thread");
        }

        while (RUNNING.get()) {
            Task task;

            synchronized (LOCK) {
                if (QUEUE.isEmpty()) return;
                task = QUEUE.peek();

                long timeCurrent = timeNow();
                long timeExecution = task.time;
                if (timeExecution > timeCurrent) return;

                QUEUE.poll();

                if (task.period >= 0) {
                    task.time += task.period;
                    QUEUE.offer(task);
                }
            }

            task.execute();
        }
    }

    /**
     * Terminates this scheduler, discarding any currently scheduled tasks.
     */
    public static void stop() {
        if (!RUNNING.getAndSet(false)) {
            return;
        }
        synchronized (LOCK) {
            QUEUE.clear();
        }
    }

    private static Task schedule(Task task) {
        if (!RUNNING.get()) {
            throw new IllegalStateException("Cannot schedule new task on stopped scheduler");
        }
        synchronized (LOCK) {
            QUEUE.offer(task);
        }
        return task;
    }

    private static boolean remove(Task task) {
        if (!RUNNING.get()) {
            return false;
        }
        synchronized (LOCK) {
            return QUEUE.remove(task);
        }
    }

    /**
     * Schedules a task to be run (once) as soon as possible.
     * @param callback Task callback to invoke.
     * @return Newly created task schedule.
     * @throws NullPointerException If the provided callback is null.
     */
    public static @NotNull Schedule now(@NotNull Callback callback) {
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        return schedule(new Task(timeNow(), -1, callback));
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
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        long ns = timeConvert(seconds);
        return schedule(new Task(timeNow() + ns, -1, callback));
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
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        long ns = timeConvert(seconds);
        return schedule(new Task(timeNow() + ns, ns, callback));
    }

}
