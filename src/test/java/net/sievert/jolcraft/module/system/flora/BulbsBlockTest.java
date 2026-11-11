package net.sievert.jolcraft.module.system.flora;

import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The bulbs' depth rule and their ten ages. */
class BulbsBlockTest {

    @Test
    void aBulbIsDeepEnoughAtYZeroAndBelowAndNotAbove() {
        Assertions.assertFalse(BulbsBlock.deepEnough(1));
        Assertions.assertTrue(BulbsBlock.deepEnough(0));
        Assertions.assertTrue(BulbsBlock.deepEnough(-1));
    }

    @Test
    void theAgesRunFromZeroToNine() {
        Assertions.assertEquals(9, BulbsBlock.MAX_AGE);
        Assertions.assertEquals(10, BulbsBlock.AGE.getPossibleValues().size());
        Assertions.assertEquals(0, BulbsBlock.AGE.getPossibleValues().stream().min(Integer::compare).orElseThrow());
        Assertions.assertEquals(9, FloraContent.DEEPSLATE_BULBS_CROP.get().getMaxAge());
    }

    @Test
    void theDefaultStateIsAgeZero() {
        BlockState state = FloraContent.DEEPSLATE_BULBS_CROP.get().defaultBlockState();
        Assertions.assertEquals(0, state.getValue(BulbsBlock.AGE));
    }
}
