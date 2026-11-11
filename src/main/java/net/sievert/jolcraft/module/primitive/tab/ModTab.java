package net.sievert.jolcraft.module.primitive.tab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Tab} over the game's creative tabs: the keys and their entries are kept until the game registers. */
@Implementation
public final class ModTab implements Tab {

    private final Map<Identifier, TabKey> declared = new LinkedHashMap<>();
    private final Map<Identifier, List<ItemLike>> entries = new LinkedHashMap<>();
    private boolean registered;

    @Override
    public void declare(TabKey key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the creative tabs were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; a creative tab's id is the mod's own");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
    }

    @Override
    public void add(TabKey key, ItemLike... items) {
        if (registered) {
            throw new IllegalStateException("entries were added to " + key + " after the creative tabs were registered");
        }
        List<ItemLike> tab = entries.computeIfAbsent(key.id(), _ -> new ArrayList<>());
        for (ItemLike item : items) {
            if (tab.stream().anyMatch(held -> held == item)) {
                throw new IllegalStateException(item + " was added twice to " + key + "; the game refuses the same stack twice in a tab");
            }
            tab.add(item);
        }
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the creative tabs were registered twice");
        }
        entries.keySet().forEach(id -> {
            if (!declared.containsKey(id)) {
                throw new IllegalStateException("entries were added to " + id + ", which no module declared as a creative tab");
            }
        });
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> declared.values().forEach(key -> helper.register(key.id(), build(key))));
    }

    /** The game's tab for a declared key: titled, iconed by its first entry and displaying its entries in order. */
    CreativeModeTab build(TabKey key) {
        return CreativeModeTab.builder()
                .title(Component.translatable(key.title()))
                .icon(() -> icon(key).map(ItemStack::new).orElse(ItemStack.EMPTY))
                .displayItems(display(key))
                .build();
    }

    /** What the tab displays when the game builds its contents: its entries, in order. */
    CreativeModeTab.DisplayItemsGenerator display(TabKey key) {
        return (_, output) -> entries(key).forEach(output::accept);
    }

    /** The tab's first entry, which the icon shows; none for a tab without entries. */
    Optional<ItemLike> icon(TabKey key) {
        return entries(key).stream().findFirst();
    }

    private List<ItemLike> entries(TabKey key) {
        return entries.getOrDefault(key.id(), List.of());
    }

    /** How many creative tabs are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
