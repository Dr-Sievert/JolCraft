package net.sievert.jolcraft.module.system.flora;

import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/** The growing hints that are one line of an item's default lore; their English is the flora system's datagen contributor's. */
@Value
public final class FloraHints {

    /** What the four hops seeds say about where their plants grow. */
    public static final LanguageKey HOPS_SEEDS = LanguageKey.of(Dictionary.TOOLTIP, Dictionary.JOLCRAFT, ModString.underscored(Dictionary.HOPS, Dictionary.SEEDS));
    /** What the deepslate bulbs say about where they grow. */
    public static final LanguageKey DEEPSLATE_BULBS = LanguageKey.of(Dictionary.TOOLTIP, Dictionary.JOLCRAFT, ModString.underscored(Dictionary.DEEPSLATE, Dictionary.BULBS));

    private FloraHints() {
    }
}
