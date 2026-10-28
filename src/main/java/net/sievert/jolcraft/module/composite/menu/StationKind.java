package net.sievert.jolcraft.module.composite.menu;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.module.primitive.menu.MenuKey;
import net.sievert.jolcraft.stereotype.Handle;

/**
 * One kind of station: its id, its {@link StationLayout}, the title of its menu and default name of its block entity,
 * how many ints its menu syncs, whether its block entity sends its items to clients, the sprites its screen draws and
 * the factory that builds its menu. A consumer keeps its kind as a constant, since its block entity and menu name it
 * before the system exists, and declares it through {@link Station} while modules are built. The kind builds and holds
 * the {@link MenuKey}, whose client side makes the client's copy from the kind alone: a simple container of the
 * layout's size and simple data of the count, reading nothing from the buffer. The withers return a new kind with the
 * same id; only the declared kind's key is registered, and any key of that id binds to its type.
 */
@Handle
public sealed interface StationKind<M extends StationMenu> {

    /** A kind with the base menu, no data, no synced items and no art. */
    static StationKind<StationMenu> of(Identifier id, StationLayout layout, LanguageKey title) {
        return new Spec<>(id, layout, title, 0, false, Optional.empty(), Optional.empty(), StationMenu::new);
    }

    Identifier id();

    StationLayout layout();

    /** The title of the menu and the default name of the block entity. */
    LanguageKey title();

    /** How many ints the menu syncs from the server. */
    int dataCount();

    /** Whether the block entity sends its item cells to the clients that track it. */
    boolean sendsItems();

    /** The sprite of the panel, read by the screen. */
    Optional<Identifier> panel();

    /** The sprite of a slot's well, read by the screen. */
    Optional<Identifier> slot();

    /** The key of the menu, declared to {@code menu} by {@link Station#declare}. */
    MenuKey<M> menu();

    Factory<M> factory();

    /** The same kind with a menu subclass built by the factory. */
    <N extends StationMenu> StationKind<N> menu(Factory<N> factory);

    /** The same kind syncing {@code count} ints to the menu, or none. */
    StationKind<M> data(int count);

    /** The same kind whose block entity sends its item cells to the clients that track it. */
    StationKind<M> syncedItems();

    /** The same kind drawn with the panel and slot sprites. */
    StationKind<M> art(Identifier panel, Identifier slot);

    /** Builds a menu of this kind on either side through its factory. */
    default M create(int windowId, Inventory inventory, Container container, ContainerData data) {
        return factory().create(this, windowId, inventory, container, data);
    }

    /** How a kind builds its menu. */
    @FunctionalInterface
    interface Factory<M extends StationMenu> {

        M create(StationKind<M> kind, int windowId, Inventory inventory, Container container, ContainerData data);
    }

    /** The kind with its key, built from the kind itself. */
    final class Spec<M extends StationMenu> implements StationKind<M> {

        private final Identifier id;
        private final StationLayout layout;
        private final LanguageKey title;
        private final int dataCount;
        private final boolean syncedItems;
        private final Optional<Identifier> panel;
        private final Optional<Identifier> slot;
        private final Factory<M> factory;
        private final MenuKey<M> menu;

        Spec(Identifier id, StationLayout layout, LanguageKey title, int dataCount, boolean syncedItems, Optional<Identifier> panel,
             Optional<Identifier> slot, Factory<M> factory) {
            if (dataCount < 0) {
                throw new IllegalArgumentException(id + " syncs " + dataCount + " ints; none or more");
            }
            this.id = id;
            this.layout = layout;
            this.title = title;
            this.dataCount = dataCount;
            this.syncedItems = syncedItems;
            this.panel = panel;
            this.slot = slot;
            this.factory = factory;
            this.menu = MenuKey.of(id, (windowId, inventory, _) -> create(windowId, inventory,
                    new SimpleContainer(layout().size()), new SimpleContainerData(dataCount())));
        }

        @Override
        public Identifier id() {
            return id;
        }

        @Override
        public StationLayout layout() {
            return layout;
        }

        @Override
        public LanguageKey title() {
            return title;
        }

        @Override
        public int dataCount() {
            return dataCount;
        }

        @Override
        public boolean sendsItems() {
            return syncedItems;
        }

        @Override
        public Optional<Identifier> panel() {
            return panel;
        }

        @Override
        public Optional<Identifier> slot() {
            return slot;
        }

        @Override
        public MenuKey<M> menu() {
            return menu;
        }

        @Override
        public Factory<M> factory() {
            return factory;
        }

        @Override
        public <N extends StationMenu> StationKind<N> menu(Factory<N> factory) {
            return new Spec<>(id, layout, title, dataCount, syncedItems, panel, slot, factory);
        }

        @Override
        public StationKind<M> data(int count) {
            return new Spec<>(id, layout, title, count, syncedItems, panel, slot, factory);
        }

        @Override
        public StationKind<M> syncedItems() {
            return new Spec<>(id, layout, title, dataCount, true, panel, slot, factory);
        }

        @Override
        public StationKind<M> art(Identifier panel, Identifier slot) {
            return new Spec<>(id, layout, title, dataCount, syncedItems, Optional.of(panel), Optional.of(slot), factory);
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
