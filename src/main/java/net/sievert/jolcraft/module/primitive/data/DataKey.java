package net.sievert.jolcraft.module.primitive.data;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One piece of saved data: its id, which names its file in the world, how a fresh one is made, and the codec the
 * game writes and reads it with. A key is kept per level unless it is {@link #serverWide()}, when every level hands
 * back the one instance the server keeps. A key is a constant where it is used and declared through {@link Data}
 * while its module is built; reading an undeclared key is an error. The data itself is a {@link SavedData} the
 * module owns and may change in place; {@link #update} makes a change and marks the data dirty, so it is written.
 */
@Handle
public sealed interface DataKey<T extends SavedData> {

    static <T extends SavedData> DataKey<T> of(Identifier id, Supplier<T> fresh, Codec<T> codec) {
        return new Spec<>(id, fresh, codec, false);
    }

    Identifier id();

    /** One instance for the whole server, in the server's own storage, instead of one per level. */
    DataKey<T> serverWide();

    boolean isServerWide();

    /** The game's type for this key; an error until the key is declared. */
    SavedDataType<@org.jetbrains.annotations.NotNull T> type();

    /** The data of this level, or the server's when the key is server-wide; made fresh the first time it is asked for. */
    default T get(ServerLevel level) {
        SavedDataType<@org.jetbrains.annotations.NotNull T> type = type();
        return (isServerWide() ? level.getServer().getDataStorage() : level.getDataStorage()).computeIfAbsent(type);
    }

    /** The server's data; only for a server-wide key, a per-level key needs its level. */
    default T get(MinecraftServer server) {
        SavedDataType<@org.jetbrains.annotations.NotNull T> type = type();
        if (!isServerWide()) {
            throw new IllegalStateException(id() + " is kept per level; read it with a level");
        }
        return server.getDataStorage().computeIfAbsent(type);
    }

    /** Changes the data and marks it dirty, so the game writes it with the world. */
    default void update(ServerLevel level, Consumer<T> change) {
        T data = get(level);
        change.accept(data);
        data.setDirty();
    }

    /** The key with its shape; {@code declaration} is flipped by {@link Data} and readable only inside the primitive. */
    record Spec<T extends SavedData>(Identifier id, Supplier<T> fresh, Codec<T> codec, boolean shared, SavedDataType<@org.jetbrains.annotations.NotNull T> type, Declaration declaration)
            implements DataKey<T> {

        Spec(Identifier id, Supplier<T> fresh, Codec<T> codec, boolean shared) {
            this(id, fresh, codec, shared, new SavedDataType<>(id, fresh, codec), new Declaration());
        }

        @Override
        public DataKey<T> serverWide() {
            return new Spec<>(id, fresh, codec, true);
        }

        @Override
        public boolean isServerWide() {
            return shared;
        }

        @Override
        public SavedDataType<@org.jetbrains.annotations.NotNull T> type() {
            if (!declaration.declared) {
                throw new IllegalStateException(id + " was not declared through Data; a module declares its keys while it is built");
            }
            return type;
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }

        /** Whether {@link Data} has seen the key; package-private on purpose, so only the primitive can say so. */
        static final class Declaration {
            boolean declared;
        }
    }
}
