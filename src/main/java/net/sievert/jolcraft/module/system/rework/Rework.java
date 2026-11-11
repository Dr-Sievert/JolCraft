package net.sievert.jolcraft.module.system.rework;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The crafting-table kind no feature owns: a shapeless recipe that reworks one base item into its result, carrying the
 * base stack's components over ({@link ReworkRecipe}, registered by {@link ReworkContent}). The kind needs no behaviour
 * of the system, since the game decodes the recipe and calls it from the crafting table and the crafter, so the class
 * holds none; design/rework.md.
 */
@ModSystem(Dictionary.REWORK)
public final class Rework implements Module {

    public Rework() {
    }
}
