package net.sievert.jolcraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Comparator;

/** Codec helpers shared across the mod. */
public final class JolCraftCodecs {

    private JolCraftCodecs() {}

    /**
     * NeoForge's extra field on every encoded {@link MobEffectInstance}; see
     * {@code MobEffectInstance.Details.MAP_CODEC}.
     */
    private static final String NEOFORGE_CURES_KEY = "neoforge:cures";

    /**
     * {@link MobEffectInstance#CODEC} with a stable encoding.
     *
     * NeoForge keeps an effect's cures in an identity hash set and encodes them in
     * iteration order, so the plain codec writes {@code neoforge:cures} in an order
     * that differs from one JVM run to the next, and every generated file holding an
     * effect instance changes without a code change. This encoder sorts that list by
     * name; decoding is the vanilla codec untouched. A hidden effect's own list is
     * left alone, since nothing in the mod serializes one.
     */
    public static final Codec<MobEffectInstance> STABLE_MOB_EFFECT_INSTANCE = Codec.of(
            new Encoder<>() {
                @Override
                public <T> DataResult<T> encode(MobEffectInstance input, DynamicOps<T> ops, T prefix) {
                    return MobEffectInstance.CODEC.encode(input, ops, prefix)
                            .map(encoded -> withSortedCures(ops, encoded));
                }
            },
            MobEffectInstance.CODEC,
            "MobEffectInstance[sorted cures]"
    );

    private static <T> T withSortedCures(DynamicOps<T> ops, T encoded) {
        return ops.get(encoded, NEOFORGE_CURES_KEY)
                .flatMap(ops::getStream)
                .result()
                .map(cures -> ops.set(
                        encoded,
                        NEOFORGE_CURES_KEY,
                        ops.createList(cures.sorted(Comparator.comparing((T cure) ->
                                ops.getStringValue(cure).result().orElse("")
                        )))
                ))
                .orElse(encoded);
    }

    /**
     * Drops the partial result from decode errors.
     *
     * DataFixerUpper keeps a partial value for an error wherever it can: an
     * optional field that fails to parse yields an empty Optional, and a record
     * built from such fields yields the record. Callers that load with
     * resultOrPartial or promotePartial (entity NBT, advancements) then accept
     * that half-built value as if it were valid. Wrapped this way, a damaged
     * element is dropped by the enclosing list or map instead, and the error is
     * handed to the caller: entity NBT logs it through resultOrPartial, while
     * NeoForge's advancement loader promotes partials without logging, so a
     * dropped criterion is only reported when it makes the whole advancement
     * fail (single criterion, or an explicit requirements array).
     */
    public static <A> Codec<A> withoutPartials(Codec<A> codec) {
        return Codec.of(codec, new Decoder<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                DataResult<Pair<A, T>> result = codec.decode(ops, input);

                if (result.isError()) {
                    String message = result.error().orElseThrow().message();
                    return DataResult.error(() -> message);
                }

                return result;
            }
        }, codec + "[without partials]");
    }
}
