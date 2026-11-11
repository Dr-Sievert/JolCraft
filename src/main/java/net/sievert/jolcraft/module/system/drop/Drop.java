package net.sievert.jolcraft.module.system.drop;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.primitive.loot.Loot;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The loot modifier kinds no feature owns: so far {@link ReplaceTableModifier}, which clears what a table rolled and
 * rolls another table in its place, the clearing NeoForge's own {@code add_table} cannot do. The constructor declares
 * the kind to {@code Loot}, which the root registers once. The kind needs no behaviour of the system, since the game
 * decodes the modifier and calls it, so the class holds none; design/drop.md.
 */
@ModSystem(Dictionary.DROP)
public final class Drop implements Module {

    public Drop(Loot loot) {
        loot.declare(DropKeys.REPLACE_TABLE);
    }
}
