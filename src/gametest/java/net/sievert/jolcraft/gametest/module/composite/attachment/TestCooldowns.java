package net.sievert.jolcraft.gametest.module.composite.attachment;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.composite.attachment.Cooldown;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The cooldown the cooldown scene starts; the game-test mod declares it in its root. */
public final class TestCooldowns {

    public static final Cooldown.Key STRIKE = Cooldown.Key.of(Ids.of(JolCraftGameTests.MOD_ID, "strike"));

    private TestCooldowns() {
    }

    public static void declare(Cooldown cooldown) {
        cooldown.declare(STRIKE);
    }
}
