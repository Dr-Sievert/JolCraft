package net.sievert.jolcraft.module.primitive.language;

import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.ModString;
import org.jetbrains.annotations.NotNull;

/**
 * The name of a language-file entry, built from {@code Dictionary} words joined with dots and never written as a
 * literal: {@code LanguageKey.of(TOOLTIP, JOLCRAFT, BOUNTY, ModString.underscored(NOT, COMPLETE))} is
 * {@code tooltip.jolcraft.bounty.not_complete}. The game's own conventions are followed by putting the category
 * first and the namespace second (tooltip, subtitle, attribute, item group); the mod's own text starts with its
 * namespace. A key is held as a constant where it is used and given its English text in datagen.
 */
@Value
public record LanguageKey(String key) {

    private static final Pattern WORD = Pattern.compile("[a-z0-9_]+");

    public LanguageKey {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("a language key needs at least one word");
        }
    }

    /** The words joined with dots; each word is lowercase letters, digits and underscores, as Dictionary words are. */
    public static LanguageKey of(String... words) {
        if (words.length == 0) {
            throw new IllegalArgumentException("a language key needs at least one word");
        }
        for (String word : words) {
            if (!WORD.matcher(word).matches()) {
                throw new IllegalArgumentException("\"" + word + "\" is no language-key word: lowercase letters, digits and underscores only");
            }
        }
        return new LanguageKey(ModString.dotted(words));
    }

    /** This key with more words after it, as {@code title} or {@code description} under an advancement's key. */
    public LanguageKey plus(String... words) {
        return new LanguageKey(ModString.dotted(key, of(words).key));
    }

    /** A translatable component for this key, with its arguments in order. */
    public MutableComponent text(Object... arguments) {
        return Component.translatable(key, arguments);
    }

    @Override
    public @NotNull String toString() {
        return key;
    }
}
