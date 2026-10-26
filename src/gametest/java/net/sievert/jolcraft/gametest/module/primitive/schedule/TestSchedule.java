package net.sievert.jolcraft.gametest.module.primitive.schedule;

import java.util.ArrayList;
import java.util.List;

import net.sievert.jolcraft.module.primitive.schedule.Schedule;

/**
 * The game-test mod's schedule as its root built it, and the failures the root's handler collected, held here for
 * the scenes: the adapter creates scene classes through a no-argument constructor and hands them nothing, so the dev mod's plumbing hands them what its root built.
 */
public final class TestSchedule {

    private static final List<Throwable> FAILURES = new ArrayList<>();
    private static Schedule schedule;

    private TestSchedule() {
    }

    public static void attach(Schedule built) {
        schedule = built;
    }

    public static Schedule schedule() {
        return schedule;
    }

    public static List<Throwable> failures() {
        return FAILURES;
    }
}
