package net.sievert.jolcraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

/** Codec helpers shared across the mod. */
public final class JolCraftCodecs {

    private JolCraftCodecs() {}

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
