package net.sievert.jolcraft.module.primitive.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class IdsTest {

    @Test
    void wordsJoinWithUnderscoresUnderTheNamespace() {
        Assertions.assertEquals(Identifier.parse("jolcraft:coin_purse"), Ids.of(Dictionary.JOLCRAFT, "coin", "purse"));
    }

    @Test
    void aPathKeepsItsFolders() {
        Assertions.assertEquals(Identifier.parse("c:ingots/copper"), Ids.path(Dictionary.C, "ingots/copper"));
    }

    @Test
    void aRegistryKeyNamesTheRegistry() {
        Assertions.assertEquals(Identifier.parse("jolcraft:recipe_output_type"), Ids.registry(Dictionary.JOLCRAFT, "recipe", "output", "type").identifier());
    }

    @Test
    void anEntryKeyBelongsToItsRegistry() {
        ResourceKey<Item> key = Ids.key(Registries.ITEM, Dictionary.JOLCRAFT, "coin");
        Assertions.assertEquals(Registries.ITEM.identifier(), key.registry());
        Assertions.assertEquals(Identifier.parse("jolcraft:coin"), key.identifier());
    }

    @Test
    void aTagKeyBelongsToItsRegistry() {
        TagKey<Item> tag = Ids.tag(Registries.ITEM, Dictionary.C, "gems");
        Assertions.assertEquals(Registries.ITEM, tag.registry());
        Assertions.assertEquals(Identifier.parse("c:gems"), tag.location());
    }

    @Test
    void aWordThatIsNoValidPathIsRefused() {
        Assertions.assertThrows(RuntimeException.class, () -> Ids.of(Dictionary.JOLCRAFT, "Coin"));
    }
}
