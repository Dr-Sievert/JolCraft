package net.sievert.jolcraft.gametest.module.primitive.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.primitive.schedule.ModSchedule;
import net.sievert.jolcraft.module.primitive.schedule.Schedule;

/**
 * The schedule primitive on the real server: a task scheduled in a tick runs at the end of the tick that comes the
 * asked ticks later and once only, a repeating task runs every interval until cancelled, a failing task is
 * reported while the next still runs, a stopped server drops its tasks and takes no more, and another thread is refused.
 */
public final class ScheduleScenes {

    @Scene(maxTicks = 40)
    public void aTaskRunsAtTheEndOfTheTickThatComesTheAskedTicksLaterAndOnce(TestScene scene) {
        MinecraftServer server = scene.level().getServer();
        List<Long> runs = new ArrayList<>();
        Schedule.Task task = TestSchedule.schedule().after(server, 3, () -> runs.add(scene.level().getGameTime()));
        scene.runAtTick(3, () -> scene.check("not yet during tick three", () -> scene.assertTrue(runs.isEmpty() && !task.isDone(), "runs " + runs)));
        scene.runAtTick(4, () -> scene.check("run once by tick four", () -> scene.assertTrue(runs.size() == 1 && task.isDone(), "runs " + runs)));
        scene.runAtTick(8, () -> {
            scene.check("and never again", () -> scene.assertEquals(1, runs.size(), "runs"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 40)
    public void aRepeatingTaskRunsEveryIntervalUntilCancelled(TestScene scene) {
        MinecraftServer server = scene.level().getServer();
        List<Integer> runs = new ArrayList<>();
        Schedule.Task task = TestSchedule.schedule().every(server, 2, () -> runs.add(runs.size() + 1));
        scene.runAtTick(6, () -> scene.check("two runs during tick six", () -> scene.assertEquals(List.of(1, 2), runs, "runs")));
        scene.runAtTick(7, () -> {
            scene.check("three by tick seven", () -> scene.assertEquals(List.of(1, 2, 3), runs, "runs"));
            task.cancel();
        });
        scene.runAtTick(12, () -> {
            scene.check("none after cancelling", () -> scene.assertEquals(3, runs.size(), "runs"));
            scene.check("the task is done", () -> scene.assertTrue(task.isDone(), "done"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 40)
    public void aFailingTaskIsReportedToTheRootAndTheNextStillRuns(TestScene scene) {
        MinecraftServer server = scene.level().getServer();
        int reported = TestSchedule.failures().size();
        List<String> runs = new ArrayList<>();
        TestSchedule.schedule().after(server, 1, () -> {
            throw new IllegalStateException("a task that breaks");
        });
        TestSchedule.schedule().after(server, 1, () -> runs.add("next"));
        scene.runAtTick(2, () -> {
            scene.check("the failure reached the root's handler", () -> scene.assertTrue(
                    TestSchedule.failures().size() == reported + 1 && TestSchedule.failures().getLast().getMessage().equals("a task that breaks"), "failures"));
            scene.check("the next task still ran", () -> scene.assertEquals(List.of("next"), runs, "runs"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 20)
    public void aStoppedServerDropsItsTasksAndTakesNoMoreAndAnotherThreadIsRefused(TestScene scene) {
        MinecraftServer server = scene.level().getServer();
        IEventBus bus = BusBuilder.builder().build();
        ModSchedule own = new ModSchedule((_, _) -> { });
        own.register(bus);
        List<String> runs = new ArrayList<>();
        Schedule.Task later = own.after(server, 5, () -> runs.add("later"));
        Schedule.Task often = own.every(server, 2, () -> runs.add("often"));
        scene.check("two tasks wait", () -> scene.assertEquals(2, own.pending(), "pending"));
        bus.post(new ServerStoppedEvent(server));
        scene.check("both are done without running", () -> scene.assertTrue(later.isDone() && often.isDone() && runs.isEmpty() && own.pending() == 0, "after the stop"));
        scene.check("nothing more is taken for the stopped server", () -> scene.assertTrue(refuses(() -> own.after(server, 1, () -> { })), "a refusal"));

        ModSchedule loose = new ModSchedule((_, _) -> { });
        scene.check("a schedule not attached to the game bus takes nothing", () -> scene.assertTrue(refuses(() -> loose.after(server, 1, () -> { })), "a refusal"));
        ModSchedule fresh = new ModSchedule((_, _) -> { });
        fresh.register(BusBuilder.builder().build()); // attached, so only the thread can be the reason for a refusal
        AtomicReference<Throwable> refused = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                fresh.after(server, 1, () -> { });
            } catch (Throwable thrown) {
                refused.set(thrown);
            }
        }, "test-other");
        other.start();
        try {
            other.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while the other thread scheduled", interrupted);
        }
        scene.check("another thread is refused", () -> scene.assertTrue(refused.get() instanceof IllegalStateException && refused.get().getMessage().contains("thread"), "refusal " + refused.get()));
        scene.succeed();
    }

    private static boolean refuses(Runnable call) {
        try {
            call.run();
            return false;
        } catch (IllegalStateException refused) {
            return true;
        }
    }
}
