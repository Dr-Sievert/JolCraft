package net.sievert.jolcraft.module.primitive.data;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Data} over the game's saved data: declaring marks the key usable and keeps ids unique across modules. */
@Implementation
public final class ModData implements Data {

    private final Map<Identifier, DataKey<?>> declared = new LinkedHashMap<>();

    @Override
    public void declare(DataKey<?> key) {
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; saved data is named by the mod");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
        ((DataKey.Spec<?>) key).declaration().declared = true;
    }

    /** How many keys are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
