package net.sievert.jolcraft.module.system.catalog;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.primitive.tab.Tab;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The mod's creative tab, {@link CatalogKeys#TAB}. Every system with items takes {@code Tab} and adds its items to that
 * key while it is built, so the tab's order is the order the root builds the systems in. The tab is hidden while it
 * holds nothing. The class holds no behaviour: the constructor declares the tab, which the root registers once.
 */
@ModSystem(Dictionary.CATALOG)
public final class Catalog implements Module {

    public Catalog(Tab tab) {
        tab.declare(CatalogKeys.TAB);
    }
}
