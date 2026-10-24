package net.sievert.jolcraft.module.primitive.sound;

import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's sounds. A module declares its {@link SoundKey}s while it is built; the root calls
 * {@link #register(IEventBus)} once, which registers a sound event per key; from then on a key plays its sound in
 * the world, at an entity or for one player. Every sound shows a subtitle, and datagen demands the sound's
 * definition and the subtitle's English from the module's contributors.
 */
@Primitive(Dictionary.SOUND)
public interface Sound extends Module {

    /** Declares a key. Only while modules are built, once per id. */
    void declare(SoundKey key);

    /** Registers every declared key's sound event with the game. The root calls it once. */
    void register(IEventBus modBus);
}
