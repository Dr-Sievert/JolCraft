package net.sievert.jolcraft.module.primitive.registration;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** What a registry declaration is refused for, before anything reaches the game. */
class ModRegistrationDeclarationsTest {

    @Test
    void aRegistryIsDeclaredOnceAndUnderTheModsNamespace() {
        ModRegistration registration = new ModRegistration();
        ResourceKey<Registry<String>> key = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "words"));
        registration.custom(key, false);
        Assertions.assertTrue(Assertions.assertThrows(IllegalStateException.class, () -> registration.datapack(key, Codec.STRING, null)).getMessage().contains("declared twice"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registration.custom(ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("words")), false));
        Assertions.assertEquals(1, registration.declared());
    }
}
