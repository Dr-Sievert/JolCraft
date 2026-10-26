package net.sievert.jolcraft.module.primitive.registration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Registration} over NeoForge's registry events. */
@Implementation
public final class ModRegistration implements Registration {

    private record Datapack<T>(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> network) {

        void declare(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(key, codec, network);
        }
    }

    /** A custom registry's declaration, filled in when the game creates it; a supplier to its users. */
    private static final class Custom<T> implements Supplier<Registry<T>> {

        private final ResourceKey<Registry<T>> key;
        private final boolean synced;
        private @Nullable Registry<T> registry;

        Custom(ResourceKey<Registry<T>> key, boolean synced) {
            this.key = key;
            this.synced = synced;
        }

        void create(NewRegistryEvent event) {
            registry = event.create(new RegistryBuilder<>(key).sync(synced));
        }

        @Override
        public Registry<T> get() {
            if (registry == null) {
                throw new IllegalStateException(key.identifier() + " is not created yet: the game creates custom registries after the mod constructors ran");
            }
            return registry;
        }
    }

    private final List<Custom<?>> customs = new ArrayList<>();
    private final List<Datapack<?>> datapacks = new ArrayList<>();
    private final Set<ResourceKey<?>> keys = new HashSet<>();
    private boolean registered;

    @Override
    public <T> Supplier<Registry<T>> custom(ResourceKey<Registry<T>> key, boolean synced) {
        open(key);
        Custom<T> custom = new Custom<>(key, synced);
        customs.add(custom);
        return custom;
    }

    @Override
    public <T> void datapack(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> network) {
        open(key);
        datapacks.add(new Datapack<>(key, codec, network));
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("registration was attached to the mod bus twice");
        }
        registered = true;
        modBus.addListener(NewRegistryEvent.class, event -> customs.forEach(custom -> custom.create(event)));
        modBus.addListener(DataPackRegistryEvent.NewRegistry.class, event -> datapacks.forEach(datapack -> datapack.declare(event)));
    }

    /** How many custom and datapack registries are declared, for the root's log line. */
    public int declared() {
        return customs.size() + datapacks.size();
    }

    private void open(ResourceKey<?> key) {
        if (registered) {
            throw new IllegalStateException(key.identifier() + " was declared after registration was attached to the mod bus");
        }
        if (key.identifier().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key.identifier() + " uses the game's namespace; a registry of the mod's is named under its own");
        }
        if (!keys.add(key)) {
            throw new IllegalStateException(key.identifier() + " was declared twice");
        }
    }
}
