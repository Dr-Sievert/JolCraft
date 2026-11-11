package net.sievert.jolcraft.module.system.drop;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The replace modifier's codec, read as NeoForge reads a file, under registry-aware ops: what it round trips, its
 * default priority and what it refuses. Rolling needs a level, so what the modifier does to a drop is a scene, and so is
 * the registration, as the loot primitive's scenes check their own kinds.
 */
class ReplaceTableModifierTest {

    private static final String CONDITIONS = "\"conditions\":[{\"condition\":\"neoforge:loot_table_id\",\"loot_table_id\":\"minecraft:gameplay/fishing\"},{\"condition\":\"minecraft:survives_explosion\"}]";
    private static final String TABLE = "\"table\":\"jolcraft:gameplay/fishing\"";
    private static final ResourceKey<LootTable> FISHING = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("jolcraft", "gameplay/fishing"));

    private final HolderLookup.Provider registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

    private DataResult<ReplaceTableModifier> read(String json) {
        return ReplaceTableModifier.CODEC.codec().parse(ops, JsonParser.parseString(json));
    }

    private JsonElement write(ReplaceTableModifier modifier) {
        return ReplaceTableModifier.CODEC.codec().encodeStart(ops, modifier).getOrThrow();
    }

    @Test
    void theKindIsReplaceTableInTheModsNamespace() {
        Assertions.assertEquals(Identifier.fromNamespaceAndPath("jolcraft", "replace_table"), DropKeys.REPLACE_TABLE.id());
    }

    @Test
    void conditionsAPriorityAndATableRoundTrip() {
        String json = "{" + CONDITIONS + ",\"priority\":1100," + TABLE + "}";
        ReplaceTableModifier read = read(json).getOrThrow();
        Assertions.assertEquals(1100, read.priority());
        Assertions.assertEquals(FISHING, read.table());
        Assertions.assertEquals(JsonParser.parseString(json), write(read), "both conditions, the priority and the table are written back");
    }

    @Test
    void withoutAPriorityItTakesNeoForgesDefaultAndWritesNone() {
        ReplaceTableModifier read = read("{" + CONDITIONS + "," + TABLE + "}").getOrThrow();
        Assertions.assertEquals(IGlobalLootModifier.DEFAULT_PRIORITY, read.priority());
        Assertions.assertFalse(write(read).getAsJsonObject().has("priority"), "the default is not written");
        Assertions.assertEquals(IGlobalLootModifier.DEFAULT_PRIORITY, new ReplaceTableModifier(new LootItemCondition[0], FISHING).priority(), "the shorter constructor gives the default too");
        Assertions.assertEquals(7, new ReplaceTableModifier(new LootItemCondition[0], 7, FISHING).priority());
    }

    @Test
    void aFileWithoutATableOrWithoutConditionsIsRefused() {
        Assertions.assertTrue(read("{" + CONDITIONS + "," + TABLE + "}").isSuccess(), "both together are read");
        Assertions.assertTrue(read("{" + CONDITIONS + "}").isError(), "no table");
        Assertions.assertTrue(read("{" + TABLE + "}").isError(), "no conditions");
    }
}
