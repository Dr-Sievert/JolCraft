package net.sievert.jolcraft.module.primitive.tab;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The mod's creative tabs. A module declares its {@link TabKey}s while it is built and adds its items to a tab with
 * {@link #add}; the root calls {@link #register(IEventBus)} once, which registers a tab per key. A tab's icon is its
 * first entry, and the game hides a tab that holds no entries (a category tab with nothing to display is not shown).
 * Every tab is titled under {@link #title(Identifier)}, which datagen demands English for.
 */
@Primitive(Dictionary.TAB)
public interface Tab extends Module {

    /** The translation key of a tab's title: {@code <namespace>.tab.<path>}, the mod's own text starting with its namespace. */
    static String title(Identifier id) {
        return ModString.dotted(id.getNamespace(), Dictionary.TAB, id.getPath());
    }

    /** Declares a tab. Only while modules are built, once per id. */
    void declare(TabKey key);

    /**
     * Appends entries to a tab, in call order. Only while modules are built. The entries are kept as given and resolved
     * when the game builds the tab's contents, so a registry holder may be passed before it is bound. The same entry
     * added twice to one tab is refused, since the game refuses a duplicate stack. The tab may be declared later, but
     * must be by the time of {@link #register(IEventBus)}.
     */
    void add(TabKey key, ItemLike... items);

    /** Registers every declared tab with the game. The root calls it once. */
    void register(IEventBus modBus);
}
