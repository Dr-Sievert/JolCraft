package net.sievert.jolcraft.module.primitive.menu.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import net.sievert.jolcraft.module.primitive.menu.MenuKey;
import net.sievert.jolcraft.stereotype.Helper;

/**
 * The client half of {@code menu}: a module's client bootstrap gives each of its menus the screen the client opens
 * for it. A menu without a screen is dropped by the client when opened, with a line in the client's log, while the
 * server keeps it open; every menu's client bootstrap gives it one.
 */
@Helper
public final class ClientMenu {

    private ClientMenu() {
    }

    public static <M extends AbstractContainerMenu, S extends Screen & MenuAccess<@org.jetbrains.annotations.NotNull M>> void screen(IEventBus modBus, MenuKey<M> key, MenuScreens.ScreenConstructor<@org.jetbrains.annotations.NotNull M, @org.jetbrains.annotations.NotNull S> screen) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(key.type(), screen));
    }
}
