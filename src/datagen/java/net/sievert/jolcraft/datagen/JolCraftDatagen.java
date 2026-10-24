package net.sievert.jolcraft.datagen;

import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.DataMapContributor;
import net.sievert.jolcraft.datagen.contribute.LanguageContributor;
import net.sievert.jolcraft.datagen.contribute.LootContributor;
import net.sievert.jolcraft.datagen.contribute.ModelContributor;
import net.sievert.jolcraft.datagen.contribute.RecipeContributor;
import net.sievert.jolcraft.datagen.contribute.RegistryContributor;
import net.sievert.jolcraft.datagen.contribute.SoundContributor;
import net.sievert.jolcraft.datagen.contribute.TagContributor;
import net.sievert.jolcraft.datagen.provider.BlockTags;
import net.sievert.jolcraft.datagen.provider.DataMaps;
import net.sievert.jolcraft.datagen.provider.ItemTags;
import net.sievert.jolcraft.datagen.provider.Loot;
import net.sievert.jolcraft.datagen.provider.Models;
import net.sievert.jolcraft.datagen.provider.Recipes;
import net.sievert.jolcraft.datagen.provider.Sounds;
import net.sievert.jolcraft.datagen.provider.Translations;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.log.ServerLog;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Dev-only mod that generates the mod's data (src/datagen/resources/META-INF/neoforge.mods.toml). runData loads
 * it beside the mod; the jar never does. One contributor per module and kind, found through the class scan
 * ({@link Contributes}), supplies content to one provider per kind; the providers walk the registries and fail
 * the run on anything a registered thing lacks, unless {@link Handmade} exempts it by name, and every exemption
 * is checked against the hand-made files.
 */
@Mod(JolCraftDatagen.MOD_ID)
public final class JolCraftDatagen {

    public static final String MOD_ID = JolCraft.MOD_ID + "_" + Dictionary.DATAGEN;

    private final Log log;

    public JolCraftDatagen(IEventBus modBus) {
        this.log = new ServerLog(LogUtils.getLogger()).tagged(Dictionary.DATAGEN);
        modBus.addListener(this::gather);
    }

    private void gather(GatherDataEvent.Client event) {
        Contributors contributors = Contributors.discover(MOD_ID, log);

        // First: the datapack-registry entries, under the mod's namespace. This replaces the event's lookup with
        // one that holds them, so every provider created after it can resolve a profession or a trade table.
        RegistrySetBuilder registries = new RegistrySetBuilder();
        contributors.of(RegistryContributor.class).forEach(contributor -> contributor.registries(registries));
        event.createDatapackRegistryObjects(registries, Set.of(JolCraft.MOD_ID));

        Handmade.verify(event.getResourceManager(PackType.CLIENT_RESOURCES), event.getResourceManager(PackType.SERVER_DATA));

        event.createProvider((output, lookup) -> new Recipes(output, lookup, contributors.of(RecipeContributor.class)));
        event.createProvider(output -> new Translations(output, contributors.of(LanguageContributor.class), log));
        event.createProvider(output -> new Models(output, contributors.of(ModelContributor.class)));
        event.createBlockAndItemTags(
                (output, lookup) -> new BlockTags(output, lookup, contributors.of(TagContributor.class)),
                (output, lookup, blockTags) -> new ItemTags(output, lookup, blockTags, contributors.of(TagContributor.class)));
        event.createProvider((output, lookup) -> new Loot(output, lookup, contributors.of(LootContributor.class)));
        event.createProvider(output -> new Sounds(output, contributors.of(SoundContributor.class), log));
        event.createProvider((output, lookup) -> new DataMaps(output, lookup, contributors.of(DataMapContributor.class)));
    }
}
