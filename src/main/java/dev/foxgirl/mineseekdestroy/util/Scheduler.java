package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Scheduler {

    private Scheduler() {
        throw new UnsupportedOperationException();
    }

    public interface Schedule {
        boolean cancel();
        long getTime();
        long getPeriod();
    }

    @FunctionalInterface
    public interface Callback {
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

        @Override
        public long getTime() {
            return this.time;
        }

        @Override
        public long getPeriod() {
            return this.period;
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

    private static long convert(double seconds) {
        if (!Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is invalid");
        }
        long ms = (long) (seconds * 1000.0D);
        if (ms < 0) {
            throw new IllegalArgumentException("Argument 'seconds' is negative");
        }
        return ms;
    }

    public static @NotNull Schedule delay(double seconds, @NotNull Callback callback) {
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        long ms = convert(seconds);
        return schedule(new Task(System.currentTimeMillis() + ms, -1, callback));
    }

    public static @NotNull Schedule interval(double seconds, @NotNull Callback callback) {
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        long ms = convert(seconds);
        return schedule(new Task(System.currentTimeMillis() + ms, ms, callback));
    }

}
