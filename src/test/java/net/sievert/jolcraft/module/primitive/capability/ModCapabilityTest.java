package net.sievert.jolcraft.module.primitive.capability;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping; the game registers capabilities once per run, so scenes in the game-test mod ask a real entity and a real stack. */
class ModCapabilityTest {

    private static final EntityCapability<String, Void> GREETING = EntityCapability.createVoid(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "greeting"), String.class);
    private static final ItemCapability<String, Void> LABEL = ItemCapability.createVoid(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "label"), String.class);
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void providersAreCountedAndAnItemProviderNeedsAnItem() {
        ModCapability capability = new ModCapability();
        capability.entity(GREETING, () -> EntityType.ARMOR_STAND, (_, _) -> "hello");
        capability.item(LABEL, (_, _) -> "stick", Items.STICK);
        Assertions.assertEquals(2, capability.declared());
        Assertions.assertThrows(IllegalArgumentException.class, () -> capability.item(LABEL, (_, _) -> "nothing"));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModCapability capability = new ModCapability();
        capability.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> capability.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> capability.entity(GREETING, () -> EntityType.ARMOR_STAND, (_, _) -> "late"));
    }

    @Test
    void theTypeIsAskedForAtRegistrationNotAtDeclaration() {
        ModCapability capability = new ModCapability();
        capability.entity(GREETING, () -> {
            throw new AssertionError("the type was asked for at declaration");
        }, (_, _) -> "hello");
        Assertions.assertEquals(1, capability.declared(), "declared without touching the type");
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.CAPABILITY, new ModCapability().id());
    }
}
