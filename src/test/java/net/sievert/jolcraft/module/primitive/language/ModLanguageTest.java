package net.sievert.jolcraft.module.primitive.language;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class ModLanguageTest {

    private final Language language = new ModLanguage();
    private final LanguageKey unknown = LanguageKey.of(Dictionary.JOLCRAFT, "test", "unknown");

    @Test
    void textCarriesTheKeyAndArguments() {
        TranslatableContents contents = Assertions.assertInstanceOf(TranslatableContents.class, language.text(unknown, 7).getContents());
        Assertions.assertEquals(unknown.key(), contents.getKey());
        Assertions.assertArrayEquals(new Object[] {7}, contents.getArgs());
    }

    @Test
    void plainFallsBackToTheKeyWhenNoTextIsKnown() {
        Assertions.assertEquals(unknown.key(), language.plain(unknown));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.LANGUAGE, language.id());
    }
}
