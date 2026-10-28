package net.sievert.jolcraft.module.composite.menu;

import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.util.text.Dictionary;

/** The kind's defaults and withers; building a menu needs a player's inventory and is a scene in the game-test mod. */
class StationKindTest {

    private static final Identifier ID = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "bench");
    private static final Identifier PANEL = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "panel");
    private static final Identifier WELL = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "well");

    private final StationLayout layout = StationLayout.of(3).input(2, 2);
    private final StationKind<StationMenu> kind = StationKind.of(ID, layout, LanguageKey.of("container", Dictionary.JOLCRAFT, "bench"));

    @Test
    void aKindStartsWithNoDataNoSyncAndNoArt() {
        Assertions.assertEquals(ID, kind.id());
        Assertions.assertSame(layout, kind.layout());
        Assertions.assertEquals("container.jolcraft.bench", kind.title().key());
        Assertions.assertEquals(0, kind.dataCount());
        Assertions.assertFalse(kind.sendsItems());
        Assertions.assertTrue(kind.panel().isEmpty() && kind.slot().isEmpty());
        Assertions.assertEquals(ID, kind.menu().id());
        Assertions.assertEquals(ID.toString(), kind.toString());
    }

    @Test
    void everyWitherKeepsTheIdAndTheMenuKeysId() {
        StationKind<StationMenu> data = kind.data(2);
        Assertions.assertEquals(2, data.dataCount());
        Assertions.assertEquals(ID, data.id());
        Assertions.assertEquals(ID, data.menu().id());
        StationKind<StationMenu> synced = kind.syncedItems();
        Assertions.assertTrue(synced.sendsItems());
        Assertions.assertEquals(ID, synced.menu().id());
        StationKind<StationMenu> art = kind.art(PANEL, WELL);
        Assertions.assertEquals(PANEL, art.panel().orElseThrow());
        Assertions.assertEquals(WELL, art.slot().orElseThrow());
        Assertions.assertEquals(ID, art.menu().id());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kind.data(-1));
    }

    @Test
    void aMenuFactoryChangesNothingElse() {
        StationKind<StationMenu> full = kind.data(1).syncedItems().art(PANEL, WELL);
        StationKind<StationMenu> swapped = full.menu((_, _, _, _, _) -> null);
        Assertions.assertEquals(full.id(), swapped.id());
        Assertions.assertSame(full.layout(), swapped.layout());
        Assertions.assertEquals(full.title(), swapped.title());
        Assertions.assertEquals(1, swapped.dataCount());
        Assertions.assertTrue(swapped.sendsItems());
        Assertions.assertEquals(full.panel(), swapped.panel());
        Assertions.assertEquals(full.slot(), swapped.slot());
        Assertions.assertNotSame(full.factory(), swapped.factory());
    }

    @Test
    void createCallsTheFactoryWithTheKind() {
        AtomicReference<StationKind<StationMenu>> seen = new AtomicReference<>();
        StationKind<StationMenu> custom = kind.menu((given, _, _, _, _) -> {
            seen.set(given);
            return null;
        });
        Assertions.assertNull(custom.create(3, null, null, null));
        Assertions.assertSame(custom, seen.get());
    }
}
