package net.sievert.jolcraft.module.primitive.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class ConfigKeyTest {

    enum Mood { CALM, GRUMPY }

    @Test
    void thePathIsTheWordsInOrderTheLastBeingTheName() {
        ConfigKey<Integer> offers = ConfigKey.whole(3, 1, 9, "How many offers a dwarf shows.", "dwarf", "trade", "offers");
        Assertions.assertEquals(List.of("dwarf", "trade", "offers"), offers.path());
        Assertions.assertEquals("offers", offers.name());
        Assertions.assertEquals(List.of("dwarf", "trade"), offers.sections());
        Assertions.assertEquals(List.of(), ConfigKey.flag(true, "c", "debug").sections());
    }

    @Test
    void aWordThatIsNoDictionaryWordOrOneTheScreenUsesIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConfigKey.flag(true, "c", "Trade"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConfigKey.flag(true, "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConfigKey.flag(true, "c", "title"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConfigKey.flag(true, "c", "trade", "button"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ConfigKey.flag(true, "c", "section", "trade"));
    }

    @Test
    void theTranslationKeyIsTheNamespaceThenConfigurationThenThePath() {
        ConfigKey<Integer> offers = ConfigKey.whole(3, 1, 9, "c", "dwarf", "trade", "offers");
        Assertions.assertEquals("jolcraft.configuration.dwarf.trade.offers", offers.translation(Dictionary.JOLCRAFT));
        Assertions.assertEquals("jolcraft.configuration.dwarf.trade", ConfigKey.translation(Dictionary.JOLCRAFT, "dwarf", "trade"));
    }

    @Test
    void everyKindDefinesItselfWithItsFallback() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        Assertions.assertEquals(true, ConfigKey.flag(true, "c", "a").defineValue(builder, "a").getDefault());
        Assertions.assertEquals(3, ConfigKey.whole(3, 1, 9, "c", "b").defineValue(builder, "b").getDefault());
        Assertions.assertEquals(0.5, ConfigKey.real(0.5, 0, 1, "c", "d").defineValue(builder, "d").getDefault());
        Assertions.assertEquals("x", ConfigKey.text("x", "c", "e").defineValue(builder, "e").getDefault());
        Assertions.assertEquals(Mood.GRUMPY, ConfigKey.choice(Mood.GRUMPY, "c", "f").defineValue(builder, "f").getDefault());
        Assertions.assertNotNull(builder.build());
    }

    @Test
    void aFlagTakesOnlyARealBoolean() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ModConfigSpec.ValueSpec spec = ConfigKey.flag(true, "c", "a").defineValue(builder, "a").getSpec();
        Assertions.assertTrue(spec.test(false));
        Assertions.assertFalse(spec.test("true"), "a quoted boolean in the file is corrected, never handed to the caller as text");
    }
}
