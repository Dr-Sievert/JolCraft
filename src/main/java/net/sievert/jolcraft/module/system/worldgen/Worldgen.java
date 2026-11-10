package net.sievert.jolcraft.module.system.worldgen;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The world-generation plumbing no feature owns. In the finished tree it holds the structure processor and placement
 * filter kinds ({@link WorldgenContent}), the jigsaw structure base with its terrain veto, and the datagen helpers
 * for template pools and processor lists. The kinds need no behaviour of the system, since the game decodes and calls
 * them, so the class holds none until the jigsaw base comes with the first structure; design/worldgen.md.
 */
@ModSystem(Dictionary.WORLDGEN)
public final class Worldgen implements Module {

    public Worldgen() {
    }
}
