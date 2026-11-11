package net.sievert.jolcraft.module.system.flora;

import net.minecraft.world.level.material.MapColor;

import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** The four kinds of hops: the word that names their blocks and items and the colour their plants take on a map. */
@Value
public enum HopsVariety {

    ASGARNIAN(Dictionary.ASGARNIAN, MapColor.TERRACOTTA_MAGENTA),
    DUSKHOLD(Dictionary.DUSKHOLD, MapColor.TERRACOTTA_LIGHT_BLUE),
    KRANDONIAN(Dictionary.KRANDONIAN, MapColor.WARPED_STEM),
    YANILLIAN(Dictionary.YANILLIAN, MapColor.COLOR_GREEN);

    private final String word;
    private final MapColor color;

    HopsVariety(String word, MapColor color) {
        this.word = word;
        this.color = color;
    }

    /** The Dictionary word of this variety. */
    public String word() {
        return word;
    }

    public MapColor color() {
        return color;
    }
}
