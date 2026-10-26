package net.sievert.jolcraft.module.composite.schedule;

import java.util.List;

import com.mojang.logging.LogUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.schedule.ModSchedule;
import net.sievert.jolcraft.util.text.Dictionary;

/** The guards; the running itself needs an actor on a server and is a scene in the game-test mod. */
class ActionTest {

    private final Action action = new Action(new ModSchedule((_, _) -> { }), new ModLog(LogUtils.getLogger()));

    @Test
    void aStepWaitsAtLeastOneTickAndDoesSomething() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Action.Step(0, _ -> { }));
        Assertions.assertThrows(NullPointerException.class, () -> new Action.Step(1, null));
        Assertions.assertEquals(2, new Action.Step(2, _ -> { }).ticks());
    }

    @Test
    void anActionHasAStepAndARecovery() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> action.start(null, List.of(), () -> { }));
        Assertions.assertEquals("a recovery", Assertions.assertThrows(NullPointerException.class, () -> action.start(null, List.of(new Action.Step(1, _ -> { })), null)).getMessage());
    }

    @Test
    void theIdIsItsWord() {
        Assertions.assertEquals(Dictionary.ACTION, action.id());
    }
}
