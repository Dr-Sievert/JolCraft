package net.sievert.jolcraft.module.system.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * Keeps a position whose absolute Y lies within both bounds, inclusive. Each bound is optional and an absent one
 * filters nothing on its side, as the game's {@code surface_relative_threshold_filter} defaults them, but the
 * bounds are world heights, not offsets from the surface.
 */
@Actor
public final class HeightFilter extends PlacementFilter {

    public static final MapCodec<HeightFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf(ModString.underscored(Dictionary.MIN, Dictionary.INCLUSIVE), Integer.MIN_VALUE).forGetter(filter -> filter.minInclusive),
            Codec.INT.optionalFieldOf(ModString.underscored(Dictionary.MAX, Dictionary.INCLUSIVE), Integer.MAX_VALUE).forGetter(filter -> filter.maxInclusive)
    ).apply(instance, HeightFilter::new));

    private final int minInclusive;
    private final int maxInclusive;

    public HeightFilter(int minInclusive, int maxInclusive) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        return minInclusive <= origin.getY() && origin.getY() <= maxInclusive;
    }

    @Override
    public PlacementModifierType<?> type() {
        return WorldgenContent.HEIGHT_FILTER.get();
    }
}
