package net.sievert.jolcraft.module.system.flora;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The shapes of the two huge mushrooms: how many blocks each ring has, how high the whole is, how wide it is at each height and which cells belong to it. */
class MushroomShapeTest {

    private static final MushroomShape DUSKCAP = FloraContent.HUGE_DUSKCAP.get().shape();
    private static final MushroomShape FESTERLING = FloraContent.HUGE_FESTERLING.get().shape();

    /** The cells of the ring found by asking the shape, which must be as many as the ring says. */
    private static int cellsAt(MushroomShape shape, int height) {
        int found = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if ((dx != 0 || dz != 0) && shape.contains(dx, height, dz)) {
                    found++;
                }
            }
        }
        return found;
    }

    @Test
    void theDuskcapRingsHaveSixteenTwelveTwelveTwelveAndEightBlocks() {
        int[] expected = {16, 12, 12, 12, 8};
        Assertions.assertEquals(expected.length, DUSKCAP.rings().size());
        for (int ring = 0; ring < expected.length; ring++) {
            Assertions.assertEquals(expected[ring], DUSKCAP.rings().get(ring).count(), "ring " + ring);
            Assertions.assertEquals(expected[ring], cellsAt(DUSKCAP, DUSKCAP.rings().get(ring).height()), "cells of ring " + ring);
        }
    }

    @Test
    void theFesterlingRingsHaveTwelveAndEightBlocks() {
        int[] expected = {12, 8};
        Assertions.assertEquals(expected.length, FESTERLING.rings().size());
        for (int ring = 0; ring < expected.length; ring++) {
            Assertions.assertEquals(expected[ring], FESTERLING.rings().get(ring).count(), "ring " + ring);
            Assertions.assertEquals(expected[ring], cellsAt(FESTERLING, FESTERLING.rings().get(ring).height()), "cells of ring " + ring);
        }
    }

    @Test
    void theWholeHeightsAreEightAndFourWithATipOnTopOfTheStem() {
        Assertions.assertEquals(8, DUSKCAP.height());
        Assertions.assertEquals(7, DUSKCAP.stem());
        Assertions.assertTrue(DUSKCAP.contains(0, 6, 0), "the top of the stem");
        Assertions.assertTrue(DUSKCAP.contains(0, 7, 0), "the tip");
        Assertions.assertFalse(DUSKCAP.contains(0, 8, 0), "nothing above the tip");
        Assertions.assertEquals(4, FESTERLING.height());
        Assertions.assertTrue(FESTERLING.contains(0, 3, 0), "the tip");
        Assertions.assertFalse(FESTERLING.contains(0, 4, 0));
        Assertions.assertFalse(FESTERLING.contains(0, -1, 0), "nothing below the origin");
    }

    @Test
    void theRadiusAtEachHeightIsTheRingsAndZeroWhereThereIsNone() {
        int[] duskcap = {0, 0, 2, 2, 2, 2, 1, 0, 0};
        for (int height = 0; height < duskcap.length; height++) {
            Assertions.assertEquals(duskcap[height], DUSKCAP.radiusAt(height), "duskcap at " + height);
        }
        int[] festerling = {0, 2, 1, 0, 0};
        for (int height = 0; height < festerling.length; height++) {
            Assertions.assertEquals(festerling[height], FESTERLING.radiusAt(height), "festerling at " + height);
        }
    }

    @Test
    void theDuskcapIsHollowInsideItsSkirtAndOnlyItsLowestRingHasCorners() {
        Assertions.assertFalse(DUSKCAP.contains(1, 3, 0), "inside the skirt, beside the stem");
        Assertions.assertFalse(DUSKCAP.contains(1, 4, 1), "inside the skirt, diagonally");
        Assertions.assertTrue(DUSKCAP.contains(2, 3, 0), "the skirt");
        Assertions.assertTrue(DUSKCAP.contains(2, 2, 2), "a corner of the lowest ring");
        Assertions.assertFalse(DUSKCAP.contains(2, 3, 2), "no corner above it");
        Assertions.assertFalse(DUSKCAP.contains(2, 6, 0), "the top ring is narrower");
        Assertions.assertTrue(DUSKCAP.contains(1, 6, 1), "the top ring has its corners");
        Assertions.assertFalse(DUSKCAP.contains(1, 2, 0), "inside the lowest ring, beside the stem");
        Assertions.assertFalse(DUSKCAP.contains(3, 3, 0), "outside the skirt");
    }

    @Test
    void theFesterlingHasAGapBetweenItsStemAndItsBrim() {
        Assertions.assertTrue(FESTERLING.contains(0, 1, 0), "the stem");
        Assertions.assertFalse(FESTERLING.contains(1, 1, 0), "the gap");
        Assertions.assertFalse(FESTERLING.contains(1, 1, 1), "the gap, diagonally");
        Assertions.assertTrue(FESTERLING.contains(2, 1, 0), "the brim");
        Assertions.assertFalse(FESTERLING.contains(2, 1, 2), "the brim has no corners");
        Assertions.assertTrue(FESTERLING.contains(1, 2, 1), "the upper ring has its corners");
        Assertions.assertTrue(FESTERLING.contains(1, 2, 0));
        Assertions.assertFalse(FESTERLING.contains(2, 2, 0), "and is narrower than the brim");
    }
}
