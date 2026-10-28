package net.sievert.jolcraft.module.composite.menu;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.module.primitive.menu.ModMenu;
import net.sievert.jolcraft.util.text.Dictionary;

/** Declaring: the id, the layout's check and the menu primitive; opening a station is a scene in the game-test mod. */
class StationTest {

    private static StationKind<StationMenu> kind(String path, StationLayout layout) {
        return StationKind.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, path), layout, LanguageKey.of("container", Dictionary.JOLCRAFT, path));
    }

    private final ModMenu menu = new ModMenu();
    private final Station station = new Station(menu, new ModLog(LogUtils.getLogger()));

    @Test
    void declaringCountsAndGoesToTheMenuPrimitive() {
        station.declare(kind("bench", StationLayout.of(3).input(2, 2)));
        station.declare(kind("crate", StationLayout.of(4).grid(2, 9, 8, 18)));
        Assertions.assertEquals(2, station.declared());
        Assertions.assertEquals(2, menu.declared());
        Assertions.assertEquals(Dictionary.STATION, station.id());
    }

    @Test
    void aSecondKindOfTheSameIdIsRefusedNamingIt() {
        station.declare(kind("bench", StationLayout.of(3).input(2, 2)));
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, () -> station.declare(kind("bench", StationLayout.of(3).input(3, 2))));
        Assertions.assertTrue(refused.getMessage().contains("jolcraft:bench"), refused.getMessage());
        Assertions.assertEquals(1, station.declared());
    }

    @Test
    void aBlockEntityOverAKindNeverDeclaredIsRefused() {
        StationKind<StationMenu> undeclared = kind("lonely", StationLayout.of(3).input(2, 2));
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class,
                () -> new StationBlockEntity(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState(), undeclared) {
                });
        Assertions.assertTrue(refused.getMessage().contains("declare it through Station"), refused.getMessage());
    }

    @Test
    void aBadLayoutFailsTheDeclarationWithTheLayoutsMessageAndDeclaresNothing() {
        IllegalArgumentException refused = Assertions.assertThrows(IllegalArgumentException.class, () -> station.declare(kind("bench", StationLayout.of(2).input(2, 1))));
        Assertions.assertTrue(refused.getMessage().contains("at least"), refused.getMessage());
        Assertions.assertEquals(0, station.declared());
        Assertions.assertEquals(0, menu.declared());
    }
}
