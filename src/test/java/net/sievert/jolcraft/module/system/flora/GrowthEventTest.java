package net.sievert.jolcraft.module.system.flora;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The four questions of a growing plant start at the number they were posted with and change as their kind allows; none needs a level. */
class GrowthEventTest {

    @Test
    void lightStartsAtTheBrightnessItWasPostedWith() {
        Assertions.assertEquals(5, new GrowthEvent.Light(null, BlockPos.ZERO, 5).brightness());
    }

    @Test
    void lightIsRaisedAndNeverLowered() {
        GrowthEvent.Light light = new GrowthEvent.Light(null, BlockPos.ZERO, 5);
        light.atLeast(9);
        Assertions.assertEquals(9, light.brightness());
        light.atLeast(3);
        Assertions.assertEquals(9, light.brightness());
    }

    @Test
    void speedStartsAtTheSpeedItWasPostedWithAndScales() {
        GrowthEvent.Speed speed = new GrowthEvent.Speed(null, BlockPos.ZERO, 2.0F);
        Assertions.assertEquals(2.0F, speed.speed());
        speed.scale(1.5F);
        Assertions.assertEquals(3.0F, speed.speed());
    }

    @Test
    void spreadStartsAtOneInTwentyFiveAndCanBeSet() {
        GrowthEvent.Spread spread = new GrowthEvent.Spread(null, BlockPos.ZERO);
        Assertions.assertEquals(1.0F / 25.0F, spread.chance());
        Assertions.assertFalse(spread.changed());
        spread.setChance(0.06F);
        Assertions.assertEquals(0.06F, spread.chance());
        Assertions.assertTrue(spread.changed());
    }

    @Test
    void bonemealStartsAtFourInTenAndCanBeSet() {
        GrowthEvent.Bonemeal bonemeal = new GrowthEvent.Bonemeal(null, BlockPos.ZERO);
        Assertions.assertEquals(0.4F, bonemeal.chance());
        bonemeal.setChance(0.6F);
        Assertions.assertEquals(0.6F, bonemeal.chance());
    }

    @Test
    void everyKindCarriesThePositionItWasAskedAt() {
        BlockPos pos = new BlockPos(1, 2, 3);
        Assertions.assertEquals(pos, new GrowthEvent.Light(null, pos, 0).pos());
        Assertions.assertEquals(pos, new GrowthEvent.Speed(null, pos, 1.0F).pos());
        Assertions.assertEquals(pos, new GrowthEvent.Spread(null, pos).pos());
        Assertions.assertEquals(pos, new GrowthEvent.Bonemeal(null, pos).pos());
    }
}
