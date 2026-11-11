package net.sievert.jolcraft.module.primitive.tab;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One creative tab of the mod: its id, under which the game registers and titles it. A key is a constant where it is
 * used, declared through {@link Tab} while its module is built.
 */
@Handle
public sealed interface TabKey {

    static TabKey of(Identifier id) {
        return new Spec(id);
    }

    Identifier id();

    /** The translation key of this tab's title. */
    default String title() {
        return Tab.title(id());
    }

    default ResourceKey<CreativeModeTab> key() {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, id());
    }

    /** Whether the game has registered the tab; it does after the mod constructors ran. */
    default boolean isRegistered() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id());
    }

    /** The game's tab for this key; there once the game registered what a module declared. */
    default CreativeModeTab tab() {
        CreativeModeTab registered = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id());
        if (registered == null) {
            throw new IllegalStateException(id() + " is not registered: no module declared it through Tab, or it is used before the game registered creative tabs, which happens after the mod constructors ran");
        }
        return registered;
    }

    /** The key over its id. */
    record Spec(Identifier id) implements TabKey {

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
