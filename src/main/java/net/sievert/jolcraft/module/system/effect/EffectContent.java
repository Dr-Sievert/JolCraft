package net.sievert.jolcraft.module.system.effect;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import net.sievert.jolcraft.stereotype.Content;

/**
 * The effect system's data map types ({@link EffectData}). It holds no register: the root calls {@link #register}
 * once and the types are added to the game's registries at {@code RegisterDataMapTypesEvent}.
 */
@Content
public final class EffectContent {

    private EffectContent() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> {
            event.register(EffectData.STACKING);
            event.register(EffectData.EFFECT_PROTECTION);
            event.register(EffectData.ATTRIBUTE_PROTECTION);
            event.register(EffectData.SCALING);
        });
    }
}
