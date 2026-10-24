package net.sievert.jolcraft.datagen.contribute;

import net.sievert.jolcraft.datagen.provider.Translations;

/** A module's English text, keyed by the constants main declares. Every language is a contributor of its own kind later. */
public interface LanguageContributor {

    void english(Translations text);
}
