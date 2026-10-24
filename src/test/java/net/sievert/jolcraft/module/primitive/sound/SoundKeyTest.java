package net.sievert.jolcraft.module.primitive.sound;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class SoundKeyTest {

    private final Identifier id = Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "dwarf_yes");
    private final SoundKey voice = SoundKey.of(id);

    @Test
    void aKeyIsHeardByVolumeUntilGivenADistance() {
        Assertions.assertEquals(id, voice.id());
        Assertions.assertEquals(Optional.empty(), voice.build().fixedRange());
        Assertions.assertEquals(Optional.of(64.0F), voice.heardWithin(64).build().fixedRange());
        Assertions.assertEquals(Optional.empty(), voice.build().fixedRange(), "the key by volume is unchanged");
        Assertions.assertEquals(id, voice.heardWithin(64).build().location());
        Assertions.assertThrows(IllegalArgumentException.class, () -> voice.heardWithin(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> voice.heardWithin(Float.NaN));
    }

    @Test
    void aRangedKeyRaisesTheVolumeSentUntilAClientHearsTheRange() {
        Assertions.assertEquals(0.5F, voice.volumeSent(0.5F), "a key heard by volume sends what was asked");
        Assertions.assertEquals(4.0F, voice.heardWithin(64).volumeSent(1.0F), "sixty-four blocks over sixteen per unit");
        Assertions.assertEquals(8.0F, voice.heardWithin(64).volumeSent(8.0F), "a louder ask stays");
    }

    @Test
    void theSubtitleKeyIsTheGamesCategoryThenTheNamespaceThenThePath() {
        Assertions.assertEquals("subtitles.jolcraft.dwarf_yes", voice.subtitle());
        Assertions.assertEquals(voice.subtitle(), SoundKey.subtitle(id));
    }

    @Test
    void theEventIsNotThereBeforeTheGameRegistered() {
        IllegalStateException refused = Assertions.assertThrows(IllegalStateException.class, voice::event);
        Assertions.assertTrue(refused.getMessage().contains("declared"), refused.getMessage());
    }
}
