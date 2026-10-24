package net.sievert.jolcraft.module.primitive.data;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's saved data: state a world keeps between sessions that belongs to no entity, kept per level or once for
 * the whole server, written by the game with the world and read back with it. A module declares its {@link DataKey}s
 * while it is built; from then on a key reads and changes its data on any level. No registration follows: the game
 * creates saved data the first time it is asked for, so declaring is what makes a key usable and keeps ids unique.
 */
@Primitive(Dictionary.DATA)
public interface Data extends Module {

    /** Declares a key. Only while modules are built, once per id. */
    void declare(DataKey<?> key);
}
