package net.sievert.jolcraft.module.primitive.config;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.util.text.Dictionary;

class ModConfigTest {

    private final ConfigKey<Integer> offers = ConfigKey.whole(3, 1, 9, "How many offers a dwarf shows.", "trade", "offers");
    private final ConfigKey<Boolean> debug = ConfigKey.flag(false, "Log every trade.", "trade", "debug");

    @Test
    void aSettingAnswersItsFallbackBeforeItsFileIsLoaded() {
        ModConfig config = new ModConfig();
        config.declare(Config.File.COMMON, offers);
        config.declare(Config.File.SERVER, debug);
        Assertions.assertEquals(3, config.get(offers));
        Assertions.assertEquals(2, config.build(Dictionary.JOLCRAFT).size());
        Assertions.assertEquals(3, config.get(offers));
        Assertions.assertEquals(false, config.get(debug));
    }

    @Test
    void aKeyNeverDeclaredAnswersItsFallbackUntilTheFilesAreBuiltAndIsAnErrorAfter() {
        ModConfig config = new ModConfig();
        Assertions.assertEquals(3, config.get(offers));
        config.declare(Config.File.COMMON, debug);
        config.build(Dictionary.JOLCRAFT);
        Assertions.assertThrows(IllegalStateException.class, () -> config.get(offers));
        ConfigKey<Boolean> lookalike = ConfigKey.flag(true, "Another comment.", "trade", "debug");
        Assertions.assertThrows(IllegalStateException.class, () -> config.get(lookalike), "a key is its record, not its path");
    }

    @Test
    void declaringTwiceOrAfterBuildingIsRefused() {
        ModConfig config = new ModConfig();
        config.declare(Config.File.COMMON, offers);
        Assertions.assertThrows(IllegalStateException.class, () -> config.declare(Config.File.SERVER, offers));
        config.build(Dictionary.JOLCRAFT);
        Assertions.assertThrows(IllegalStateException.class, () -> config.declare(Config.File.COMMON, debug));
        Assertions.assertThrows(IllegalStateException.class, () -> config.build(Dictionary.JOLCRAFT));
    }

    @Test
    void aPathThatIsAnotherSettingsSectionIsRefusedEitherWayRound() {
        ConfigKey<Boolean> trade = ConfigKey.flag(true, "c", "trade");
        ModConfig sectionFirst = new ModConfig();
        sectionFirst.declare(Config.File.COMMON, offers);
        Assertions.assertThrows(IllegalStateException.class, () -> sectionFirst.declare(Config.File.COMMON, trade));
        Assertions.assertThrows(IllegalStateException.class, () -> sectionFirst.declare(Config.File.SERVER, trade), "across files too, the labels share a key");
        ModConfig settingFirst = new ModConfig();
        settingFirst.declare(Config.File.COMMON, trade);
        Assertions.assertThrows(IllegalStateException.class, () -> settingFirst.declare(Config.File.COMMON, offers));
        Assertions.assertEquals(1, settingFirst.declared());
    }

    @Test
    void settingsOfOneFileShareItsSpecAndSectionsAndCarryTheirKeys() {
        ModConfig config = new ModConfig();
        config.declare(Config.File.COMMON, offers);
        config.declare(Config.File.COMMON, debug);
        Map<Config.File, ModConfigSpec> files = config.build(Dictionary.JOLCRAFT);
        ModConfigSpec common = files.get(Config.File.COMMON);
        Assertions.assertEquals(List.of(Config.File.COMMON), List.copyOf(files.keySet()));
        Assertions.assertEquals("jolcraft.configuration.trade", common.getLevelTranslationKey(List.of("trade")));
        Assertions.assertEquals(2, ((UnmodifiableConfig) common.getValues().get(List.of("trade"))).size());
        ModConfigSpec.ValueSpec spec = (ModConfigSpec.ValueSpec) common.getSpec().get(List.of("trade", "offers"));
        Assertions.assertEquals("jolcraft.configuration.trade.offers", spec.getTranslationKey());
        Assertions.assertTrue(Objects.requireNonNull(spec.getComment()).startsWith("How many offers a dwarf shows."), "the comment leads, the game appends default and range");
        Assertions.assertEquals(ModConfigSpec.RestartType.NONE, spec.restartType());
        Assertions.assertEquals(2, config.declared());
    }

    @Test
    void aStartupSettingNeedsAGameRestartAndIsAnErrorBeforeTheFilesAreRegistered() {
        ConfigKey<Boolean> extras = ConfigKey.flag(false, "Register the extra blocks.", "extras");
        ModConfig config = new ModConfig();
        config.declare(Config.File.STARTUP, extras);
        Assertions.assertThrows(IllegalStateException.class, () -> config.get(extras), "a module may not read it while it is built");
        ModConfigSpec startup = config.build(Dictionary.JOLCRAFT).get(Config.File.STARTUP);
        Assertions.assertThrows(IllegalStateException.class, () -> config.get(extras), "nor before the file is loaded");
        Assertions.assertEquals(ModConfigSpec.RestartType.GAME, ((ModConfigSpec.ValueSpec) startup.getSpec().get("extras")).restartType());
    }

    @Test
    void everyFileKindIsOneTheGameKeeps() {
        for (Config.File file : Config.File.values()) {
            Assertions.assertDoesNotThrow(() -> net.neoforged.fml.config.ModConfig.Type.valueOf(file.name()), file.name());
        }
    }

    @Test
    void aLoadedFileAnswersItsValuesAndTheFallbackAgainOnceUnloaded() {
        ModConfig config = new ModConfig();
        config.declare(Config.File.COMMON, offers);
        config.declare(Config.File.COMMON, debug);
        ModConfigSpec common = config.build(Dictionary.JOLCRAFT).get(Config.File.COMMON);
        ModContainer container = ModList.get().getModContainerById(JolCraft.MOD_ID).orElseThrow();
        net.neoforged.fml.config.ModConfig file = ConfigTracker.INSTANCE.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON, common, container, "jolcraft-test-common.toml");
        Assertions.assertFalse(common.isLoaded());
        Assertions.assertEquals(3, config.get(offers));

        ConfigTracker.acceptSyncedConfig(file, "[trade]\n\toffers = 7\n\tdebug = true\n".getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(common.isLoaded());
        Assertions.assertEquals(7, config.get(offers));
        Assertions.assertEquals(true, config.get(debug));

        common.acceptConfig(null);
        Assertions.assertFalse(common.isLoaded());
        Assertions.assertEquals(3, config.get(offers));
    }

    @Test
    void theIdIsTheFolderName() {
        Assertions.assertEquals(Dictionary.CONFIG, new ModConfig().id());
    }
}
