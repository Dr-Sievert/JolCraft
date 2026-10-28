package net.sievert.jolcraft.module.composite.menu;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.composite.menu.StationLayout.Role;
import net.sievert.jolcraft.module.composite.menu.StationLayout.Run;

/** The layout's arithmetic and checks; nothing here needs a world. */
class StationLayoutTest {

    private static StationLayout bench() {
        return StationLayout.of(4).input(2, 3).input(4, 3).tool(6, 3, _ -> true, null).output(9, 3);
    }

    @Test
    void tilesCountFromOneInSixteenPixels() {
        Assertions.assertEquals(0, StationLayout.tile(1));
        Assertions.assertEquals(16, StationLayout.tile(2));
        Assertions.assertEquals(128, StationLayout.tile(9));
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.tile(0));
    }

    @Test
    void cellsKeepTheirOrderAndPlace() {
        StationLayout layout = bench();
        Assertions.assertEquals(4, layout.size());
        Assertions.assertEquals(List.of(Role.INPUT, Role.INPUT, Role.TOOL, Role.OUTPUT), layout.cells().stream().map(StationLayout.Cell::role).toList());
        Assertions.assertEquals(16, layout.cell(0).x());
        Assertions.assertEquals(32, layout.cell(0).y());
        Assertions.assertEquals(48, layout.cell(1).x());
        Assertions.assertEquals(128, layout.cell(3).x());
        Assertions.assertEquals(64, layout.panelHeight());
        layout.check();
    }

    @Test
    void aGridPlacesStorageRowByRow() {
        StationLayout layout = StationLayout.of(4).grid(2, 9, 8, 18);
        Assertions.assertEquals(18, layout.size());
        Assertions.assertEquals(8, layout.cell(0).x());
        Assertions.assertEquals(18, layout.cell(0).y());
        Assertions.assertEquals(26, layout.cell(1).x());
        Assertions.assertEquals(8, layout.cell(9).x());
        Assertions.assertEquals(36, layout.cell(9).y());
        Assertions.assertEquals(152, layout.cell(17).x());
        Assertions.assertTrue(layout.cells().stream().allMatch(cell -> cell.role() == Role.STORAGE));
        layout.check();
    }

    @Test
    void anOutputAcceptsNothingAndAnInputWhatItIsGiven() {
        StationLayout layout = StationLayout.of(3).input(2, 2).output(5, 2);
        Assertions.assertTrue(layout.cell(0).accepts(ItemStack.EMPTY));
        Assertions.assertFalse(layout.cell(1).accepts(ItemStack.EMPTY));
        Assertions.assertFalse(Role.OUTPUT.placeable());
        Assertions.assertTrue(Role.OUTPUT.extractable() && Role.STORAGE.extractable());
        Assertions.assertFalse(Role.INPUT.extractable() || Role.TOOL.extractable());
        Assertions.assertTrue(Role.OUTPUT.backwards() && Role.STORAGE.backwards());
        Assertions.assertFalse(Role.INPUT.backwards() || Role.TOOL.backwards());
        Assertions.assertTrue(Role.OUTPUT.result());
        Assertions.assertFalse(Role.STORAGE.result());
    }

    @Test
    void runsAreToolInputStorageAndNeverOutput() {
        StationLayout layout = StationLayout.of(5).input(2, 2).input(4, 2).tool(6, 2, _ -> true, null).storage(2, 4).storage(4, 4).output(9, 3);
        Assertions.assertEquals(List.of(new Run(Role.TOOL, 2, 3), new Run(Role.INPUT, 0, 2), new Run(Role.STORAGE, 3, 5)), layout.runs());
        Assertions.assertEquals(List.of(new Run(Role.INPUT, 0, 2)), StationLayout.of(3).input(2, 2).input(4, 2).output(8, 2).runs());
    }

    @Test
    void aRoleSplitInTwoRunsIsRefused() {
        StationLayout layout = StationLayout.of(4).input(2, 2).output(5, 2).input(8, 2);
        IllegalArgumentException refused = Assertions.assertThrows(IllegalArgumentException.class, layout::check);
        Assertions.assertTrue(refused.getMessage().contains("contiguous"), refused.getMessage());
    }

    @Test
    void aFrameOutsideThePanelIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(3).input(1, 2).check(), "frame left of the panel");
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(3).input(2, 1).check(), "frame above the panel");
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(3).input(11, 2).check(), "frame right of the panel");
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(3).input(2, 3).check(), "frame below the panel");
        StationLayout.of(3).input(10, 2).check();
    }

    @Test
    void twoOverlappingFramesAreRefused() {
        IllegalArgumentException refused = Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(4).input(2, 2).input(3, 2).check());
        Assertions.assertTrue(refused.getMessage().contains("overlap"), refused.getMessage());
        StationLayout.of(4).grid(1, 2, 8, 18).check();
    }

    @Test
    void aLowPanelAndAnEmptyOneAreRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(2).input(2, 1).check());
        Assertions.assertThrows(IllegalArgumentException.class, () -> StationLayout.of(3).check());
    }
}
