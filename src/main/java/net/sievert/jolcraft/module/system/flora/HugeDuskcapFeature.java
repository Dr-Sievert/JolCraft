package net.sievert.jolcraft.module.system.flora;

import java.util.List;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

import net.sievert.jolcraft.stereotype.Actor;

/** The huge duskcap: a stem seven high under a skirt of three rings, a flat top ring, a narrower one and a tip, eight blocks high in all. */
@Actor
public class HugeDuskcapFeature extends LayeredMushroomFeature {

    private static final MushroomShape SHAPE = new MushroomShape(7, List.of(
            new MushroomShape.Ring(2, 2, true),
            new MushroomShape.Ring(3, 2, false),
            new MushroomShape.Ring(4, 2, false),
            new MushroomShape.Ring(5, 2, false),
            new MushroomShape.Ring(6, 1, true)));

    public HugeDuskcapFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public MushroomShape shape() {
        return SHAPE;
    }
}
