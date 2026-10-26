package net.sievert.jolcraft.module.primitive.menu;

import net.neoforged.bus.api.IEventBus;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's menus: the containers a player opens on a bench, a strongbox or a dwarf. A module declares a
 * {@link MenuKey} per menu while it is built, with the way the client builds its copy from the data the server
 * sends; the root calls {@link #register(IEventBus)} once; from then on the key opens the menu for a player on the
 * server, and the module's client bootstrap gives the menu its screen through {@code client.ClientMenu}.
 */
@Primitive(Dictionary.MENU)
public interface Menu extends Module {

    /** Declares a menu. Only while modules are built, once per id. */
    void declare(MenuKey<?> key);

    /** Registers every declared menu's type with the game. The root calls it once. */
    void register(IEventBus modBus);
}
