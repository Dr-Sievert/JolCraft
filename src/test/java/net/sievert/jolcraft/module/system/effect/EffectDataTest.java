package net.sievert.jolcraft.module.system.effect;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.system.effect.EffectData.Direction;
import net.sievert.jolcraft.module.system.effect.EffectData.Protection;
import net.sievert.jolcraft.module.system.effect.EffectData.Scaling;
import net.sievert.jolcraft.module.system.effect.EffectData.Stacking;

/** The records of the effect data: their codecs, and the arithmetic the rules call, none of which needs a level. */
class EffectDataTest {

    private static final int INFINITE = MobEffectInstance.INFINITE_DURATION;
    // A holder set resolves only under registry-aware ops.
    private static final RegistryOps<JsonElement> OPS = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private static <T> T roundTrip(Codec<T> codec, T original) {
        return codec.parse(OPS, codec.encodeStart(OPS, original).getOrThrow()).getOrThrow();
    }

    // ------------------------------------------------------------------ codecs

    @Test
    void aStackingRoundTripsWithAndWithoutACap() {
        Assertions.assertEquals(new Stacking(Optional.of(2)), roundTrip(Stacking.CODEC, new Stacking(Optional.of(2))));
        Assertions.assertEquals(new Stacking(Optional.empty()), roundTrip(Stacking.CODEC, new Stacking(Optional.empty())));
        Assertions.assertEquals(new Stacking(Optional.empty()), Stacking.CODEC.parse(JsonOps.INSTANCE, json("{}")).getOrThrow(), "absent is no cap");
        Assertions.assertEquals("2", Stacking.CODEC.encodeStart(JsonOps.INSTANCE, new Stacking(Optional.of(2))).getOrThrow().getAsJsonObject().get("max_amplifier").getAsString());
    }

    @Test
    void aStackingCapOutsideTheAmplifierRangeIsRefused() {
        Assertions.assertTrue(Stacking.CODEC.parse(JsonOps.INSTANCE, json("{\"max_amplifier\":-1}")).isError());
        Assertions.assertTrue(Stacking.CODEC.parse(JsonOps.INSTANCE, json("{\"max_amplifier\":256}")).isError());
        Assertions.assertTrue(Stacking.CODEC.parse(JsonOps.INSTANCE, json("{\"max_amplifier\":255}")).isSuccess());
    }

    @Test
    void anEffectProtectionRoundTripsAndCleansesOnlyWhenAsked() {
        Protection protection = new Protection(HolderSet.direct(MobEffects.POISON, MobEffects.WITHER), true);
        Protection back = roundTrip(Protection.EFFECT_CODEC, protection);
        Assertions.assertTrue(back.cleanse());
        Assertions.assertTrue(back.blocks().contains(MobEffects.POISON) && back.blocks().contains(MobEffects.WITHER) && !back.blocks().contains(MobEffects.LUCK));
        Protection plain = Protection.EFFECT_CODEC.parse(OPS, json("{\"blocks\":\"minecraft:poison\"}")).getOrThrow();
        Assertions.assertFalse(plain.cleanse(), "cleanse is off by default");
        Assertions.assertTrue(plain.blocks().contains(MobEffects.POISON));
        Assertions.assertTrue(Protection.EFFECT_CODEC.parse(OPS, json("{\"cleanse\":true}")).isError(), "blocks is required");
        Assertions.assertTrue(Protection.EFFECT_CODEC.parse(OPS, json("{\"blocks\":\"minecraft:no_such_effect\"}")).isError());
    }

    @Test
    void anAttributeProtectionNeverCleanses() {
        Protection parsed = Protection.ATTRIBUTE_CODEC.parse(OPS, json("{\"blocks\":[\"minecraft:poison\"],\"cleanse\":true}")).getOrThrow();
        Assertions.assertFalse(parsed.cleanse());
        Assertions.assertFalse(Protection.ATTRIBUTE_CODEC.encodeStart(OPS, new Protection(HolderSet.direct(MobEffects.POISON), true)).getOrThrow().getAsJsonObject().has("cleanse"));
    }

