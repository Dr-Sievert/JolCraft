package net.sievert.jolcraft.module.primitive.loot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Loot} over the game's loot registries and the server's reloadable tables. */
@Implementation
public final class ModLoot implements Loot {

    private final Map<LootKey.Kind, Map<Identifier, LootKey<?>>> declared = new EnumMap<>(LootKey.Kind.class);
    private final Map<LootKey.Kind, Set<MapCodec<?>>> codecs = new EnumMap<>(LootKey.Kind.class);
    private boolean registered;

    @Override
    public void declare(LootKey<?> key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the loot kinds were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; a loot kind's id is the mod's own");
        }
        var ofKind = declared.computeIfAbsent(key.kind(), _ -> new LinkedHashMap<>());
        var codecsOfKind = codecs.computeIfAbsent(key.kind(), _ -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (ofKind.containsKey(key.id())) {
            throw new IllegalStateException(key + " was declared twice");
        }
        if (codecsOfKind.contains(key.codec())) {
            throw new IllegalStateException(key + " shares its codec with another " + key.kind() + " of the mod; the game refuses one codec under two ids");
        }
        ofKind.put(key.id(), key); // nothing is stored before both checks pass, so a refused key leaves no trace
        codecsOfKind.add(key.codec());
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the loot kinds were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    @SuppressWarnings("unchecked")
    private void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                helper -> of(LootKey.Kind.MODIFIER).forEach(key -> helper.register(key.id(), (MapCodec<? extends IGlobalLootModifier>) key.codec())));
        event.register(Registries.LOOT_CONDITION_TYPE,
                helper -> of(LootKey.Kind.CONDITION).forEach(key -> helper.register(key.id(), (MapCodec<? extends LootItemCondition>) key.codec())));
        event.register(Registries.LOOT_FUNCTION_TYPE,
                helper -> of(LootKey.Kind.FUNCTION).forEach(key -> helper.register(key.id(), (MapCodec<? extends LootItemFunction>) key.codec())));
    }

    private List<LootKey<?>> of(LootKey.Kind kind) {
        return List.copyOf(declared.getOrDefault(kind, Map.of()).values());
    }

    @Override
    public List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> table, LootParams params) {
        return level.getServer().reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE)
                .flatMap(tables -> tables.get(table))
                .<List<ItemStack>>map(found -> found.value().getRandomItems(params))
                .orElse(List.of());
    }

    @Override
    public LootContext context(ServerLevel level, Shape shape, Consumer<LootParams.Builder> params) {
        return new LootContext.Builder(paramsUnder(level, shape, params)).create(Optional.empty());
    }

    @Override
    public LootContext context(ServerLevel level, Shape shape, RandomSource random, Consumer<LootParams.Builder> params) {
        return new LootContext.Builder(paramsUnder(level, shape, params)).withOptionalRandomSource(random).create(Optional.empty());
    }

    private static LootParams paramsUnder(ServerLevel level, Shape shape, Consumer<LootParams.Builder> params) {
        LootParams.Builder builder = new LootParams.Builder(level);
        params.accept(builder);
        try {
            return builder.create(shape.keys());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("no context under " + shape + ": " + e.getMessage() + "; the shape says what a caller provides", e);
        }
    }

    /** How many kinds are declared, for the root's log line. */
    public int declared() {
        return declared.values().stream().mapToInt(Map::size).sum();
    }
}
