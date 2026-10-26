package net.sievert.jolcraft.gametest.module.primitive.stat;

import net.minecraft.stats.StatFormatter;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.primitive.stat.Stat;
import net.sievert.jolcraft.module.primitive.stat.StatKey;

/** The statistic the stat scene awards; the game-test mod declares it in its root. */
public final class TestStats {

    public static final StatKey PINGS = StatKey.of(Ids.of(JolCraftGameTests.MOD_ID, "pings"));
    public static final StatKey WALKED = StatKey.of(Ids.of(JolCraftGameTests.MOD_ID, "walked")).shownAs(StatFormatter.DISTANCE);

    private TestStats() {
    }

    public static void declare(Stat stat) {
        stat.declare(PINGS);
        stat.declare(WALKED);
    }
}
