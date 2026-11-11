package net.sievert.jolcraft.module.system.flora;

import java.util.List;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

import net.sievert.jolcraft.stereotype.Actor;

/** The huge festerling: a stem three high under a brim of one ring without corners, a smaller ring and a tip, four blocks high in all. */
@Actor
public class HugeFesterlingFeature extends LayeredMushroomFeature {

    private static final MushroomShape SHAPE = new MushroomShape(3, List.of(
            new MushroomShape.Ring(1, 2, false),
            new MushroomShape.Ring(2, 1, true)));

    public HugeFesterlingFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public MushroomShape shape() {
        return SHAPE;
    }
}
