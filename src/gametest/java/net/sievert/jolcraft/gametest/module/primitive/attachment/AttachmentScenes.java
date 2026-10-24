package net.sievert.jolcraft.gametest.module.primitive.attachment;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestPlayer;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The attachment primitive against real holders: a value is read back, written with the holder when saved, carried
 * over a death when kept, and sent to the players who see it when synced. The last two add players and are isolated.
 */
public final class AttachmentScenes {

    private static final BlockPos STAND = new BlockPos(0, 1, 0);

    @Scene(maxTicks = 20)
    public void aHolderAnswersTheFallbackUntilAValueIsSet(TestScene scene) {
        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, STAND);
        scene.check("nothing is stored at first", () -> scene.assertFalse(TestAttachments.PASSING.isSet(stand), "a fresh holder carries nothing"));
        scene.check("the fallback answers", () -> scene.assertEquals(0, TestAttachments.PASSING.get(stand), "fallback"));
        TestAttachments.PASSING.set(stand, 5);
        scene.check("the set value answers", () -> scene.assertEquals(5, TestAttachments.PASSING.get(stand), "after set"));
        scene.check("an update answers and stores the change", () -> scene.assertEquals(6, TestAttachments.PASSING.update(stand, value -> value + 1), "after update"));
        TestAttachments.PASSING.reset(stand);
        scene.check("a reset holder answers the fallback again", () -> scene.assertEquals(0, TestAttachments.PASSING.get(stand), "after reset"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aSavedValueIsWrittenWithTheHolderAndAPassingOneIsNot(TestScene scene) {
        ArmorStand stand = scene.helper().spawn(EntityType.ARMOR_STAND, STAND);
        TestAttachments.SAVED.set(stand, 4);
        TestAttachments.PASSING.set(stand, 9);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, scene.level().registryAccess());
        stand.saveWithoutId(output);
        CompoundTag attachments = output.buildResult().getCompoundOrEmpty(AttachmentHolder.ATTACHMENTS_NBT_KEY);
        scene.check("the saved value is in the holder's data",
                () -> scene.assertEquals(4, attachments.getCompoundOrEmpty(TestAttachments.SAVED.id().toString()).getIntOr("value", -1), "saved value"));
        scene.check("the passing value is not",
                () -> scene.assertFalse(attachments.contains(TestAttachments.PASSING.id().toString()), "a passing value is never written"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void aKeptValueSurvivesADeathAndAMerelySavedOneDoesNot(TestScene scene) {
        ArmorStand dying = scene.helper().spawn(EntityType.ARMOR_STAND, STAND);
        ArmorStand reborn = scene.helper().spawn(EntityType.ARMOR_STAND, STAND.east());
        TestAttachments.KEPT.set(dying, 7);
        TestAttachments.SAVED.set(dying, 8);
        // What the game does for a respawning player or a converted entity.
        reborn.copyAttachmentsFrom(dying, true);
        scene.check("the kept value came along", () -> scene.assertEquals(7, TestAttachments.KEPT.get(reborn), "kept value"));
        scene.check("the merely saved value did not", () -> scene.assertFalse(TestAttachments.SAVED.isSet(reborn), "saved but not kept"));
        scene.succeed();
    }

    @Scene(maxTicks = 40, isolated = true)
    public void aSyncedValueIsSentToThePlayerWhoCarriesItAndAnUnsyncedOneIsNot(TestScene scene) {
        TestPlayer player = scene.addPlayer("Carrier");
        player.clearReceived();
        TestAttachments.KEPT.set(player.player(), 5);
        scene.check("setting a synced value sent the player a sync", () -> scene.assertFalse(syncs(player).isEmpty(), "expected a sync payload"));
        player.clearReceived();
        TestAttachments.PASSING.set(player.player(), 5);
        scene.check("setting an unsynced value sent nothing", () -> scene.assertTrue(syncs(player).isEmpty(), "expected no sync payload, got " + syncs(player)));
        scene.succeed();
    }

    @Scene(maxTicks = 60, isolated = true)
    public void aSyncedValueIsSentToEveryPlayerWhoSeesTheHolder(TestScene scene) {
        TestPlayer carrier = scene.addPlayer("Carrier");
        TestPlayer watcher = scene.addPlayer("Watcher");
        scene.runAtTick(10, () -> {
            scene.check("the watcher sees the carrier", () -> scene.assertTrue(
                    scene.level().getChunkSource().chunkMap.getPlayersWatching(carrier.player()).contains(watcher.player()), "the watcher must track the carrier first"));
            watcher.clearReceived();
            TestAttachments.KEPT.set(carrier.player(), 5);
            scene.check("the watcher was sent the sync", () -> scene.assertFalse(syncs(watcher).isEmpty(), "expected a sync payload at the watcher"));
            scene.succeed();
        });
    }

    private static List<SyncAttachmentsPayload> syncs(TestPlayer player) {
        return player.received(ClientboundCustomPayloadPacket.class).stream()
                .map(ClientboundCustomPayloadPacket::payload)
                .filter(SyncAttachmentsPayload.class::isInstance)
                .map(SyncAttachmentsPayload.class::cast)
                .toList();
    }
}
