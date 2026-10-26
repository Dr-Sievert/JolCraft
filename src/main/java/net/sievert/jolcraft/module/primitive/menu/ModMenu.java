package net.sievert.jolcraft.module.primitive.menu;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Menu} over the game's menu type registry: the keys are kept until the game registers. */
@Implementation
public final class ModMenu implements Menu {

    private final Map<Identifier, MenuKey<?>> declared = new LinkedHashMap<>();
    private boolean registered;

    @Override
    public void declare(MenuKey<?> key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the menus were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; a menu id is the mod's own");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the menus were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.MENU, helper -> declared.values().forEach(key -> helper.register(key.id(), key.build())));
    }

    /** How many menus are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
