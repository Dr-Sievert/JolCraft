package net.sievert.jolcraft.module.primitive.language;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

class LanguageKeyTest {

    @Test
    void wordsJoinWithDots() {
        Assertions.assertEquals("jolcraft.trade.offer", LanguageKey.of(Dictionary.JOLCRAFT, "trade", "offer").key());
    }

    @Test
    void aWordMayCarryUnderscoresFromModString() {
        Assertions.assertEquals("tooltip.jolcraft.bounty.not_complete",
                LanguageKey.of("tooltip", Dictionary.JOLCRAFT, "bounty", ModString.underscored("not", "complete")).key());
    }

    @Test
    void plusAppendsWords() {
        LanguageKey advancement = LanguageKey.of("advancements", Dictionary.JOLCRAFT, "first_trade");
        Assertions.assertEquals("advancements.jolcraft.first_trade.title", advancement.plus("title").key());
    }

    @Test
    void aWordThatIsNoDictionaryWordIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> LanguageKey.of("itemGroup"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> LanguageKey.of("with space"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> LanguageKey.of());
    }

    @Test
    void textIsATranslatableComponentWithTheArguments() {
        var component = LanguageKey.of(Dictionary.JOLCRAFT, "greeting").text("Tester", 3);
        TranslatableContents contents = Assertions.assertInstanceOf(TranslatableContents.class, component.getContents());
        Assertions.assertEquals("jolcraft.greeting", contents.getKey());
        Assertions.assertArrayEquals(new Object[] {"Tester", 3}, contents.getArgs());
    }
}
