package net.sievert.jolcraft.module.primitive.attachment;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Handle;

/**
 * One piece of data a holder carries: its id, the fallback a holder answers until something is set, and what the
 * game does with it. {@link #saved(MapCodec)} writes it with the holder, {@link #synced(StreamCodec)} sends it to
 * every player who sees the holder, the holder itself included when it is a player, and {@link #keptOnDeath()}
 * carries it over a player's respawn or an entity's conversion. There is no owner-only sync: the game sends every
 * synced value to a player who starts seeing the holder, so what is a player's own business goes to that player
 * through a network payload instead. A key is a constant where it is used, declared through {@link Attachment}
 * while its module is built, and reads and writes a holder's data once the game has registered it. Values are
 * immutable: a change is a new value {@link #set} or {@link #update}d.
 */
@Handle
public sealed interface AttachmentKey<T> {

    /**
     * A key that is neither saved nor synced: the value is never written and never sent, and lives as long as the
     * holder instance does. The game re-creates an entity on a cross-dimension teleport, so the value is lost there.
     */
    static <T> AttachmentKey<T> of(Identifier id, Supplier<T> fallback) {
        return new Spec<>(id, fallback, null, null, false);
    }

    Identifier id();

    /** What a holder answers until something is set; called per holder, so a mutable fallback is not shared. */
    Supplier<T> fallback();

    /** Written with the holder through the codec and read back when the holder is. */
    AttachmentKey<T> saved(MapCodec<T> codec);

    /** Sent to every player who sees the holder, whenever it is set or reset, and once when a player starts seeing it. */
    AttachmentKey<T> synced(StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

    /** Carried over a player's respawn or a living entity's conversion; needs {@link #saved(MapCodec)}, the game copies through the codec. */
    AttachmentKey<T> keptOnDeath();

    boolean isSaved();

    boolean isSynced();

    boolean isKeptOnDeath();

    /** The registered type, for the game's own holder calls; there once the game registered what a module declared. */
    AttachmentType<T> type();

    /** The game's type for this key, built once by {@link Attachment} when the game registers. */
    AttachmentType<T> build();

    /** The holder's value, the fallback when nothing was set; the fallback is then stored and, when synced, sent, as the game does. */
    default T get(IAttachmentHolder holder) {
        return holder.getData(type());
    }

    /** Whether a value is stored on the holder, set or read once. */
    default boolean isSet(IAttachmentHolder holder) {
        return holder.hasData(type());
    }

    /** Stores the value, and sends it when the key is synced. */
    default void set(IAttachmentHolder holder, T value) {
        holder.setData(type(), value);
    }

    /** Stores the changed value and answers it; the fallback goes into the change unstored, so one value is sent, not two. */
    default T update(IAttachmentHolder holder, UnaryOperator<T> change) {
        T current = holder.getExistingDataOrNull(type());
        T changed = change.apply(current != null ? current : fallback().get());
        set(holder, changed);
        return changed;
    }

    /** Drops the stored value, so the holder answers the fallback again; sent as a removal when the key is synced. */
    default void reset(IAttachmentHolder holder) {
        holder.removeData(type());
    }

    /** The key with its shape; {@code holder} resolves the registered type lazily. */
    record Spec<T>(Identifier id, Supplier<T> fallback, @Nullable MapCodec<T> saved,
            @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> synced, boolean kept,
            DeferredHolder<AttachmentType<?>, AttachmentType<T>> holder) implements AttachmentKey<T> {

        Spec(Identifier id, Supplier<T> fallback, @Nullable MapCodec<T> saved,
                @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> synced, boolean kept) {
            this(id, fallback, saved, synced, kept, DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, id));
        }

        @Override
        public AttachmentKey<T> saved(MapCodec<T> codec) {
            return new Spec<>(id, fallback, codec, synced, kept);
        }

        @Override
        public AttachmentKey<T> synced(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            return new Spec<>(id, fallback, saved, codec, kept);
        }

        @Override
        public AttachmentKey<T> keptOnDeath() {
            return new Spec<>(id, fallback, saved, synced, true);
        }

        @Override
        public boolean isSaved() {
            return saved != null;
        }

        @Override
        public boolean isSynced() {
            return synced != null;
        }

        @Override
        public boolean isKeptOnDeath() {
            return kept;
        }

        @Override
        public AttachmentType<T> type() {
            if (!holder.isBound()) {
                throw new IllegalStateException(id + " is not registered: no module declared it through Attachment,"
                        + " or it is read before the game registered attachment types, which happens after the mod constructors ran");
            }
            return holder.get();
        }

        @Override
        public AttachmentType<T> build() {
            AttachmentType.Builder<T> builder = AttachmentType.builder(fallback);
            if (saved != null) {
                builder.serialize(saved);
            }
            if (synced != null) {
                builder.sync(synced);
            }
            if (kept) {
                builder.copyOnDeath();
            }
            return builder.build();
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
