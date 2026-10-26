package net.sievert.jolcraft.gametest.module.composite.schedule;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;
import net.sievert.jolcraft.module.composite.schedule.Action;

/**
 * The action composite on a real actor: the steps run in order at their ticks and the action is done after the
 * last; a cancel mid-way runs the recovery once and no further step; a step that throws, an actor that dies and
 * an actor that leaves the world mid-way are recovered too.
 */
public final class ActionScenes {

    @Scene(maxTicks = 60)
    public void theStepsRunInOrderAndTheActionIsDoneAfterTheLast(TestScene scene) {
        ArmorStand actor = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        List<String> ran = new ArrayList<>();
        Action.Running running = JolCraftGameTests.action().start(actor,
                List.of(new Action.Step(2, stand -> ran.add(stand == actor ? "first" : "wrong actor")), new Action.Step(2, _ -> ran.add("second"))), () -> ran.add("recovered"));
        scene.runAtTick(2, () -> scene.check("nothing during tick two", () -> scene.assertEquals(List.of(), ran, "ran")));
        scene.runAtTick(3, () -> scene.check("the first step by tick three", () -> scene.assertEquals(List.of("first"), ran, "ran")));
        scene.runAtTick(4, () -> scene.check("the second still waits at tick four", () -> scene.assertEquals(List.of("first"), ran, "ran")));
        scene.runAtTick(5, () -> {
            scene.check("the second by tick five", () -> scene.assertEquals(List.of("first", "second"), ran, "ran"));
            scene.check("and the action is done, unrecovered", () -> scene.assertTrue(running.isDone() && !running.wasRecovered() && running.stepsRun() == 2, "state"));
            running.cancel();
            scene.check("a cancel after the end changes nothing", () -> scene.assertTrue(List.of("first", "second").equals(ran) && !running.wasRecovered(), "after a late cancel"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 60)
    public void aCancelMidWayRecoversOnceAndRunsNoFurtherStep(TestScene scene) {
        ArmorStand actor = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        List<String> ran = new ArrayList<>();
        Action.Running running = JolCraftGameTests.action().start(actor,
                List.of(new Action.Step(1, _ -> ran.add("first")), new Action.Step(10, _ -> ran.add("second"))), () -> ran.add("recovered"));
        scene.runAtTick(3, () -> {
            scene.check("the first step ran", () -> scene.assertEquals(List.of("first"), ran, "ran"));
            running.cancel();
            running.cancel();
            scene.check("cancelling recovered once", () -> scene.assertEquals(List.of("first", "recovered"), ran, "ran"));
        });
        scene.runAtTick(14, () -> {
            scene.check("the second step never ran", () -> scene.assertEquals(List.of("first", "recovered"), ran, "ran"));
            scene.check("the action is done and recovered", () -> scene.assertTrue(running.isDone() && running.wasRecovered() && running.stepsRun() == 1, "state"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 60)
    public void aStepThatThrowsEndsTheActionWithItsRecovery(TestScene scene) {
        ArmorStand actor = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        List<String> ran = new ArrayList<>();
        Action.Running running = JolCraftGameTests.action().start(actor, List.of(
                new Action.Step(1, _ -> ran.add("first")),
                new Action.Step(1, _ -> {
                    throw new IllegalStateException("a step that breaks");
                }),
                new Action.Step(1, _ -> ran.add("third"))), () -> ran.add("recovered"));
        scene.runAtTick(5, () -> {
            scene.check("the failing step ended the action with its recovery", () -> scene.assertEquals(List.of("first", "recovered"), ran, "ran"));
            scene.check("done, recovered, one step run", () -> scene.assertTrue(running.isDone() && running.wasRecovered() && running.stepsRun() == 1, "state"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 60)
    public void anActorThatDiesIsRecovered(TestScene scene) {
        Pig actor = scene.helper().spawn(EntityType.PIG, new BlockPos(0, 1, 0));
        List<String> ran = new ArrayList<>();
        Action.Running running = JolCraftGameTests.action().start(actor,
                List.of(new Action.Step(1, _ -> ran.add("first")), new Action.Step(3, _ -> ran.add("second"))), () -> ran.add("recovered"));
        scene.runAtTick(2, () -> {
            scene.check("the first step ran", () -> scene.assertEquals(List.of("first"), ran, "ran"));
            actor.setHealth(0.0F);
        });
        scene.runAtTick(6, () -> {
            scene.check("death replaced the second step with the recovery", () -> scene.assertEquals(List.of("first", "recovered"), ran, "ran"));
            scene.check("the action is done and recovered", () -> scene.assertTrue(running.isDone() && running.wasRecovered(), "state"));
            scene.succeed();
        });
    }

    @Scene(maxTicks = 60)
    public void anActorThatLeavesTheWorldIsRecovered(TestScene scene) {
        ArmorStand actor = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        List<String> ran = new ArrayList<>();
        Action.Running running = JolCraftGameTests.action().start(actor,
                List.of(new Action.Step(1, _ -> ran.add("first")), new Action.Step(3, _ -> ran.add("second"))), () -> ran.add("recovered"));
        scene.runAtTick(2, () -> {
            scene.check("the first step ran", () -> scene.assertEquals(List.of("first"), ran, "ran"));
            actor.discard();
        });
        scene.runAtTick(6, () -> {
            scene.check("the second step was replaced by the recovery", () -> scene.assertEquals(List.of("first", "recovered"), ran, "ran"));
            scene.check("the action is done and recovered", () -> scene.assertTrue(running.isDone() && running.wasRecovered(), "state"));
            scene.succeed();
        });
    }
}
