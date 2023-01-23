package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.concurrent.Executor;
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

    private static final Object lock = new Object();
    private static final AtomicBoolean running = new AtomicBoolean(true);

    private static PriorityQueue<Task> queue = new PriorityQueue<>(16);
    private static TaskThread thread = new TaskThread();

    static {
        thread.setName("MnSnD-Scheduler");
        thread.setDaemon(true);
        thread.start();
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
            Executor executor;
            try {
                executor = Game.getGame().getServer();
            } catch (NullPointerException cause) {
                Game.LOGGER.error("Scheduler failed to execute task, server not ready", cause);
                return;
            }
            try {
                executor.execute(new TaskRunnable(this));
            } catch (Exception cause) {
                Game.LOGGER.error("Scheduler failed to execute task", cause);
                return;
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

    private static final class TaskRunnable implements Runnable {
        private final Task task;

        private TaskRunnable(Task task) {
            this.task = task;
        }

        @Override
        public void run() {
            Task task = this.task;
            try {
                task.callback.invoke(task);
            } catch (Exception cause) {
                Game.LOGGER.error("Scheduler encountered exception while executing task", cause);
            }
        }
    }

    private static final class TaskThread extends Thread {
        @Override
        public void run() {
            try {
                while (running.get()) {
                    Task task;

                    synchronized (lock) {
                        while (queue.size() == 0) lock.wait();
                        task = queue.peek();

                        long timeCurrent = System.currentTimeMillis();
                        long timeExecution = task.time;

                        if (timeExecution > timeCurrent) {
                            lock.wait(timeExecution - timeCurrent);
                            continue;
                        }

                        queue.poll();

                        if (task.period >= 0) {
                            task.time += task.period;
                            queue.offer(task);
                        }
                    }

                    task.execute();
                }
            } catch (InterruptedException err) {
            }
        }
    }

    /**
     * Terminates this scheduler, discarding any currently scheduled tasks.
     */
    public static void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException err) {
            throw new RuntimeException(err);
        } finally {
            thread = null;
            queue.clear();
            queue = null;
        }
    }

    private static Task schedule(Task task) {
        if (!running.get()) {
            throw new IllegalStateException("Attempted to schedule new task on stopped Scheduler");
        }
        synchronized (lock) {
            queue.offer(task);
            if (queue.peek() == task) lock.notify();
        }
        return task;
    }

    private static boolean remove(Task task) {
        if (!running.get()) {
            return false;
        }
        synchronized (lock) {
            return queue.remove(task);
        }
    }

    private static long timeNow() {
        return System.currentTimeMillis();
    }

    private static long timeConvert(double seconds) {
        if (!Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is invalid");
        }
        long ms = (long) (seconds * 1000.0D);
        if (ms < 0) {
            throw new IllegalArgumentException("Argument 'seconds' is negative");
        }
        return ms;
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
        long ms = timeConvert(seconds);
        return schedule(new Task(timeNow() + ms, -1, callback));
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
        long ms = timeConvert(seconds);
        return schedule(new Task(timeNow() + ms, ms, callback));
    }

}
