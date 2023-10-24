package dev.foxgirl.mineseekdestroy.util.async;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Queue;

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

    private static final Object TICKS_LOCK = new Object();
    private static int TICKS_CURRENT = 0;

    private static int ticksNow() {
        synchronized (TICKS_LOCK) { return TICKS_CURRENT; }
    }
    private static void ticksIncrement() {
        synchronized (TICKS_LOCK) { TICKS_CURRENT++; }
    }

    private static final LinkedHashSet<Executable> EXECUTE_NEXT = new LinkedHashSet<>(128);

    private static class Executable implements Schedule {
        private final Callback callback;

        private Executable(Callback callback) {
            this.callback = callback;
        }

        final void execute() {
            try {
                callback.invoke(this);
            } catch (Throwable cause) {
                cancel();
                Game.LOGGER.error("Unhandled exception in scheduled task", cause);
            }
        }

        Schedule schedule() {
            synchronized (EXECUTE_NEXT) { EXECUTE_NEXT.add(this); }
            return this;
        }

        @Override
        public boolean cancel() {
            synchronized (EXECUTE_NEXT) { return EXECUTE_NEXT.remove(this); }
        }
    }

    private static abstract class Task<T extends Task<T>> extends Executable implements Comparable<T> {
        private Task(Callback callback) {
            super(callback);
        }

        abstract Queue<T> getWaitingQueue();

        abstract boolean shouldContinue();
        abstract boolean shouldReschedule();

        @SuppressWarnings("unchecked")
        final boolean attemptContinue() {
            // Note that attemptContinue is only ever called while synchronized on this queue
            var waitingQueue = getWaitingQueue();

            // Ensure this task is ready to be executed
            if (!shouldContinue()) {
                // Return false to indicate that this task is not ready, and we should stop traversing the queue
                return false;
            }

            // Remove it from the waiting queue
            waitingQueue.remove(this);

            // Check if this task should be rescheduled, and re-add it to the waiting queue if so
            // Note that shouldReschedule must update the task's timing information if it returns true
            if (shouldReschedule()) {
                waitingQueue.offer((T) this);
            }

            // Call Executable's schedule method to add this task to the execute-next set
            super.schedule();

            // Return true to indicate that we should continue onto the next task in the queue
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        final Schedule schedule() {
            var waitingQueue = getWaitingQueue();
            synchronized (waitingQueue) { waitingQueue.offer((T) this); }
            return this;
        }

        @Override
        public final boolean cancel() {
            var removed = super.cancel();
            var waitingQueue = getWaitingQueue();
            synchronized (waitingQueue) {
                var removedWaiting = waitingQueue.remove(this);
                if (removedWaiting) removed = true;
            }
            return removed;
        }
    }

    private static final PriorityQueue<TimeBasedTask> WAITING_TIME_QUEUE = new PriorityQueue<>(128);
    private static final PriorityQueue<TickBasedTask> WAITING_TICK_QUEUE = new PriorityQueue<>(128);

    private static final class TimeBasedTask extends Task<TimeBasedTask> {
        private long time;
        private final long period;

        private TimeBasedTask(Callback callback, long time, long period) {
            super(callback);
            if (time < 0) throw new IllegalArgumentException("Invalid calculated time");
            if (period < -1) throw new IllegalArgumentException("Invalid calculated time period");
            this.time = time;
            this.period = period;
        }


        @Override
        Queue<TimeBasedTask> getWaitingQueue() {
            return WAITING_TIME_QUEUE;
        }

        @Override
        boolean shouldContinue() {
            return time <= timeNow();
        }

        @Override
        boolean shouldReschedule() {
            if (period > 0) {
                time += period;
                return true;
            }
            return false;
        }

        @Override
        public int compareTo(@NotNull Scheduler.TimeBasedTask other) {
            return Long.compare(this.time, other.time);
        }
    }

    private static final class TickBasedTask extends Task<TickBasedTask> {
        private int ticks;
        private final int period;

        private TickBasedTask(Callback callback, int ticks, int period) {
            super(callback);
            if (ticks < 0) throw new IllegalArgumentException("Invalid calculated ticks");
            if (period < -1) throw new IllegalArgumentException("Invalid calculated tick period");
            this.ticks = ticks;
            this.period = period;
        }

        @Override
        Queue<TickBasedTask> getWaitingQueue() {
            return WAITING_TICK_QUEUE;
        }

        @Override
        boolean shouldContinue() {
            return ticks <= TICKS_CURRENT;
        }

        @Override
        boolean shouldReschedule() {
            if (period > 0) {
                ticks += period;
                return true;
            }
            return false;
        }

        @Override
        public int compareTo(@NotNull TickBasedTask other) {
            return Integer.compare(this.ticks, other.ticks);
        }
    }

    private static <T extends Task<T>> void updateWaitingQueue(Queue<T> waitingQueue) {
        synchronized (waitingQueue) {
            while (!waitingQueue.isEmpty()) {
                boolean shouldContinue = waitingQueue.peek().attemptContinue();
                if (!shouldContinue) break;
            }
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

        updateWaitingQueue(WAITING_TIME_QUEUE);
        updateWaitingQueue(WAITING_TICK_QUEUE);

        Executable[] executables;

        synchronized (EXECUTE_NEXT) {
            executables = EXECUTE_NEXT.toArray(new Executable[0]);
            EXECUTE_NEXT.clear();
        }

        for (var executable : executables) {
            try {
                executable.execute();
            } catch (Throwable cause) {
                executable.cancel();
                Game.LOGGER.error("Unhandled exception in scheduled task", cause);
            }
        }

        ticksIncrement();
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
        return new TimeBasedTask(callback, timeNow() + ns, -1).schedule();
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
        return new TimeBasedTask(callback, timeNow() + ns, ns).schedule();
    }

    /**
     * Schedules a task to be run (once) at some time in the future in units of
     * ticks.
     * @param ticks Delay in ticks to wait before execution.
     * @param callback Task callback to invoke.
     * @return Newly created task schedule.
     * @throws IllegalArgumentException If the provided delay is invalid.
     * @throws NullPointerException If the provided callback is null.
     */
    public static @NotNull Schedule delayTicks(int ticks, @NotNull Callback callback) {
        if (callback == null) throw new NullPointerException("Argument 'callback'");
        if (ticks < 0) throw new IllegalArgumentException("Argument 'ticks' is negative");
        return new TickBasedTask(callback, ticksNow() + ticks, -1).schedule();
    }
    /**
     * Schedules a task to be run multiple times on an interval in units of
     * ticks.
     * @param ticks Delay in ticks to wait before and between executions.
     * @param callback Task callback to invoke.
     * @return Newly created task schedule.
     * @throws IllegalArgumentException If the provided delay is invalid.
     * @throws NullPointerException If the provided callback is null.
     */
    public static @NotNull Schedule intervalTicks(int ticks, @NotNull Callback callback) {
        if (callback == null) throw new NullPointerException("Argument 'callback'");
        if (ticks < 0) throw new IllegalArgumentException("Argument 'ticks' is negative");
        return new TickBasedTask(callback, ticksNow() + ticks, ticks).schedule();
    }

}
