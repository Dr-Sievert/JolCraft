package net.sievert.jolcraft.module.primitive.attachment;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Attachment} over NeoForge's attachment types: the keys are kept until the game registers. */
@Implementation
public final class ModAttachment implements Attachment {

    private final Map<Identifier, AttachmentKey<?>> declared = new LinkedHashMap<>();
    private boolean registered;

    @Override
    public void declare(AttachmentKey<?> key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the attachments were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; an attachment id is the mod's own");
        }
        if (key.isKeptOnDeath() && !key.isSaved()) {
            throw new IllegalStateException(key + " is kept on death but not saved; the game carries an attachment over through its codec");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the attachments were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper -> declared.values().forEach(key -> helper.register(key.id(), key.build())));
    }

    /** How many keys are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
