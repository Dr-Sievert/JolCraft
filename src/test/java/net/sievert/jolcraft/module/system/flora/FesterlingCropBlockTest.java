package net.sievert.jolcraft.module.system.flora;

import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The festerling crop's speed from its surroundings, and the shape of its block state: four ages, the first by default. */
class FesterlingCropBlockTest {

    @Test
    void theSpeedIsOneOnPlainGroundWithNothingAround() {
        Assertions.assertEquals(1.0F, FesterlingCropBlock.speed(false, 0, 0));
    }

    @Test
    void verdantGroundMultipliesByOneAndAHalf() {
        Assertions.assertEquals(1.5F, FesterlingCropBlock.speed(true, 0, 0));
    }

    @Test
    void eachVerdantBlockAroundAddsTwoAndEachLogBesideHalf() {
        Assertions.assertEquals(5.0F, FesterlingCropBlock.speed(false, 2, 0));
        Assertions.assertEquals(2.0F, FesterlingCropBlock.speed(false, 0, 2));
        Assertions.assertEquals(5.5F, FesterlingCropBlock.speed(false, 2, 1));
        Assertions.assertEquals(19.5F, FesterlingCropBlock.speed(true, 8, 4), "the most it can be: 1.5, eight times two, four times a half");
    }

    @Test
    void theBlockHasFourAgesAndStartsAtZero() {
        FesterlingCropBlock block = FloraContent.FESTERLING_CROP.get();
        Assertions.assertEquals(4, block.getStateDefinition().getPossibleStates().size());
        BlockState state = block.defaultBlockState();
        Assertions.assertEquals(0, state.getValue(FesterlingCropBlock.AGE));
        Assertions.assertEquals(3, FesterlingCropBlock.MAX_AGE);
    }
}
