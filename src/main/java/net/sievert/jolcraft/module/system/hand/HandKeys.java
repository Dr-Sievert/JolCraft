package net.sievert.jolcraft.module.system.hand;

import net.sievert.jolcraft.module.primitive.recipe.RecipeKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Keys;
import net.sievert.jolcraft.util.text.Dictionary;

/** The hand system's handles: its recipe kind, which {@link Hand} declares to {@code Recipe}. The yield kinds its record embeds are {@code OutcomeKeys.YIELDS}, never here, which would initialise in a cycle. */
@Keys
public final class HandKeys {

    /** The kind {@code jolcraft:hand}. */
    public static final RecipeKey<HandInput, HandRecipe> RECIPE = RecipeKey.of(Ids.of(Dictionary.JOLCRAFT, Dictionary.HAND), HandRecipe.CODEC, HandRecipe.STREAM_CODEC);

    private HandKeys() {
    }
}
