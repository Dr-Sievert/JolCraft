package net.sievert.jolcraft.module.primitive.stat;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Stat} over the game's custom statistics: the keys are kept until the game registers. */
@Implementation
public final class ModStat implements Stat {

    private final Map<Identifier, StatKey> declared = new LinkedHashMap<>();
    private boolean registered;

    @Override
    public void declare(StatKey key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the statistics were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; a statistic's id is the mod's own");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the statistics were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.CUSTOM_STAT, helper -> declared.values().forEach(key -> {
            helper.register(key.id(), key.id());
            Stats.CUSTOM.get(key.id(), key.formatter()); // as the game does for its own: the statistic exists with its formatter before anyone asks
        }));
    }

    @Override
    public void award(ServerPlayer player, StatKey key, int amount) {
        net.minecraft.stats.Stat<Identifier> stat = declared(key).stat();
        player.awardStat(stat, amount);
    }

    @Override
    public int value(ServerPlayer player, StatKey key) {
        net.minecraft.stats.Stat<Identifier> stat = declared(key).stat();
        return player.getStats().getValue(stat);
    }

    private StatKey declared(StatKey key) {
        StatKey known = declared.get(key.id());
        if (known == null) {
            throw new IllegalArgumentException(key + " was never declared through Stat; a module declares its statistics while it is built");
        }
        return known;
    }

    /** How many statistics are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
