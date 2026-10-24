package net.sievert.jolcraft.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * What is made by hand and therefore exempt from a completeness check, by name and per registry: a Blockbench
 * block state or item definition, a hand-written loot table, each a file in src/main/resources. An exemption is
 * visible here and nowhere else, and {@link #verify} fails the run when one names nothing registered or has no
 * file behind it. Names and sound definitions have no exemption: those files are always generated whole.
 */
public final class Handmade {

    /** Blocks whose {@code blockstates/<path>.json} is hand-made. */
    private static final Set<Identifier> BLOCK_MODELS = Set.of();
    /** Items whose {@code items/<path>.json} is hand-made. */
    private static final Set<Identifier> ITEM_MODELS = Set.of();
    /** Blocks whose {@code loot_table/blocks/<path>.json} is hand-written. */
    private static final Set<Identifier> BLOCK_LOOT = Set.of();
    /** Entity types whose {@code loot_table/entities/<path>.json} is hand-written. */
    private static final Set<Identifier> ENTITY_LOOT = Set.of();

    private Handmade() {
    }

    public static boolean blockModel(Identifier id) {
        return BLOCK_MODELS.contains(id);
    }

    public static boolean itemModel(Identifier id) {
        return ITEM_MODELS.contains(id);
    }

    public static boolean blockLoot(Identifier id) {
        return BLOCK_LOOT.contains(id);
    }

    public static boolean entityLoot(Identifier id) {
        return ENTITY_LOOT.contains(id);
    }

    /** Every exemption names a registered thing and a file that exists among the hand-made resources, or the run fails. */
    public static void verify(ResourceManager assets, ResourceManager data) {
        List<String> problems = new ArrayList<>();
        check(problems, BLOCK_MODELS, BuiltInRegistries.BLOCK, assets, "blockstates", "block model");
        check(problems, ITEM_MODELS, BuiltInRegistries.ITEM, assets, "items", "item model");
        check(problems, BLOCK_LOOT, BuiltInRegistries.BLOCK, data, "loot_table/blocks", "block loot table");
        check(problems, ENTITY_LOOT, BuiltInRegistries.ENTITY_TYPE, data, "loot_table/entities", "entity loot table");
        if (!problems.isEmpty()) {
            throw new IllegalStateException(problems.size() + " stale Handmade exemptions: " + String.join("; ", problems));
        }
    }

    private static void check(List<String> problems, Set<Identifier> ids, Registry<?> registry, ResourceManager manager, String folder, String what) {
        for (Identifier id : ids) {
            if (!registry.containsKey(id)) {
                problems.add(what + " exemption " + id + " names nothing registered");
            }
            Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(), folder + "/" + id.getPath() + ".json");
            if (manager.getResource(file).isEmpty()) {
                problems.add(what + " exemption " + id + " has no hand-made file " + file);
            }
        }
    }
}
