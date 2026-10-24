package net.sievert.jolcraft.module.primitive.language;

import net.minecraft.network.chat.MutableComponent;

import net.sievert.jolcraft.stereotype.Implementation;

/** {@link Language} over the game's translatable components. */
@Implementation
public final class ModLanguage implements Language {

    @Override
    public MutableComponent text(LanguageKey key, Object... arguments) {
        return key.text(arguments);
    }

    @Override
    public String plain(LanguageKey key, Object... arguments) {
        return key.text(arguments).getString();
    }
}
