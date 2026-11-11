package net.sievert.jolcraft.module.system.catalog;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.primitive.tab.TabKey;
import net.sievert.jolcraft.stereotype.Keys;
import net.sievert.jolcraft.util.text.Dictionary;

/** The catalog system's handle: the mod's one creative tab, which {@link Catalog} declares to {@code Tab}. */
@Keys
public final class CatalogKeys {

    /** The tab {@code jolcraft:jolcraft}, which every system with items adds to. */
    public static final TabKey TAB = TabKey.of(Ids.of(Dictionary.JOLCRAFT, Dictionary.JOLCRAFT));

    private CatalogKeys() {
    }
}