    @Test
    void aScalingRoundTripsByCategoryByListAndByBoth() {
        for (Scaling scaling : new Scaling[] {
                new Scaling(Optional.of(MobEffectCategory.HARMFUL), Optional.empty(), Direction.SHORTEN),
                new Scaling(Optional.empty(), Optional.of(HolderSet.direct(MobEffects.LUCK)), Direction.LENGTHEN),
                new Scaling(Optional.of(MobEffectCategory.BENEFICIAL), Optional.of(HolderSet.direct(MobEffects.LUCK)), Direction.LENGTHEN)}) {
            Scaling back = roundTrip(Scaling.CODEC, scaling);
            Assertions.assertEquals(scaling.category(), back.category());
            Assertions.assertEquals(scaling.direction(), back.direction());
            Assertions.assertEquals(scaling.effects().isPresent(), back.effects().isPresent());
        }
        JsonElement written = Scaling.CODEC.encodeStart(OPS, new Scaling(Optional.of(MobEffectCategory.HARMFUL), Optional.empty(), Direction.SHORTEN)).getOrThrow();
        Assertions.assertEquals("harmful", written.getAsJsonObject().get("category").getAsString());
        Assertions.assertEquals("shorten", written.getAsJsonObject().get("direction").getAsString());
    }

    @Test
    void aScalingWithNoFilterIsRefused() {
        Assertions.assertTrue(Scaling.CODEC.parse(OPS, json("{\"direction\":\"shorten\"}")).isError());
        Assertions.assertTrue(Scaling.CODEC.parse(OPS, json("{\"category\":\"harmful\"}")).isError(), "the direction is required too");
        Assertions.assertTrue(Scaling.CODEC.parse(OPS, json("{\"category\":\"harmful\",\"direction\":\"sideways\"}")).isError());
    }

    @Test
    void theCategoryCodecRefusesAnUnknownName() {
        for (MobEffectCategory category : MobEffectCategory.values()) {
            Assertions.assertEquals(category, roundTrip(EffectData.CATEGORY, category));
        }
        Assertions.assertTrue(EffectData.CATEGORY.parse(JsonOps.INSTANCE, json("\"cursed\"")).isError());
        Assertions.assertTrue(EffectData.CATEGORY.parse(JsonOps.INSTANCE, json("\"HARMFUL\"")).isError(), "the names are lowercase");
    }

    // ------------------------------------------------------------------ stacking

    private static Stacking.Merged merge(Stacking stacking, int oldAmplifier, int oldDuration, int addedAmplifier, int addedDuration) {
        return stacking.merge(oldAmplifier, oldDuration, addedAmplifier, addedDuration);
    }

    @Test
    void stackedAmplifiersAddPlusOneUpToTheCap() {
        Stacking capped = new Stacking(Optional.of(2));
        Assertions.assertEquals(1, merge(capped, 0, 100, 0, 100).amplifier());
        Assertions.assertEquals(2, merge(capped, 1, 100, 0, 100).amplifier());
        Assertions.assertEquals(2, merge(capped, 2, 100, 0, 100).amplifier(), "stays at the cap");
        Assertions.assertEquals(2, merge(capped, 1, 100, 1, 100).amplifier(), "clamped, not 3");
    }

    @Test
    void anAmplifierAlreadyAboveALoweredCapIsNeverLowered() {
        Stacking capped = new Stacking(Optional.of(1));
        Assertions.assertEquals(4, merge(capped, 4, 100, 0, 100).amplifier());
    }

    @Test
    void noCapStillStopsAt255() {
        Stacking uncapped = new Stacking(Optional.empty());
        Assertions.assertEquals(6, merge(uncapped, 3, 100, 2, 100).amplifier());
        Assertions.assertEquals(255, merge(uncapped, 254, 100, 5, 100).amplifier());
        Assertions.assertEquals(255, merge(uncapped, 255, 100, 255, 100).amplifier());
    }

    @Test
    void stackedDurationsTakeTheLonger() {
        Stacking stacking = new Stacking(Optional.empty());
        Assertions.assertEquals(300, merge(stacking, 0, 300, 0, 100).duration());
        Assertions.assertEquals(300, merge(stacking, 0, 100, 0, 300).duration());
    }

    @Test
    void anInfiniteIncomingStaysInfinite() {
        Stacking stacking = new Stacking(Optional.empty());
        Assertions.assertEquals(INFINITE, merge(stacking, 0, 100, 0, INFINITE).duration());
        Assertions.assertEquals(INFINITE, merge(stacking, 0, INFINITE, 0, INFINITE).duration());
    }

    @Test
    void aFiniteIncomingOnAnInfiniteOldStaysFinite() {
        Stacking stacking = new Stacking(Optional.of(2));
        Assertions.assertEquals(100, merge(stacking, 0, INFINITE, 0, 100).duration(), "so the game's update pushes the held instance down the chain");
    }

    // ------------------------------------------------------------------ scaling

