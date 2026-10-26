package net.sievert.jolcraft.gametest.module.composite.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;

/** The cooldown composite on a real entity: ready at first, not while running, counting down, ready again after, and cleared on demand. */
public final class CooldownScenes {

    @Scene(maxTicks = 40)
    public void aCooldownRunsItsTicksAndIsReadyAgain(TestScene scene) {
        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, new BlockPos(0, 1, 0));
        scene.check("ready before anything", () -> scene.assertTrue(JolCraftGameTests.cooldown().ready(stand, TestCooldowns.STRIKE), "ready at first"));
        JolCraftGameTests.cooldown().start(stand, TestCooldowns.STRIKE, 5);
        scene.check("not ready while running", () -> scene.assertTrue(
                !JolCraftGameTests.cooldown().ready(stand, TestCooldowns.STRIKE) && JolCraftGameTests.cooldown().remaining(stand, TestCooldowns.STRIKE) == 5, "running"));
        scene.runAtTick(3, () -> scene.check("counting down", () -> scene.assertEquals(2, JolCraftGameTests.cooldown().remaining(stand, TestCooldowns.STRIKE), "remaining")));
        scene.runAtTick(5, () -> {
            scene.check("ready again after its ticks", () -> scene.assertTrue(JolCraftGameTests.cooldown().ready(stand, TestCooldowns.STRIKE), "ready after"));
            JolCraftGameTests.cooldown().start(stand, TestCooldowns.STRIKE, 100);
            JolCraftGameTests.cooldown().start(stand, TestCooldowns.STRIKE, 2);
            scene.check("a running cooldown is replaced, not added to", () -> scene.assertEquals(2, JolCraftGameTests.cooldown().remaining(stand, TestCooldowns.STRIKE), "replaced"));
            scene.check("it is kept as the game time it ends at, in a saved attachment", () -> scene.assertTrue(
                    TestCooldowns.STRIKE.attachment().get(stand) == scene.level().getGameTime() + 2 && TestCooldowns.STRIKE.attachment().isSaved(), "end time"));
            JolCraftGameTests.cooldown().start(stand, TestCooldowns.STRIKE, 100);
            JolCraftGameTests.cooldown().clear(stand, TestCooldowns.STRIKE);
            scene.check("cleared on demand", () -> scene.assertTrue(JolCraftGameTests.cooldown().ready(stand, TestCooldowns.STRIKE), "cleared"));
            scene.succeed();
        });
    }
}
