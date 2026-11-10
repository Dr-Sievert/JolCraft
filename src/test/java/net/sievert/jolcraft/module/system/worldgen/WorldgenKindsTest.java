package net.sievert.jolcraft.module.system.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The kinds through the game's dispatch codecs, under the ids the content registered them with. */
class WorldgenKindsTest {

    private static JsonElement encode(StructureProcessor processor) {
        return StructureProcessorType.SINGLE_CODEC.encodeStart(JsonOps.INSTANCE, processor).getOrThrow();
    }

    private static JsonObject processor(String type) {
        JsonObject json = new JsonObject();
        json.addProperty("processor_type", type);
        return json;
    }

    @Test
    void addLootTableRoundTripsUnderItsId() {
        JsonObject json = encode(new AddLootTableProcessor(Blocks.CHEST, BuiltInLootTables.SIMPLE_DUNGEON)).getAsJsonObject();
        Assertions.assertEquals("jolcraft:add_loot_table", json.get("processor_type").getAsString());
        Assertions.assertEquals("minecraft:chest", json.get("block").getAsString());
        Assertions.assertEquals("minecraft:chests/simple_dungeon", json.get("loot_table").getAsString());
        StructureProcessor parsed = StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        Assertions.assertInstanceOf(AddLootTableProcessor.class, parsed);
        Assertions.assertEquals(json, encode(parsed));
    }

    @Test
    void anUnknownBlockIsRefused() {
        JsonObject json = processor("jolcraft:add_loot_table");
        json.addProperty("block", "jolcraft:no_such_block");
        json.addProperty("loot_table", "minecraft:chests/simple_dungeon");
        Assertions.assertTrue(StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, json).isError());
    }

    @Test
    void theChanceKindsRoundTripUnderTheirIds() {
        for (StructureProcessor kind : new StructureProcessor[] {new RandomCobwebProcessor(0.25F), new RandomCaveInProcessor(0.5F)}) {
            JsonObject json = encode(kind).getAsJsonObject();
            StructureProcessor parsed = StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            Assertions.assertEquals(kind.getClass(), parsed.getClass());
            Assertions.assertEquals(json, encode(parsed));
        }
        Assertions.assertEquals("jolcraft:random_cobweb", encode(new RandomCobwebProcessor(0.25F)).getAsJsonObject().get("processor_type").getAsString());
        Assertions.assertEquals("jolcraft:random_cave_in", encode(new RandomCaveInProcessor(0.5F)).getAsJsonObject().get("processor_type").getAsString());
        Assertions.assertEquals(0.25F, encode(new RandomCobwebProcessor(0.25F)).getAsJsonObject().get("chance").getAsFloat());
    }

    @Test
    void aChanceOutsideZeroToOneIsRefused() {
        for (String type : new String[] {"jolcraft:random_cobweb", "jolcraft:random_cave_in"}) {
            for (float chance : new float[] {-0.1F, 1.1F}) {
                JsonObject json = processor(type);
                json.addProperty("chance", chance);
                Assertions.assertTrue(StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, json).isError(), type + " " + chance);
            }
            JsonObject bounds = processor(type);
            bounds.addProperty("chance", 0.0F);
            Assertions.assertTrue(StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, bounds).isSuccess(), type + " at 0");
            bounds.addProperty("chance", 1.0F);
            Assertions.assertTrue(StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, bounds).isSuccess(), type + " at 1");
        }
    }

    @Test
    void hangingHasNoFieldsAndRoundTrips() {
        JsonObject json = encode(new HangingProcessor()).getAsJsonObject();
        Assertions.assertEquals("jolcraft:hanging", json.get("processor_type").getAsString());
        Assertions.assertEquals(1, json.size());
        Assertions.assertInstanceOf(HangingProcessor.class, StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void theHeightFilterRoundTripsAndEitherBoundMayBeAbsent() {
        JsonObject json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, new HeightFilter(10, 20)).getOrThrow().getAsJsonObject();
        Assertions.assertEquals("jolcraft:height_filter", json.get("type").getAsString());
        Assertions.assertEquals(10, json.get("min_inclusive").getAsInt());
        Assertions.assertEquals(20, json.get("max_inclusive").getAsInt());
        PlacementModifier parsed = PlacementModifier.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        Assertions.assertInstanceOf(HeightFilter.class, parsed);
        Assertions.assertEquals(json, PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, parsed).getOrThrow());

        JsonObject bare = new JsonObject();
        bare.addProperty("type", "jolcraft:height_filter");
        bare.addProperty("max_inclusive", 64);
        Assertions.assertTrue(PlacementModifier.CODEC.parse(JsonOps.INSTANCE, bare).isSuccess());
        bare.remove("max_inclusive");
        Assertions.assertTrue(PlacementModifier.CODEC.parse(JsonOps.INSTANCE, bare).isSuccess());
    }
}
