package net.sievert.jolcraft.module.primitive.schedule;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The attachment to the bus; the ticking is {@link QueueTest}'s, and scenes in the game-test mod run tasks on the real server. */
class ModScheduleTest {

    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void attachingTwiceIsRefused() {
        ModSchedule schedule = new ModSchedule((_, _) -> { });
        schedule.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> schedule.register(bus));
        Assertions.assertEquals(0, schedule.pending());
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.SCHEDULE, new ModSchedule((_, _) -> { }).id());
    }
}
