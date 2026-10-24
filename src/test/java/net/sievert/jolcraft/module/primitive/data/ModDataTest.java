package net.sievert.jolcraft.module.primitive.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping; scenes in the game-test mod read, change, save and reload real data on a level. */
class ModDataTest {

    static final class Tally extends SavedData {
        static final Codec<Tally> CODEC = Codec.INT.fieldOf("count").codec().xmap(count -> new Tally(), tally -> 0);
    }

    private final DataKey<Tally> tally = DataKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "tally"), Tally::new, Tally.CODEC);
    private final DataKey<Tally> ledger = DataKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "ledger"), Tally::new, Tally.CODEC).serverWide();

    @Test
    void keysAreCountedOncePerIdAndDeclaringMakesThemReadable() {
        ModData data = new ModData();
        Assertions.assertThrows(IllegalStateException.class, tally::type);
        data.declare(tally);
        data.declare(ledger);
        Assertions.assertEquals(2, data.declared());
        Assertions.assertNotNull(tally.type());
        Assertions.assertThrows(IllegalStateException.class, () -> data.declare(tally.serverWide()), "same id, another shape");
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ModData().declare(DataKey.of(Identifier.withDefaultNamespace("tally"), Tally::new, Tally.CODEC)));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.DATA, new ModData().id());
    }
}
