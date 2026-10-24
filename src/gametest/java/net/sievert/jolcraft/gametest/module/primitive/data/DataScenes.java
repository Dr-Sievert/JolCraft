package net.sievert.jolcraft.gametest.module.primitive.data;

import java.nio.file.Path;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

import net.sievert.jolcraft.gametest.scene.Scene;
import net.sievert.jolcraft.gametest.scene.TestScene;

/**
 * The data primitive against a real server: a level hands back one instance and an update marks it dirty,
 * server-wide data is one instance from every level while per-level data is not, and what is written comes back
 * through the codec from a fresh storage on the same folder, and server-wide data lands in the server's own folder.
 */
public final class DataScenes {

    @Scene(maxTicks = 20)
    public void aLevelHandsBackOneInstanceAndAnUpdateMarksItDirty(TestScene scene) {
        ServerLevel level = scene.level();
        TestData.Counter counter = TestData.LOCAL.get(level);
        scene.check("the same instance comes back", () -> scene.assertTrue(counter == TestData.LOCAL.get(level), "one instance per level"));
        counter.setDirty(false); // the game marks fresh data dirty itself; only the update may mark it from here
        int before = counter.count();
        TestData.LOCAL.update(level, data -> data.add(3));
        scene.check("the update changed it", () -> scene.assertEquals(before + 3, counter.count(), "count after update"));
        scene.check("and marked it dirty", () -> scene.assertTrue(counter.isDirty(), "an updated data is dirty until saved"));
        scene.succeed();
    }

    @Scene(maxTicks = 20)
    public void serverWideDataIsOneAcrossLevelsAndPerLevelDataIsNot(TestScene scene) {
        ServerLevel level = scene.level();
        MinecraftServer server = level.getServer();
        ServerLevel other = server.getLevel(level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER);
        scene.check("another level exists to compare with", () -> scene.assertTrue(other != null, "the server has a second level"));
        scene.check("server-wide data is one instance", () -> scene.assertTrue(
                TestData.SHARED.get(level) == TestData.SHARED.get(other) && TestData.SHARED.get(level) == TestData.SHARED.get(server), "one for the server"));
        scene.check("per-level data is one per level", () -> scene.assertTrue(TestData.LOCAL.get(level) != TestData.LOCAL.get(other), "one per level"));
        scene.succeed();
    }

    @Scene(maxTicks = 40)
    public void writtenDataComesBackThroughItsCodec(TestScene scene) {
        ServerLevel level = scene.level();
        MinecraftServer server = level.getServer();
        TestData.LOCAL.get(level);
        level.getDataStorage().saveAndJoin(); // writes whatever is there, so the 42 below reaches the disk only through the update's dirty mark
        TestData.LOCAL.update(level, data -> data.add(42 - data.count()));
        level.getDataStorage().saveAndJoin();
        Path folder = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT)).resolve("data");
        try (SavedDataStorage fresh = new SavedDataStorage(folder, server.getFixerUpper(), level.registryAccess())) {
            TestData.Counter read = fresh.get(TestData.LOCAL.type());
            scene.check("the file is there and reads back", () -> scene.assertTrue(read != null, "the saved file for " + TestData.LOCAL + " in " + folder));
            scene.check("with the written count", () -> scene.assertEquals(42, read.count(), "count read back"));
        }
        scene.succeed();
    }

    @Scene(maxTicks = 40)
    public void serverWideDataIsWrittenInTheServersFolderNotTheLevels(TestScene scene) {
        ServerLevel level = scene.level();
        MinecraftServer server = level.getServer();
        TestData.SHARED.get(server);
        server.getDataStorage().saveAndJoin();
        TestData.SHARED.update(level, data -> data.add(7 - data.count()));
        server.getDataStorage().saveAndJoin();
        Path serverFolder = server.getWorldPath(LevelResource.DATA);
        Path levelFolder = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT)).resolve("data");
        scene.check("the server keeps its data apart from the level's", () -> scene.assertFalse(serverFolder.equals(levelFolder), "two folders"));
        try (SavedDataStorage fresh = new SavedDataStorage(serverFolder, server.getFixerUpper(), level.registryAccess())) {
            TestData.Counter read = fresh.get(TestData.SHARED.type());
            scene.check("the server's folder holds it", () -> scene.assertTrue(read != null && read.count() == 7, "server-wide data in " + serverFolder));
        }
        try (SavedDataStorage fresh = new SavedDataStorage(levelFolder, server.getFixerUpper(), level.registryAccess())) {
            scene.check("the level's folder does not", () -> scene.assertTrue(fresh.get(TestData.SHARED.type()) == null, "no server-wide file in " + levelFolder));
        }
        scene.succeed();
    }
}
