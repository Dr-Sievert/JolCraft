package net.sievert.jolcraft.module.primitive.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sievert.jolcraft.stereotype.Internal;

/**
 * The tasks of one server, in scheduling order, each due at an absolute tick count. The end of a tick runs every
 * task due by then; a repeating task is wound up from that tick again, so a server that fell behind does not
 * burst; a task scheduled while the tick runs waits for the next one. A task that throws is reported and the
 * rest of the tick goes on.
 */
@Internal
final class Queue {

    private final Schedule.Failure failure;
    private final List<Entry> entries = new ArrayList<>();

    Queue(Schedule.Failure failure) {
        this.failure = failure;
    }

    /** A task due {@code ticks} ticks after tick {@code now}. */
    Schedule.Task after(long now, int ticks, Runnable task) {
        return add(now, ticks, 0, task);
    }

    Schedule.Task every(long now, int interval, Runnable task) {
        return add(now, interval, interval, task);
    }

    private Schedule.Task add(long now, int ticks, int interval, Runnable task) {
        Objects.requireNonNull(task, "a task");
        if (ticks < 1) {
            throw new IllegalArgumentException("a task waits at least one tick, not " + ticks);
        }
        Entry entry = new Entry(task, now + ticks, interval);
        entries.add(entry);
        return entry;
    }

    /** The end of tick {@code now}: every task due by it runs. */
    void tick(long now) {
        for (Entry entry : List.copyOf(entries)) {
            if (entry.done) {
                entries.remove(entry);
                continue;
            }
            if (entry.due > now) {
                continue;
            }
            try {
                entry.task.run();
            } catch (Throwable thrown) {
                failure.failed(entry.task, thrown);
            }
            if (entry.interval > 0 && !entry.done) {
                entry.due = now + entry.interval;
            } else {
                entry.done = true;
                entries.remove(entry);
            }
        }
    }

    void drop() {
        entries.forEach(entry -> entry.done = true);
        entries.clear();
    }

    int pending() {
        return entries.size();
    }

    private static final class Entry implements Schedule.Task {
        private final Runnable task;
        private final int interval;
        private long due;
        private boolean done;

        private Entry(Runnable task, long due, int interval) {
            this.task = task;
            this.due = due;
            this.interval = interval;
        }

        @Override
        public void cancel() {
            done = true;
        }

        @Override
        public boolean isDone() {
            return done;
        }
    }
}
