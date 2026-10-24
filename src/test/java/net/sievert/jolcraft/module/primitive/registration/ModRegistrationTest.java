package net.sievert.jolcraft.module.primitive.registration;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

class ModRegistrationTest {

    private final ResourceKey<Registry<String>> customKey = Ids.registry(Dictionary.JOLCRAFT, "test", "custom");
    private final ResourceKey<Registry<String>> datapackKey = Ids.registry(Dictionary.JOLCRAFT, "test", "datapack");

    @Test
    void aCustomRegistryIsDeclaredNowAndCreatedByTheGameLater() {
        ModRegistration registration = new ModRegistration();
        Supplier<Registry<String>> registry = registration.custom(customKey, true);
        Assertions.assertEquals(1, registration.declared());
        IllegalStateException notYet = Assertions.assertThrows(IllegalStateException.class, registry::get);
        Assertions.assertTrue(notYet.getMessage().contains(customKey.identifier().toString()));
    }

    @Test
    void declarationsAreCountedAndAttachedOnce() {
        ModRegistration registration = new ModRegistration();
        registration.datapack(datapackKey, Codec.STRING, null);
        IEventBus bus = BusBuilder.builder().build();
        registration.register(bus);
        Assertions.assertEquals(1, registration.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> registration.register(bus));
    }

    @Test
    void aDeclarationAfterAttachingIsRefused() {
        ModRegistration registration = new ModRegistration();
        registration.register(BusBuilder.builder().build());
        Assertions.assertThrows(IllegalStateException.class, () -> registration.datapack(datapackKey, Codec.STRING, null));
        Assertions.assertThrows(IllegalStateException.class, () -> registration.custom(customKey, false));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.REGISTRATION, new ModRegistration().id());
    }
}
