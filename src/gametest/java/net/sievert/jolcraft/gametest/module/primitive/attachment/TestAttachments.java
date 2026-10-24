package net.sievert.jolcraft.gametest.module.primitive.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.primitive.attachment.Attachment;
import net.sievert.jolcraft.module.primitive.attachment.AttachmentKey;
import net.sievert.jolcraft.module.primitive.registration.Ids;

/** The keys the attachment scenes use, one per shape; the game-test mod declares them in its root. */
public final class TestAttachments {

    /** Neither saved nor synced. */
    public static final AttachmentKey<Integer> PASSING = AttachmentKey.of(Ids.of(JolCraftGameTests.MOD_ID, "passing"), () -> 0);

    /** Written with the holder, lost on death. */
    public static final AttachmentKey<Integer> SAVED = AttachmentKey.of(Ids.of(JolCraftGameTests.MOD_ID, "saved"), () -> 0)
            .saved(Codec.INT.fieldOf("value"));

    /** Written, sent to everyone who sees the holder, and carried over a death. */
    public static final AttachmentKey<Integer> KEPT = AttachmentKey.of(Ids.of(JolCraftGameTests.MOD_ID, "kept"), () -> 0)
            .saved(Codec.INT.fieldOf("value"))
            .synced(ByteBufCodecs.VAR_INT)
            .keptOnDeath();

    private TestAttachments() {
    }

    public static void declare(Attachment attachment) {
        attachment.declare(PASSING);
        attachment.declare(SAVED);
        attachment.declare(KEPT);
    }
}
