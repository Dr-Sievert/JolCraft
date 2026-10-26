package net.sievert.jolcraft.module.composite.attachment;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.attachment.Attachment;
import net.sievert.jolcraft.module.primitive.attachment.AttachmentKey;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.util.text.Dictionary;
import org.jetbrains.annotations.NotNull;

/**
 * Cooldowns on entities that outlive a save: a dwarf that will not be endorsed again for a day, a player who may
 * not pay a second time this minute. A module declares a {@link Key} per cooldown while it is built, which this
 * composite keeps as an attachment holding the game time the cooldown ends at; {@link #ready} asks, {@link #start}
 * sets, and the game writes it with the entity and carries it over a player's death. Built from {@code attachment}
 * and {@code log}; read on the server, where the game time is the server's.
 */
@Composite(Dictionary.COOLDOWN)
public final class Cooldown implements Module {

    private final Attachment attachment;
    private final Log log;

    public Cooldown(Attachment attachment, Log log) {
        this.attachment = attachment;
        this.log = log.tagged(this);
    }

    /** Declares a cooldown; only while modules are built. */
    public void declare(Key key) {
        attachment.declare(key.attachment());
    }

    /** Whether the entity's cooldown is over, or was never started. */
    public boolean ready(Entity entity, Key key) {
        return remaining(entity, key) == 0;
    }

    /** The ticks until the entity's cooldown is over; zero when ready. */
    public int remaining(Entity entity, Key key) {
        if (!key.attachment().isSet(entity)) {
            return 0;
        }
        long until = key.attachment().get(entity);
        long now = entity.level().getGameTime();
        return until <= now ? 0 : (int) Math.min(Integer.MAX_VALUE, until - now);
    }

    /** Starts the entity's cooldown, over after the ticks; a running cooldown is replaced. */
    public void start(Entity entity, Key key, int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("a cooldown lasts at least one tick, not " + ticks);
        }
        key.attachment().set(entity, entity.level().getGameTime() + ticks);
        log.debug("{} on {} for {} ticks", key, entity.getName().getString(), ticks);
    }

    /** Ends the entity's cooldown now. */
    public void clear(Entity entity, Key key) {
        key.attachment().reset(entity);
    }

    /** One cooldown: its id names the attachment the end time is kept in. */
    public record Key(Identifier id, AttachmentKey<Long> attachment) {

        public static Key of(Identifier id) {
            Supplier<Long> never = () -> Long.MIN_VALUE;
            return new Key(id, AttachmentKey.of(id, never).saved(Codec.LONG.fieldOf(Dictionary.UNTIL)).keptOnDeath());
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
