package net.sievert.jolcraft.module.primitive.advancement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Advancement} over the game's trigger registry and the players' advancement progress. */
@Implementation
public final class ModAdvancement implements Advancement {

    private final Map<Identifier, TriggerKey<?>> declared = new LinkedHashMap<>();
    private boolean registered;

    @Override
    public void declare(TriggerKey<?> key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the criterion kinds were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; a criterion kind's id is the mod's own");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the criterion kinds were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.TRIGGER_TYPE, helper -> declared.values().forEach(key -> helper.register(key.id(), key.build())));
    }

    @Override
    public boolean has(ServerPlayer player, Identifier advancement) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @Override
    public boolean grant(ServerPlayer player, Identifier advancement) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
        if (holder == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        remaining.forEach(criterion -> player.getAdvancements().award(holder, criterion));
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** How many kinds are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
