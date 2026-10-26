package net.sievert.jolcraft.module.primitive.schedule;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueueTest {

    private final List<String> failures = new ArrayList<>();
    private final Queue queue = new Queue((_, thrown) -> failures.add(thrown.getMessage()));
    private final List<String> ran = new ArrayList<>();

    @Test
    void aTaskRunsOnceAtTheTickItIsDue() {
        Schedule.Task task = queue.after(10, 3, () -> ran.add("a"));
        queue.tick(10);
        queue.tick(11);
        queue.tick(12);
        Assertions.assertEquals(List.of(), ran, "not before tick thirteen");
        Assertions.assertFalse(task.isDone());
        queue.tick(13);
        Assertions.assertEquals(List.of("a"), ran);
        Assertions.assertTrue(task.isDone());
        queue.tick(14);
        Assertions.assertEquals(List.of("a"), ran, "once");
        Assertions.assertEquals(0, queue.pending());
    }

    @Test
    void theDelayIsTheSameWhetherScheduledDuringTheTickOrAfterIt() {
        queue.after(10, 1, () -> ran.add("during"));
        queue.tick(10);
        Assertions.assertEquals(List.of(), ran, "the tick a task was scheduled in does not count");
        queue.tick(11);
        Assertions.assertEquals(List.of("during"), ran);
    }

    @Test
    void aRepeatingTaskRunsEveryIntervalUntilCancelledAndDoesNotBurstAfterALag() {
        Schedule.Task task = queue.every(10, 2, () -> ran.add("r"));
        for (long tick = 10; tick <= 16; tick++) {
            queue.tick(tick);
        }
        Assertions.assertEquals(List.of("r", "r", "r"), ran, "at twelve, fourteen and sixteen");
        queue.tick(30);
        Assertions.assertEquals(4, ran.size(), "one run for a long gap, then from thirty on");
        queue.tick(31);
        Assertions.assertEquals(4, ran.size());
        queue.tick(32);
        Assertions.assertEquals(5, ran.size());
        task.cancel();
        queue.tick(34);
        Assertions.assertEquals(5, ran.size(), "cancelled");
        Assertions.assertTrue(task.isDone());
        Assertions.assertEquals(0, queue.pending());
    }

    @Test
    void aTaskMayCancelItselfWhileRunning() {
        Schedule.Task[] self = new Schedule.Task[1];
        self[0] = queue.every(0, 1, () -> {
            ran.add("once");
            self[0].cancel();
        });
        queue.tick(1);
        queue.tick(2);
        Assertions.assertEquals(List.of("once"), ran);
    }

    @Test
    void aFailingTaskIsReportedAndTheOthersStillRun() {
        queue.after(0, 1, () -> {
            throw new IllegalStateException("boom");
        });
        queue.after(0, 1, () -> ran.add("after"));
        queue.tick(1);
        Assertions.assertEquals(List.of("boom"), failures);
        Assertions.assertEquals(List.of("after"), ran);
    }

    @Test
    void aTaskScheduledDuringATickWaitsForTheNext() {
        queue.after(0, 1, () -> queue.after(1, 1, () -> ran.add("nested")));
        queue.tick(1);
        Assertions.assertEquals(List.of(), ran);
        queue.tick(2);
        Assertions.assertEquals(List.of("nested"), ran);
    }

    @Test
    void tasksRunInSchedulingOrderWaitAtLeastOneTickAndAreNotNull() {
        queue.after(0, 1, () -> ran.add("first"));
        queue.after(0, 1, () -> ran.add("second"));
        queue.tick(1);
        Assertions.assertEquals(List.of("first", "second"), ran);
        Assertions.assertThrows(IllegalArgumentException.class, () -> queue.after(0, 0, () -> { }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> queue.every(0, -2, () -> { }));
        Assertions.assertThrows(NullPointerException.class, () -> queue.after(0, 1, null));
    }

    @Test
    void droppingMarksEveryWaitingTaskDoneWithoutRunningIt() {
        Schedule.Task task = queue.after(0, 5, () -> ran.add("never"));
        queue.drop();
        Assertions.assertTrue(task.isDone());
        queue.tick(10);
        Assertions.assertEquals(List.of(), ran);
    }
}
