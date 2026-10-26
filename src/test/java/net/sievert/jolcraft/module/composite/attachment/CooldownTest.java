package net.sievert.jolcraft.module.composite.attachment;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.module.primitive.attachment.ModAttachment;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.util.text.Dictionary;

/** The key's shape and the guards; counting down needs an entity on a level and is a scene in the game-test mod. */
class CooldownTest {

    private final Cooldown.Key strike = Cooldown.Key.of(Identifier.fromNamespaceAndPath(Dictionary.JOLCRAFT, "strike"));

    @Test
    void aKeyIsASavedAttachmentUnderItsId() {
        Assertions.assertEquals(strike.id(), strike.attachment().id());
        Assertions.assertTrue(strike.attachment().isSaved(), "a cooldown survives a save");
        Assertions.assertFalse(strike.attachment().isSynced());
        Assertions.assertTrue(strike.attachment().isKeptOnDeath(), "a cooldown survives a death");
    }

    @Test
    void declaringReachesTheAttachmentsAndALengthIsAtLeastOneTick() {
        ModAttachment attachment = new ModAttachment();
        Cooldown cooldown = new Cooldown(attachment, new ModLog(LogUtils.getLogger()));
        cooldown.declare(strike);
        Assertions.assertEquals(1, attachment.declared());
        Assertions.assertThrows(IllegalArgumentException.class, () -> cooldown.start(null, strike, 0));
        Assertions.assertEquals(Dictionary.COOLDOWN, cooldown.id());
    }
}
