package net.sievert.jolcraft.module.primitive.sound;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.stereotype.Handle;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * One sound of the mod: its id, which names its event and its definition in {@code sounds.json}, and how far it is
 * heard: by its volume, or {@link #heardWithin(float)} blocks whatever the volume. Every sound shows the subtitle
 * under {@link #subtitle()}, so no sound is lost on a player who cannot hear. A key is a constant where it is used,
 * declared through {@link Sound} while its module is built, and plays once the game has registered it. Playing is
 * the server's: every player in range hears it, the entity it plays at included; a client level plays nothing, as
 * the game's own call does, since the server's broadcast is what the client hears.
 */
@Handle
public sealed interface SoundKey {

    /** The game hears a sound this far per unit of volume; a key's range raises the volume sent until the client hears that far. */
    float BLOCKS_PER_VOLUME = 16.0F;

    static SoundKey of(Identifier id) {
        return new Spec(id, Optional.empty());
    }

    /** The translation key of the subtitle of the sound with this id: {@code subtitles.<namespace>.<path>}; the one formula the mod and datagen share. */
    static String subtitle(Identifier id) {
        return ModString.dotted(Dictionary.SUBTITLES, id.getNamespace(), id.getPath());
    }

    Identifier id();

    /**
     * Heard within this many blocks whatever the volume asked for; for horns and alarms. The server sends it that
     * far, and the volume sent is raised to what makes a client hear that far, since the game attenuates a sound
     * over {@value #BLOCKS_PER_VOLUME} blocks per unit of volume; loudness at the source never passes full anyway.
     */
    SoundKey heardWithin(float blocks);

    /** The fixed range in blocks, if the key has one. */
    Optional<Float> range();

    /** The translation key of this sound's subtitle. */
    default String subtitle() {
        return subtitle(id());
    }

    /** The registered event's holder, for the game's own calls; there once the game registered what a module declared. */
    Holder<SoundEvent> holder();

    default SoundEvent event() {
        return holder().value();
    }

    /** The game's event for this key, built once by {@link Sound} when the game registers. */
    SoundEvent build();

    /** The volume sent for the loudness asked: raised for a ranged key until a client hears the whole range. */
    default float volumeSent(float volume) {
        return range().map(blocks -> Math.max(volume, blocks / BLOCKS_PER_VOLUME)).orElse(volume);
    }

    /** Plays at a point: on a server every player in range hears it; a client level plays nothing. */
    default void play(Level level, Vec3 at, SoundSource source, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, event(), source, volumeSent(volume), pitch);
    }

    /** Plays at the centre of a block. */
    default void play(Level level, BlockPos at, SoundSource source, float volume, float pitch) {
        play(level, at.getCenter(), source, volume, pitch);
    }

    /** Plays where an entity is, with the entity's own voice, heard by everyone in range, the entity included when it is a player; a silenced entity stays silent. */
    default void play(Entity at, float volume, float pitch) {
        if (at.isSilent()) {
            return;
        }
        play(at.level(), at.position(), at.getSoundSource(), volume, pitch);
    }

    /** Plays at a point for one player alone; nobody else hears it. */
    default void playFor(ServerPlayer player, Vec3 at, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(holder(), source, at.x, at.y, at.z, volumeSent(volume), pitch, player.level().getRandom().nextLong()));
    }

    /** Plays where the player stands, for that player alone. */
    default void playFor(ServerPlayer player, SoundSource source, float volume, float pitch) {
        playFor(player, player.position(), source, volume, pitch);
    }

    /** The key with its shape; {@code deferred} resolves the registered event lazily. */
    record Spec(Identifier id, Optional<Float> range, DeferredHolder<SoundEvent, @org.jetbrains.annotations.NotNull SoundEvent> deferred) implements SoundKey {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        Spec(Identifier id, Optional<Float> range) {
            this(id, range, DeferredHolder.create(Registries.SOUND_EVENT, id));
        }

        @Override
        public SoundKey heardWithin(float blocks) {
            if (!(blocks > 0.0F) || Float.isInfinite(blocks)) {
                throw new IllegalArgumentException(id + " must be heard within a positive number of blocks, not " + blocks);
            }
            return new Spec(id, Optional.of(blocks));
        }

        @Override
        public Holder<SoundEvent> holder() {
            if (!deferred.isBound()) {
                throw new IllegalStateException(id + " is not registered: no module declared it through Sound,"
                        + " or it is used before the game registered sound events, which happens after the mod constructors ran");
            }
            return deferred;
        }

        @Override
        public SoundEvent build() {
            return range.map(blocks -> SoundEvent.createFixedRangeEvent(id, blocks)).orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
