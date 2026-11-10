package net.sievert.jolcraft.module.system.control;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** What each stop is called. How an item picks its stop needs bound item components and tags, which only a running game has: the scenes cover it. */
class StopTest {

    @Test
    void eachStopHoldsAMobEffectTagOfItsOwnUnderTheModsNamespace() {
        Set<String> tags = new HashSet<>();
        for (Stop stop : Stop.values()) {
            Assertions.assertEquals("jolcraft", stop.tag().location().getNamespace());
            Assertions.assertEquals(stop.name().toLowerCase(Locale.ROOT), stop.tag().location().getPath());
            tags.add(stop.tag().location().toString());
        }
        Assertions.assertEquals(Stop.values().length, tags.size(), "no two stops share a tag");
    }
}
