package net.sievert.jolcraft.datagen.module.system.catalog;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.LanguageContributor;
import net.sievert.jolcraft.datagen.provider.Translations;
import net.sievert.jolcraft.module.system.catalog.CatalogKeys;
import net.sievert.jolcraft.util.text.Dictionary;

/** The English title of the mod's creative tab. */
@Contributes(Dictionary.CATALOG)
public final class CatalogText implements LanguageContributor {

    @Override
    public void english(Translations text) {
        text.add(CatalogKeys.TAB, "JolCraft");
    }
}
