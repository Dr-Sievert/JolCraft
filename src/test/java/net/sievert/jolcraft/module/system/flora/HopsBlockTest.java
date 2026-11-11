package net.sievert.jolcraft.module.system.flora;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The hops' arithmetic and the shape of their block state: ten ages and two halves, the lower half at age 0 by default. */
class HopsBlockTest {

    @Test
    void theFirstFiveAgesAreSingleAndTheRestDouble() {
        for (int age = 0; age <= 4; age++) {
            Assertions.assertFalse(HopsBlock.isDouble(age), "age " + age);
        }
        for (int age = 5; age <= 9; age++) {
            Assertions.assertTrue(HopsBlock.isDouble(age), "age " + age);
        }
    }

    @Test
    void theUpperHalfShowsTheAgeLessFiveAndNeverLessThanZero() {
        int[] expected = {0, 0, 0, 0, 0, 0, 1, 2, 3, 4};
        for (int age = 0; age <= 9; age++) {
            Assertions.assertEquals(expected[age], HopsBlock.upperStage(age), "age " + age);
        }
    }

    @Test
    void theBlockHasTwentyStatesOfTenAgesAndTwoHalves() {
        HopsBlock block = FloraContent.ASGARNIAN_HOPS_CROP.get();
        Assertions.assertEquals(20, block.getStateDefinition().getPossibleStates().size());
        Assertions.assertEquals(10, HopsBlock.AGE.getPossibleValues().size());
        Assertions.assertEquals(2, HopsBlock.HALF.getPossibleValues().size());
    }

    @Test
    void theDefaultStateIsTheLowerHalfAtAgeZero() {
        BlockState state = FloraContent.DUSKHOLD_HOPS_CROP.get().defaultBlockState();
        Assertions.assertEquals(DoubleBlockHalf.LOWER, state.getValue(HopsBlock.HALF));
        Assertions.assertEquals(0, state.getValue(HopsBlock.AGE));
    }
}
