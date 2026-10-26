package net.sievert.jolcraft.module.primitive.schedule;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.sievert.jolcraft.stereotype.Implementation;

/**
 * {@link Schedule} over the server tick: one {@link Queue} per running server, ticked at the end of each server
 * tick against the server's own tick count, so a delay is the same from every place in a tick a task is scheduled.
 */
@Implementation
public final class ModSchedule implements Schedule {

    private final Map<MinecraftServer, Queue> queues = new IdentityHashMap<>();
    private final Set<MinecraftServer> stopped = Collections.newSetFromMap(new WeakHashMap<>());
    private final Failure failure;
    private boolean registered;

    public ModSchedule(Failure failure) {
        this.failure = failure;
    }

    @Override
    public Task after(MinecraftServer server, int ticks, Runnable task) {
        return queue(server).after(server.getTickCount(), ticks, task);
    }

    @Override
    public Task every(MinecraftServer server, int interval, Runnable task) {
        return queue(server).every(server.getTickCount(), interval, task);
    }

    private Queue queue(MinecraftServer server) {
        if (!server.isSameThread()) {
            throw new IllegalStateException("tasks are scheduled from the server's thread; another thread hands the server its work first");
        }
        if (stopped.contains(server)) {
            throw new IllegalStateException("the server has stopped; nothing runs on it again");
        }
        if (!registered) {
            throw new IllegalStateException("the schedule is not attached to the game bus; the root attaches it once, before anything schedules");
        }
        return queues.computeIfAbsent(server, _ -> new Queue(failure));
    }

    @Override
    public void register(IEventBus gameBus) {
        if (registered) {
            throw new IllegalStateException("the schedule was attached to the game bus twice");
        }
        registered = true;
        gameBus.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        gameBus.addListener(ServerStoppedEvent.class, event -> stopped(event.getServer()));
    }

    /** The end of a server tick: its queue's turn. */
    void tick(MinecraftServer server) {
        Queue queue = queues.get(server);
        if (queue != null) {
            queue.tick(server.getTickCount());
        }
    }

    /** A server stopped: every task it still had is done without running, and it takes no more. */
    void stopped(MinecraftServer server) {
        Queue queue = queues.remove(server);
        if (queue != null) {
            queue.drop();
        }
        stopped.add(server);
    }

    /** How many tasks are queued across the running servers, cancelled ones included until the next tick sweeps them; for tests. */
    public int pending() {
        return queues.values().stream().mapToInt(Queue::pending).sum();
    }
}
