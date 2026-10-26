package net.sievert.jolcraft.module.primitive.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class AttachmentKeyTest {

    private final Identifier id = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "mood");
    private final AttachmentKey<Integer> plain = AttachmentKey.of(id, () -> 0);

    @Test
    void aKeyStartsPlainAndEachAskIsANewKey() {
        Assertions.assertEquals(id, plain.id());
        Assertions.assertEquals(0, plain.fallback().get());
        Assertions.assertFalse(plain.isSaved());
        Assertions.assertFalse(plain.isSynced());
        Assertions.assertFalse(plain.isKeptOnDeath());

        AttachmentKey<Integer> kept = plain.saved(Codec.INT.fieldOf("value")).synced(ByteBufCodecs.VAR_INT).keptOnDeath();
        AttachmentKey<Integer> reordered = plain.keptOnDeath().synced(ByteBufCodecs.VAR_INT).saved(Codec.INT.fieldOf("value"));
        Assertions.assertTrue(reordered.isSaved() && reordered.isSynced() && reordered.isKeptOnDeath(), "every ask is kept whatever the order");
        Assertions.assertTrue(kept.isSaved());
        Assertions.assertTrue(kept.isSynced());
        Assertions.assertTrue(kept.isKeptOnDeath());
        Assertions.assertFalse(plain.isSaved(), "the plain key is unchanged");
        Assertions.assertEquals(id, kept.id());
    }

    @Test
    void theTypeIsNotThereBeforeTheGameRegistered() {
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, plain::type);
        Assertions.assertTrue(refused.getMessage().contains("declared"), "the message names the likelier cause: " + refused.getMessage());
    }

    @Test
    void buildingDoesNotThrowForAnyShapeAndRefusesKeptWithoutSaved() {
        Assertions.assertNotNull(plain.build());
        Assertions.assertNotNull(plain.saved(Codec.INT.fieldOf("value")).build());
        Assertions.assertNotNull(plain.synced(ByteBufCodecs.VAR_INT).build());
        Assertions.assertNotNull(plain.saved(Codec.INT.fieldOf("value")).keptOnDeath().build());
        Assertions.assertThrows(IllegalStateException.class, () -> plain.keptOnDeath().build(), "the game refuses copyOnDeath without a serializer, which proves kept reaches it");
    }
}