    @Test
    void lengtheningAddsTheValueAndShorteningSubtractsItClamped() {
        Scaling lengthen = new Scaling(Optional.of(MobEffectCategory.BENEFICIAL), Optional.empty(), Direction.LENGTHEN);
        Scaling shorten = new Scaling(Optional.of(MobEffectCategory.HARMFUL), Optional.empty(), Direction.SHORTEN);
        Assertions.assertEquals(1.5D, lengthen.factor(0.5D));
        Assertions.assertEquals(3.0D, lengthen.factor(2.0D), "a lengthening is not capped");
        Assertions.assertEquals(1.0D, lengthen.factor(-3.0D), "and never shortens");
        Assertions.assertEquals(0.5D, shorten.factor(0.5D));
        Assertions.assertEquals(0.0D, shorten.factor(1.0D));
        Assertions.assertEquals(0.0D, shorten.factor(7.0D), "a shortening stops at nothing");
        Assertions.assertEquals(1.0D, shorten.factor(-1.0D), "and never lengthens");
    }

    @Test
    void aShorteningOfOneOrMoreBlocksAndNothingElseDoes() {
        Scaling lengthen = new Scaling(Optional.of(MobEffectCategory.BENEFICIAL), Optional.empty(), Direction.LENGTHEN);
        Scaling shorten = new Scaling(Optional.of(MobEffectCategory.HARMFUL), Optional.empty(), Direction.SHORTEN);
        Assertions.assertFalse(shorten.immune(0.99D));
        Assertions.assertTrue(shorten.immune(1.0D));
        Assertions.assertTrue(shorten.immune(2.0D));
        Assertions.assertFalse(lengthen.immune(5.0D));
    }

    @Test
    void aScaledDurationIsRoundedAndAtLeastOneTick() {
        Assertions.assertEquals(150, Scaling.scale(100, 1.5D));
        Assertions.assertEquals(50, Scaling.scale(100, 0.5D));
        Assertions.assertEquals(1, Scaling.scale(100, 0.0D));
        Assertions.assertEquals(1, Scaling.scale(1, 0.4D));
        Assertions.assertEquals(2, Scaling.scale(3, 0.5D), "rounded, not floored");
    }

    @Test
    void anEntryMatchesWhatEveryFilterItHasHolds() {
        Scaling category = new Scaling(Optional.of(MobEffectCategory.BENEFICIAL), Optional.empty(), Direction.LENGTHEN);
        Scaling list = new Scaling(Optional.empty(), Optional.of(HolderSet.direct(MobEffects.LUCK, MobEffects.UNLUCK)), Direction.LENGTHEN);
        Scaling both = new Scaling(Optional.of(MobEffectCategory.BENEFICIAL), Optional.of(HolderSet.direct(MobEffects.LUCK, MobEffects.UNLUCK)), Direction.LENGTHEN);
        Assertions.assertTrue(category.matches(MobEffects.LUCK));
        Assertions.assertFalse(category.matches(MobEffects.UNLUCK));
        Assertions.assertTrue(list.matches(MobEffects.UNLUCK));
        Assertions.assertFalse(list.matches(MobEffects.POISON));
        Assertions.assertTrue(both.matches(MobEffects.LUCK));
        Assertions.assertFalse(both.matches(MobEffects.UNLUCK), "in the list but not beneficial");
        Assertions.assertFalse(both.matches(MobEffects.SPEED), "beneficial but not in the list");
    }

    // ------------------------------------------------------------------ protection

    @Test
    void aStrengthOfZeroNeverRollsAndOneAlwaysDoesWithoutADraw() {
        RandomSource untouched = RandomSource.create(7L);
        Assertions.assertFalse(Protection.roll(0.0D, untouched));
        Assertions.assertFalse(Protection.roll(-0.5D, untouched));
        Assertions.assertTrue(Protection.roll(1.0D, untouched));
        Assertions.assertTrue(Protection.roll(3.0D, untouched));
        Assertions.assertEquals(RandomSource.create(7L).nextInt(), untouched.nextInt(), "no draw was spent");
    }

    @Test
    void aStrengthBetweenIsOneDrawCompared() {
        RandomSource rolled = RandomSource.create(42L);
        RandomSource mirror = RandomSource.create(42L);
        for (int i = 0; i < 200; i++) {
            Assertions.assertEquals(mirror.nextDouble() < 0.3D, Protection.roll(0.3D, rolled), "draw " + i);
        }
    }

    @Test
    void aSeededChanceHitsAboutItsStrength() {
        RandomSource random = RandomSource.create(1234L);
        int hits = 0;
        for (int i = 0; i < 10_000; i++) {
            hits += Protection.roll(0.25D, random) ? 1 : 0;
        }
        Assertions.assertTrue(Math.abs(hits - 2_500) < 300, "hit " + hits + " of 10000 at 0.25");
    }
}
