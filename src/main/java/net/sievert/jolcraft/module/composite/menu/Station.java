package net.sievert.jolcraft.module.composite.menu;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.menu.Menu;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The stations of the mod: benches, mortars and strongboxes, containers with cells of roles that a player and a hopper
 * treat each their own way. A module declares a {@link StationKind} per station while it is built; this composite
 * checks the kind's layout, refuses a second kind of the same id and declares the kind's menu to {@code menu}, so a bad
 * layout fails at startup rather than at the first click, and a block entity built over an undeclared kind refuses
 * naming the remedy. Everything that happens in a running game is done by the bases beside it, which hold no module and
 * speak to the systems through {@link StationEvent}. Built from {@code menu} and {@code log}; design in
 * {@code design/station.md}.
 */
@Composite(Dictionary.STATION)
public final class Station implements Module {

    private final Menu menu;
    private final Log log;
    private final Map<Identifier, StationKind<?>> declared = new LinkedHashMap<>();

    public Station(Menu menu, Log log) {
        this.menu = menu;
        this.log = log.tagged(this);
    }

    /** Declares a station kind; only while modules are built, once per id, with a layout that passes its check. */
    public void declare(StationKind<?> kind) {
        if (declared.containsKey(kind.id())) {
            throw new IllegalStateException(kind + " was declared twice");
        }
        kind.layout().check();
        menu.declare(kind.menu());
        declared.put(kind.id(), kind);
        log.debug("{} declared with {} cells", kind, kind.layout().size());
    }

    /** How many kinds are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
