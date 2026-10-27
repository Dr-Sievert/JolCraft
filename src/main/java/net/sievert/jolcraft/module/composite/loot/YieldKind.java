package net.sievert.jolcraft.module.composite.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import net.sievert.jolcraft.stereotype.Handle;

/**
 * A kind of yield a module declares through {@link Result} while it is built: its id, which data names under
 * {@code type}, and the codec of its values. The composite's own four kinds need no key; a module's own does.
 */
@Handle
public sealed interface YieldKind {

    static YieldKind of(Identifier id, MapCodec<? extends Yield> codec) {
        return new Spec(id, codec);
    }

    Identifier id();

    MapCodec<? extends Yield> codec();

    record Spec(Identifier id, MapCodec<? extends Yield> codec) implements YieldKind {

        @Override
        public String toString() {
            return id.toString();
        }
    }
}
