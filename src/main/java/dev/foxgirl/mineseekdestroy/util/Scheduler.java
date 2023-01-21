package dev.foxgirl.mineseekdestroy.util;

import dev.foxgirl.mineseekdestroy.Game;
import org.jetbrains.annotations.NotNull;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Scheduler {

    private Scheduler() {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    public interface Callback {
        void invoke();
    }

    public interface Schedule {
        boolean cancel();
        long getTime();
        long getPeriod();
        @NotNull Callback getCallback();
    }

    private static final Object lock = new Object();
    private static final AtomicBoolean running = new AtomicBoolean(true);

    private static PriorityQueue<Task> queue = new PriorityQueue<>(16);
    private static Executor executor = new Executor();

    static {
        executor.setName("MnSnD-Scheduler");
        executor.setDaemon(false);
        executor.start();
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
                this.callback.invoke();
            } catch (Throwable err) {
                Game.LOGGER.error("Unhandled exception in scheduled task", err);
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

        @Override
        public @NotNull Callback getCallback() {
            return this.callback;
        }
    }

    private static final class Executor extends Thread {
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
            executor.interrupt();
            executor.join();
        } catch (InterruptedException err) {
            throw new RuntimeException(err);
        } finally {
            executor = null;
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

    public static @NotNull Schedule delay(long ms, @NotNull Callback callback) {
        if (ms < 0) {
            throw new IllegalArgumentException("Argument 'ms' is negative");
        }
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        return schedule(new Task(System.currentTimeMillis() + ms, -1, callback));
    }

    public static @NotNull Schedule interval(long ms, @NotNull Callback callback) {
        if (ms < 0) {
            throw new IllegalArgumentException("Argument 'ms' is negative");
        }
        if (callback == null) {
            throw new NullPointerException("Argument 'callback'");
        }
        return schedule(new Task(System.currentTimeMillis() + ms, ms, callback));
    }

    public static @NotNull Schedule delay(double seconds, @NotNull Callback callback) {
        if (seconds < 0.0 || !Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is negative/invalid");
        }
        return delay((long) (seconds * 1000.0D), callback);
    }

    public static @NotNull Schedule interval(double seconds, @NotNull Callback callback) {
        if (seconds < 0.0 || !Double.isFinite(seconds)) {
            throw new IllegalArgumentException("Argument 'seconds' is negative/invalid");
        }
        return interval((long) (seconds * 1000.0D), callback);
    }

}
