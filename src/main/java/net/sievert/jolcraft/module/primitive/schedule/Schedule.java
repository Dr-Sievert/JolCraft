package net.sievert.jolcraft.module.primitive.schedule;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Work for later ticks on a server: once after a number of ticks, or every so many ticks until cancelled. Tasks
 * run on the server's thread at the end of a tick, in the order they were scheduled, and are dropped when the
 * server stops, so nothing outlives a world. Scheduling is done from the server's thread; a task that throws is
 * reported through the {@link Failure} the root gives the implementation and the other tasks still run.
 */
@Primitive(Dictionary.SCHEDULE)
public interface Schedule extends Module {

    /** Runs the task once, at the end of the tick that comes {@code ticks} ticks after the current one, wherever in the current tick it is scheduled; {@code ticks} is at least 1. */
    Task after(MinecraftServer server, int ticks, Runnable task);

    /** Runs the task every {@code interval} ticks, the first time one interval after the current tick, until cancelled or the server stops; a server that fell behind runs it once and counts from there. */
    Task every(MinecraftServer server, int interval, Runnable task);

    /** Attaches to the game bus: the tick that runs the tasks and the server stop that drops them. The root calls it once. */
    void register(IEventBus gameBus);

    /** A scheduled task: cancel it before it runs, or between runs when it repeats. */
    interface Task {

        void cancel();

        /** Whether the task will not run again: it ran, was cancelled, or its server stopped. */
        boolean isDone();
    }

    /** What the root does with a task that threw; a primitive takes no log, so the root adapts one. */
    @FunctionalInterface
    interface Failure {

        void failed(Runnable task, Throwable thrown);
    }
}
