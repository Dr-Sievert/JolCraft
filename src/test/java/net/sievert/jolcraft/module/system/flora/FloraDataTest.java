package net.sievert.jolcraft.module.system.flora;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The planting data: a plant is a block by name, and a name that is no block is refused. And the bonemeal data: a list of rows, a feature and an optional biome tag each, joined across packs. */
class FloraDataTest {

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    @Test
    void aPlantsEntryRoundTripsAndIsWrittenAsTheBlocksName() {
        FloraData.Plants plants = new FloraData.Plants(FloraContent.FESTERLING_CROP.get());
        JsonElement written = FloraData.Plants.CODEC.encodeStart(JsonOps.INSTANCE, plants).getOrThrow();
        Assertions.assertEquals("jolcraft:festerling_crop", written.getAsJsonObject().get("block").getAsString());
        Assertions.assertEquals(plants, FloraData.Plants.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void anUnknownBlockIsRefused() {
        Assertions.assertTrue(FloraData.Plants.CODEC.parse(JsonOps.INSTANCE, json("{\"block\":\"jolcraft:no_such_block\"}")).isError());
    }

    @Test
    void anEntryWithoutABlockIsRefused() {
        Assertions.assertTrue(FloraData.Plants.CODEC.parse(JsonOps.INSTANCE, json("{}")).isError());
    }

    @Test
    void theTypeIsTheModsOwnOnItems() {
        Assertions.assertEquals("jolcraft:plants", FloraData.PLANTS.id().toString());
    }

    private static final ResourceKey<PlacedFeature> SKYBELL = ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse("jolcraft:skybell_bonemeal"));
    private static final ResourceKey<PlacedFeature> CYANELLA = ResourceKey.create(Registries.PLACED_FEATURE, Identifier.parse("jolcraft:cyanella_bonemeal"));
    private static final TagKey<Biome> SPROUTS_SKYBELL = TagKey.create(Registries.BIOME, Identifier.parse("jolcraft:sprouts_skybell"));

    @Test
    void aSproutWithABiomeTagRoundTripsAndIsWrittenAsAHashedTag() {
        FloraData.Sprout sprout = new FloraData.Sprout(SKYBELL, Optional.of(SPROUTS_SKYBELL));
        JsonElement written = FloraData.Sprout.CODEC.encodeStart(JsonOps.INSTANCE, sprout).getOrThrow();
        Assertions.assertEquals("jolcraft:skybell_bonemeal", written.getAsJsonObject().get("feature").getAsString());
        Assertions.assertEquals("#jolcraft:sprouts_skybell", written.getAsJsonObject().get("biome").getAsString());
        Assertions.assertEquals(sprout, FloraData.Sprout.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void aSproutWithoutABiomeTagRoundTripsAndWritesNoBiome() {
        FloraData.Sprout sprout = new FloraData.Sprout(CYANELLA, Optional.empty());
        JsonElement written = FloraData.Sprout.CODEC.encodeStart(JsonOps.INSTANCE, sprout).getOrThrow();
        Assertions.assertFalse(written.getAsJsonObject().has("biome"));
        Assertions.assertEquals(sprout, FloraData.Sprout.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
        Assertions.assertEquals(sprout, FloraData.Sprout.CODEC.parse(JsonOps.INSTANCE, json("{\"feature\":\"jolcraft:cyanella_bonemeal\"}")).getOrThrow());
    }

    @Test
    void aSproutWithoutAFeatureIsRefused() {
        Assertions.assertTrue(FloraData.Sprout.CODEC.parse(JsonOps.INSTANCE, json("{}")).isError());
        Assertions.assertTrue(FloraData.Sprout.CODEC.parse(JsonOps.INSTANCE, json("{\"biome\":\"#jolcraft:sprouts_skybell\"}")).isError());
    }

    @Test
    void aBonemealEntryIsAListOfRows() {
        List<FloraData.Sprout> rows = List.of(new FloraData.Sprout(SKYBELL, Optional.of(SPROUTS_SKYBELL)), new FloraData.Sprout(CYANELLA, Optional.empty()));
        JsonElement written = FloraData.BONEMEAL.codec().encodeStart(JsonOps.INSTANCE, rows).getOrThrow();
        Assertions.assertTrue(written.isJsonArray());
        Assertions.assertEquals(2, written.getAsJsonArray().size());
        Assertions.assertEquals(rows, FloraData.BONEMEAL.codec().parse(JsonOps.INSTANCE, written).getOrThrow());
        Assertions.assertTrue(FloraData.BONEMEAL.codec().parse(JsonOps.INSTANCE, json("{\"feature\":\"jolcraft:skybell_bonemeal\"}")).isError(), "a bare row is no list");
    }

    @Test
    void theTypeJoinsTheRowsOfASecondPackToTheFirstsInOrder() {
        List<FloraData.Sprout> first = List.of(new FloraData.Sprout(SKYBELL, Optional.of(SPROUTS_SKYBELL)));
        List<FloraData.Sprout> second = List.of(new FloraData.Sprout(CYANELLA, Optional.empty()));
        List<FloraData.Sprout> merged = FloraData.BONEMEAL.merger().merge(null, null, first, null, second);
        Assertions.assertEquals(List.of(first.getFirst(), second.getFirst()), merged);
        Assertions.assertEquals(1, first.size(), "the first pack's list is not changed");
    }

    @Test
    void theBonemealTypeIsTheModsOwnOnBlocks() {
        Assertions.assertEquals("jolcraft:bonemeal", FloraData.BONEMEAL.id().toString());
        Assertions.assertEquals(Registries.BLOCK, FloraData.BONEMEAL.registryKey());
        Assertions.assertNull(FloraData.BONEMEAL.networkCodec(), "not synced");
    }
}
