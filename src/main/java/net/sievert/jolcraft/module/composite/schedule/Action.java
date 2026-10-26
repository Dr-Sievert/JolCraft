package net.sievert.jolcraft.module.composite.schedule;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.schedule.Schedule;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * A transaction an actor performs over several ticks: a dwarf that walks to a player, inspects the offer, then
 * hands over the goods. The steps run in order, each after its wait, on the actor's server; the action ends early
 * when the actor leaves the world, dies, is cancelled or a step throws, and then the recovery runs once, so whatever
 * the steps took or promised is put back; a server stop drops the steps without recovery, the world being saved as it
 * stands. Built from {@code schedule}, which runs the steps, and {@code log}.
 */
@Composite(Dictionary.ACTION)
public final class Action implements Module {

    private final Schedule schedule;
    private final Log log;

    public Action(Schedule schedule, Log log) {
        this.schedule = schedule;
        this.log = log.tagged(this);
    }

    /** Starts the steps for the actor; the recovery runs if the action ends before the last step ran. */
    public Running start(Entity actor, List<Step> steps, Runnable recovery) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("an action has at least one step");
        }
        Objects.requireNonNull(recovery, "a recovery");
        MinecraftServer server = actor.level().getServer();
        if (server == null) {
            throw new IllegalStateException("an action runs on a server, not on a client level");
        }
        Running running = new Running(actor, List.copyOf(steps), recovery, server);
        running.next();
        return running;
    }

    /** One step: how many ticks after the previous step it runs, and what it does with the actor. */
    public record Step(int ticks, Consumer<Entity> run) {
        public Step {
            Objects.requireNonNull(run, "what the step does");
            if (ticks < 1) {
                throw new IllegalArgumentException("a step waits at least one tick, not " + ticks);
            }
        }
    }

    /** A started action: cancel it, or ask whether it finished and how. */
    public final class Running {
        private final Entity actor;
        private final List<Step> steps;
        private final Runnable recovery;
        private final MinecraftServer server;
        private int at;
        private Schedule.Task waiting;
        private boolean done;
        private boolean recovered;

        private Running(Entity actor, List<Step> steps, Runnable recovery, MinecraftServer server) {
            this.actor = actor;
            this.steps = steps;
            this.recovery = recovery;
            this.server = server;
        }

        private void next() {
            Step step = steps.get(at);
            waiting = schedule.after(server, step.ticks(), () -> {
                if (done) {
                    return;
                }
                if (actor.isRemoved() || !actor.isAlive()) {
                    log.debug("{} left before step {} of {}; recovering", actor.getName().getString(), at + 1, steps.size());
                    end(true);
                    return;
                }
                try {
                    step.run().accept(actor);
                } catch (RuntimeException | Error thrown) {
                    log.error("{} failed step {} of {}; recovering", actor.getName().getString(), at + 1, steps.size(), thrown);
                    end(true);
                    return;
                }
                at++;
                if (at < steps.size()) {
                    next();
                } else {
                    end(false);
                }
            });
        }

        /** Ends the action before its last step; the recovery runs once. Nothing happens to an action already over. */
        public void cancel() {
            if (!done) {
                waiting.cancel();
                end(true);
            }
        }

        private void end(boolean early) {
            done = true;
            if (early && !recovered) {
                recovered = true;
                recovery.run();
            }
        }

        /** Whether the action is over, by its last step, by cancel or by losing the actor. */
        public boolean isDone() {
            return done;
        }

        /** Whether the action ended early, by cancel, a lost actor or a failed step, and its recovery ran. */
        public boolean wasRecovered() {
            return recovered;
        }

        /** How many steps ran. */
        public int stepsRun() {
            return at;
        }
    }
}
