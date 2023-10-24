package dev.foxgirl.mineseekdestroy.util.async;

import dev.foxgirl.mineseekdestroy.Game;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    private static double validateSeconds(double seconds) {
        if (!Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is not finite");
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("Argument 'seconds' is negative");
        }
        return seconds;
    }

    private static final long TIME_BASE = System.nanoTime();

    private static long timeNow() {
        return System.nanoTime() - TIME_BASE;
    }
    private static long timeConvert(double seconds) {
        var ns = (long) (validateSeconds(seconds) * 1e+9);
        if (ns < 0) {
            throw new IllegalStateException("Converted time value is invalid");
        }
        return ns;
    }

    private static int TICKS_CURRENT = 0;
    private static final Object TICKS_LOCK = new Object();

    private static int ticksNow() {
        synchronized (TICKS_LOCK) {
            return TICKS_CURRENT;
        }
    }

    private static final ArrayList<Executable> EXECUTE_QUEUE = new ArrayList<>(128);

    private static final PriorityQueue<TimeBasedTask> WAITING_TIME_QUEUE = new PriorityQueue<>(128);
    private static final PriorityQueue<TickBasedTask> WAITING_TICK_QUEUE = new PriorityQueue<>(128);

    private static abstract class Executable implements Schedule {
        abstract void execute();
        abstract Schedule schedule();

        @Override
        public boolean cancel() {
            synchronized (EXECUTE_QUEUE) {
                return EXECUTE_QUEUE.remove(this);
            }
        }
    }

    private static abstract class DirectExecutable extends Executable {
        @Override
        Schedule schedule() {
            synchronized (EXECUTE_QUEUE) {
                EXECUTE_QUEUE.add(this);
            }
            return this;
        }
    }

    private static final class CallbackDirectExecutable extends DirectExecutable {
        private final Callback callback;

        private CallbackDirectExecutable(Callback callback) {
            this.callback = callback;
        }

        @Override
        void execute() {
            callback.invoke(this);
        }
    }

    private static final class ContinuationDirectExecutable extends DirectExecutable {
        private final Continuation<Unit> continuation;

        private ContinuationDirectExecutable(Continuation<Unit> continuation) {
            this.continuation = continuation;
        }

        @Override
        void execute() {
            continuation.resumeWith(Unit.INSTANCE);
        }
    }

    private static abstract class Task<T> extends Executable implements Comparable<T> {
        private final Callback callback;

        private Task(Callback callback) {
            this.callback = callback;
        }

        @Override
        public final void execute() {
            callback.invoke(this);
        }

        abstract boolean tryContinue();

        protected final void enqueueContinue() {
            synchronized (EXECUTE_QUEUE) {
                if (!EXECUTE_QUEUE.contains(this)) EXECUTE_QUEUE.add(this);
            }
        }

        protected final boolean cancel(Queue<? extends Task<?>> waitingQueue) {
            var removed = super.cancel();
            synchronized (waitingQueue) {
                var removedWaiting = waitingQueue.remove(this);
                if (removedWaiting) removed = true;
            }
            return removed;
        }
    }

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
        Schedule schedule() {
            synchronized (WAITING_TIME_QUEUE) {
                WAITING_TIME_QUEUE.offer(this);
            }
            return this;
        }

        @Override
        boolean tryContinue() {
            if (time > timeNow()) return true;

            WAITING_TIME_QUEUE.poll();

            if (period > 0) {
                time += period;
                WAITING_TIME_QUEUE.offer(this);
            }

            enqueueContinue();
            return false;
        }

        @Override
        public boolean cancel() {
            return cancel(WAITING_TIME_QUEUE);
        }

        @Override
        public int compareTo(@NotNull TimeBasedTask other) {
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
        Schedule schedule() {
            synchronized (WAITING_TICK_QUEUE) {
                WAITING_TICK_QUEUE.offer(this);
            }
            return this;
        }

        @Override
        boolean tryContinue() {
            if (ticks > TICKS_CURRENT) return true;

            WAITING_TICK_QUEUE.poll();

            if (period > 0) {
                ticks += period;
                WAITING_TICK_QUEUE.add(this);
            }

            enqueueContinue();
            return false;
        }

        @Override
        public boolean cancel() {
            return cancel(WAITING_TICK_QUEUE);
        }

        @Override
        public int compareTo(@NotNull TickBasedTask other) {
            return Integer.compare(this.ticks, other.ticks);
        }
    }

    private static void updateWaitingQueue(Queue<? extends Task<?>> waitingQueue) {
        synchronized (waitingQueue) {
            while (!waitingQueue.isEmpty()) {
                var notContinuing = waitingQueue.peek().tryContinue();
                if (notContinuing) break;
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

        synchronized (EXECUTE_QUEUE) {
            executables = EXECUTE_QUEUE.toArray(new Executable[0]);
            EXECUTE_QUEUE.clear();
        }

        for (var executable : executables) {
            try {
                executable.execute();
            } catch (Throwable cause) {
                executable.cancel();
                Game.LOGGER.error("Unhandled exception in scheduled task", cause);
            }
        }

        synchronized (TICKS_LOCK) {
            TICKS_CURRENT++;
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
        return new CallbackDirectExecutable(callback).schedule();
    }
    /**
     * Schedules a task to be run (once) as soon as possible.
     * @param continuation Task continuation to resume.
     * @return Newly created task schedule.
     * @throws NullPointerException If the provided continuation is null.
     */
    public static @NotNull Schedule now(@NotNull Continuation<Unit> continuation) {
        if (continuation == null) throw new NullPointerException("Argument 'continuation'");
        return new ContinuationDirectExecutable(continuation).schedule();
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
