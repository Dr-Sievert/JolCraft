package net.sievert.jolcraft.module.primitive.language;

import net.minecraft.network.chat.MutableComponent;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Player-facing text. Code holds a {@link LanguageKey} and never the words: the English text lives in datagen's
 * language contributor, other languages in their files, and the client shows the player's own. A module is handed
 * a {@code Language} and asks it for components; an actor, which takes no module, builds a component from the key
 * itself with {@link LanguageKey#text(Object...)}.
 */
@Primitive(Dictionary.LANGUAGE)
public interface Language extends Module {

    /** A translatable component for the key, with its arguments in order; the client fills in the player's language. */
    MutableComponent text(LanguageKey key, Object... arguments);

    /**
     * The key's text in the game's own language, for a log line or a console reply where no client translates;
     * the key itself when no text is known.
     */
    String plain(LanguageKey key, Object... arguments);
}
