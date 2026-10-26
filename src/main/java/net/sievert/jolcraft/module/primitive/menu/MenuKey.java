package net.sievert.jolcraft.module.primitive.menu;

import java.util.OptionalInt;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.sievert.jolcraft.stereotype.Handle;
import org.jetbrains.annotations.NotNull;

/**
 * One menu of the mod: its id, under which the game knows its type, and how the client builds its copy from the
 * window id, the player's inventory and the data the server wrote when it opened the menu. A key is a constant
 * where it is used, declared through {@link Menu} while its module is built; the server-side menu takes what it
 * needs through {@link #open}, where the module builds it with its own dependencies and writes the client's data.
 * The menu class passes {@link #type()} to its {@code AbstractContainerMenu} constructor.
 */
@Handle
public sealed interface MenuKey<M extends AbstractContainerMenu> {

    static <M extends AbstractContainerMenu> MenuKey<M> of(Identifier id, ClientSide<M> client) {
        return new Spec<>(id, client);
    }

    Identifier id();

    /** The registered type, there once the game registered what a module declared. */
    MenuType<@org.jetbrains.annotations.NotNull M> type();

    /** The game's type for this key, built once by {@link Menu} when the game registers. */
    MenuType<@org.jetbrains.annotations.NotNull M> build();

    /**
     * Opens the menu for the player: the server side is built by {@code server}, with the window id the game picks,
     * and the client receives the title and what {@code data} writes to build its copy. The window id, or empty when
     * the game did not open it.
     */
    default OptionalInt open(ServerPlayer player, Component title, ServerSide<M> server, Consumer<RegistryFriendlyByteBuf> data) {
        return player.openMenu(new SimpleMenuProvider(server::create, title), data);
    }

    /** Opens the menu for the player with no data for the client. */
    default OptionalInt open(ServerPlayer player, Component title, ServerSide<M> server) {
        return open(player, title, server, _ -> { });
    }

    /** How the client builds its copy of the menu; the data is what the server wrote on opening, empty when it wrote nothing. */
    @FunctionalInterface
    interface ClientSide<M extends AbstractContainerMenu> {

        M create(int windowId, Inventory inventory, RegistryFriendlyByteBuf data);
    }

    /** How the server builds the menu for the opening player. */
    @FunctionalInterface
    interface ServerSide<M extends AbstractContainerMenu> {

        M create(int windowId, Inventory inventory, Player opener);
    }

    /** The key with its client-side constructor; {@code deferred} resolves the registered type lazily. */
    record Spec<M extends AbstractContainerMenu>(Identifier id, ClientSide<M> client, DeferredHolder<MenuType<?>, @org.jetbrains.annotations.NotNull MenuType<@org.jetbrains.annotations.NotNull M>> deferred) implements MenuKey<M> {

        Spec(Identifier id, ClientSide<M> client) {
            this(id, client, DeferredHolder.create(Registries.MENU, id));
        }

        @Override
        public MenuType<@NotNull M> type() {
            if (!deferred.isBound()) {
                throw new IllegalStateException(id + " is not registered: no module declared it through Menu,"
                        + " or it is used before the game registered menu types, which happens after the mod constructors ran");
            }
            return deferred.get();
        }

        @Override
        public MenuType<@NotNull M> build() {
            // The game hands the client null when the server wrote no data; the client side is promised an empty buffer instead.
            return IMenuTypeExtension.create((windowId, inventory, data) -> client.create(windowId, inventory,
                    data != null ? data : new RegistryFriendlyByteBuf(Unpooled.EMPTY_BUFFER, inventory.player.registryAccess(), ConnectionType.OTHER)));
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }
}
