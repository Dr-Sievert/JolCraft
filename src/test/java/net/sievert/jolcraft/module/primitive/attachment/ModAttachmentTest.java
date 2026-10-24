package net.sievert.jolcraft.module.primitive.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.util.text.Dictionary;

/** The bookkeeping; the game's registries are already frozen when these run, so scenes in the game-test mod carry real values. */
class ModAttachmentTest {

    private final AttachmentKey<Integer> mood = AttachmentKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "mood"), () -> 0);
    private final AttachmentKey<Integer> steps = AttachmentKey.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "steps"), () -> 0).saved(Codec.INT.fieldOf("value"));
    private final IEventBus bus = BusBuilder.builder().build();

    @Test
    void keysAreCountedOncePerId() {
        ModAttachment attachment = new ModAttachment();
        attachment.declare(mood);
        attachment.declare(steps);
        Assertions.assertEquals(2, attachment.declared());
        Assertions.assertThrows(IllegalStateException.class, () -> attachment.declare(mood.saved(Codec.INT.fieldOf("value"))), "same id, another shape");
    }

    @Test
    void aKeyKeptOnDeathMustBeSaved() {
        Assertions.assertThrows(IllegalStateException.class, () -> new ModAttachment().declare(mood.keptOnDeath()));
        Assertions.assertDoesNotThrow(() -> new ModAttachment().declare(steps.keptOnDeath()));
    }

    @Test
    void theGamesNamespaceIsRefused() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ModAttachment().declare(AttachmentKey.of(Identifier.withDefaultNamespace("mood"), () -> 0)));
    }

    @Test
    void registeringTwiceOrDeclaringAfterItIsRefused() {
        ModAttachment attachment = new ModAttachment();
        attachment.declare(mood);
        attachment.register(bus);
        Assertions.assertThrows(IllegalStateException.class, () -> attachment.register(bus));
        Assertions.assertThrows(IllegalStateException.class, () -> attachment.declare(steps));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.ATTACHMENT, new ModAttachment().id());
    }
}
