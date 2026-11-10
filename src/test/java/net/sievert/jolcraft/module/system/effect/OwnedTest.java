package net.sievert.jolcraft.module.system.effect;

import java.util.Set;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The sources a held effect remembers: the codec the attachment saves through, and the immutable changes. */
class OwnedTest {

    private static final Identifier ONE = Identifier.fromNamespaceAndPath("jolcraft", "one");
    private static final Identifier TWO = Identifier.fromNamespaceAndPath("jolcraft", "two");

    @Test
    void anOwnedRoundTripsThroughItsCodec() {
        Owned owned = new Owned(Set.of(ONE, TWO));
        JsonElement written = Owned.CODEC.codec().encodeStart(JsonOps.INSTANCE, owned).getOrThrow();
        Assertions.assertEquals(owned, Owned.CODEC.codec().parse(JsonOps.INSTANCE, written).getOrThrow());
        Assertions.assertEquals(2, written.getAsJsonObject().getAsJsonArray("sources").size());
        Assertions.assertEquals(Owned.NONE, Owned.CODEC.codec().parse(JsonOps.INSTANCE, Owned.CODEC.codec().encodeStart(JsonOps.INSTANCE, Owned.NONE).getOrThrow()).getOrThrow());
    }

    @Test
    void aSourceIsAddedAndRemovedWithoutChangingTheOriginal() {
        Owned added = Owned.NONE.with(ONE);
        Assertions.assertTrue(added.has(ONE));
        Assertions.assertFalse(Owned.NONE.has(ONE), "the original is unchanged");
        Assertions.assertEquals(added, added.with(ONE), "a source is recorded once");
        Assertions.assertEquals(Set.of(TWO), added.with(TWO).without(ONE).sources());
        Assertions.assertEquals(Owned.NONE, added.without(ONE));
        Assertions.assertEquals(added, added.without(TWO), "forgetting an unknown source changes nothing");
    }

    @Test
    void anOwnedWithNoSourcesEqualsTheFallbackSoItIsNeverWritten() {
        Assertions.assertEquals(new Owned(Set.of()), Owned.NONE);
    }
}
