package net.sievert.jolcraft.module.primitive.attachment;

import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Data the game's holders carry for the mod: a value on an entity, a block entity, a chunk or a level, kept with
 * the holder and, where asked, written with it, sent to clients and carried over a death. A module declares its
 * {@link AttachmentKey}s while it is built; the root calls {@link #register(IEventBus)} once; from then on the key
 * reads and writes the holder's data. Values are immutable: a change is a new value set through the key.
 */
@Primitive(Dictionary.ATTACHMENT)
public interface Attachment extends Module {

    /** Declares a key. Only while modules are built, once per id. */
    void declare(AttachmentKey<?> key);

    /** Registers every declared key's attachment type with the game. The root calls it once. */
    void register(IEventBus modBus);
}
