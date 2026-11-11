package net.sievert.jolcraft.module.system.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** The configured features the mushroom blocks name; the data contributor writes them. */
@Value
public final class FloraFeatures {

    /** The huge duskcap, which a duskcap grows into from bonemeal. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_DUSKCAP = Ids.key(Registries.CONFIGURED_FEATURE, Dictionary.JOLCRAFT, Dictionary.HUGE, Dictionary.DUSKCAP);
    /** The huge festerling, which a festerling grows into from bonemeal. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_FESTERLING = Ids.key(Registries.CONFIGURED_FEATURE, Dictionary.JOLCRAFT, Dictionary.HUGE, Dictionary.FESTERLING);

    private FloraFeatures() {
    }
}
