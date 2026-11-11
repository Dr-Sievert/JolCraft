package net.sievert.jolcraft.module.system.catalog;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The mod's one creative tab keeps the id the root gave it and is registered under it, titled with its key. */
class CatalogKeysTest {

    @Test
    void theTabIsTheModsOneAndIsTitledUnderItsNamespace() {
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft", "jolcraft"), CatalogKeys.TAB.id());
        Assertions.assertEquals("jolcraft.tab.jolcraft", CatalogKeys.TAB.title());
    }

    @Test
    void theGameRegisteredItUnderThatIdWithThatTitle() {
        Assertions.assertTrue(CatalogKeys.TAB.isRegistered());
        TranslatableContents title = (TranslatableContents) CatalogKeys.TAB.tab().getDisplayName().getContents();
        Assertions.assertEquals("jolcraft.tab.jolcraft", title.getKey());
    }
}
