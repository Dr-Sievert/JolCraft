package net.sievert.jolcraft.module.system.flora;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The two hints keep their keys: tooltip category, the mod's namespace, then the item they belong to. */
class FloraHintsTest {

    @Test
    void theHopsSeedsKeyIsATooltipOfTheMod() {
        Assertions.assertEquals("tooltip.jolcraft.hops_seeds", FloraHints.HOPS_SEEDS.key());
    }

    @Test
    void theBulbsKeyIsATooltipOfTheMod() {
        Assertions.assertEquals("tooltip.jolcraft.deepslate_bulbs", FloraHints.DEEPSLATE_BULBS.key());
    }
}
