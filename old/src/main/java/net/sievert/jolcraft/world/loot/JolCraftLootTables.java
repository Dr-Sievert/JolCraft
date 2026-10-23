package net.sievert.jolcraft.world.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.data.id.loot.JolCraftLootTableIds;
import net.sievert.jolcraft.data.language.JolCraftDictionary;
import net.sievert.jolcraft.util.JolCraftStrings;
import net.sievert.jolcraft.world.item.component.custom.crate.CrateTheme;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores IDs for JolCraft built-in loot tables, i.e. loot tables which are not based directly on a block or entity ID.
 * Mirrors vanilla's BuiltInLootTables pattern.
 */
public final class JolCraftLootTables {

    private JolCraftLootTables() {}

    private static final Set<ResourceKey<LootTable>> LOCATIONS = new HashSet<>();
    private static final Set<ResourceKey<LootTable>> IMMUTABLE_LOCATIONS = Collections.unmodifiableSet(LOCATIONS);

    /* ---------------------------------------------------------------------
     * Chests
     * ------------------------------------------------------------------ */

    public static final class Chests {


        public static final ResourceKey<LootTable> MISC_SALVAGE = chest(JolCraftLootTableIds.MISC_SALVAGE);
        public static final ResourceKey<LootTable> DEEPSLATE_SALVAGE = chest(JolCraftLootTableIds.DEEPSLATE_SALVAGE);
        public static final ResourceKey<LootTable> MITHRIL_SALVAGE = chest(JolCraftLootTableIds.MITHRIL_SALVAGE);
        public static final ResourceKey<LootTable> SALVAGE = chest(JolCraftLootTableIds.SALVAGE);
        public static final ResourceKey<LootTable> SMITHING_SALVAGE = chest(JolCraftLootTableIds.SMITHING_SALVAGE);

        public static final ResourceKey<LootTable> DWARVEN_TOMES = chest(JolCraftLootTableIds.DWARVEN_TOMES);

        public static final ResourceKey<LootTable> VANILLA_GEMS = chest(JolCraftLootTableIds.VANILLA_GEMS);
        public static final ResourceKey<LootTable> VANILLA_METAL = chest(JolCraftLootTableIds.VANILLA_METAL);

        public static final ResourceKey<LootTable> DWARVEN_METAL = chest(JolCraftLootTableIds.DWARVEN_METAL);

        public static final ResourceKey<LootTable> DEEPSLATE_ARMOR = chest(JolCraftLootTableIds.DEEPSLATE_ARMOR);
        public static final ResourceKey<LootTable> DEEPSLATE_GEAR = chest(JolCraftLootTableIds.DEEPSLATE_GEAR);
        public static final ResourceKey<LootTable> MITHRIL_ARMOR = chest(JolCraftLootTableIds.MITHRIL_ARMOR);
        public static final ResourceKey<LootTable> MITHRIL_GEAR = chest(JolCraftLootTableIds.MITHRIL_GEAR);

        public static final ResourceKey<LootTable> UNCUT_GEMS = chest(JolCraftLootTableIds.UNCUT_GEMS);

        public static final ResourceKey<LootTable> GEODES = chest(JolCraftLootTableIds.GEODES);

        public static final ResourceKey<LootTable> SUPPLIES = chest(JolCraftLootTableIds.SUPPLIES);
    }

    /* ---------------------------------------------------------------------
     * Strongbox
     * ------------------------------------------------------------------ */

    public static final class Strongbox {

        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_FORGE = strongbox(JolCraftLootTableIds.DWARVEN_FORTRESS_FORGE);
        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_VAULT = strongbox(JolCraftLootTableIds.DWARVEN_FORTRESS_VAULT);
        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_GARDEN = strongbox(JolCraftLootTableIds.DWARVEN_FORTRESS_GARDEN);
        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_ARCHIVES = strongbox(JolCraftLootTableIds.DWARVEN_FORTRESS_ARCHIVES);
        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_CATACOMBS = strongbox(JolCraftLootTableIds.DWARVEN_FORTRESS_CATACOMBS);
    }

    /* ---------------------------------------------------------------------
     * Crates
     * ------------------------------------------------------------------ */

    public static final class Crates {

        public static final ResourceKey<LootTable> SUPPLY_CRATE = crate(JolCraftLootTableIds.SUPPLY_CRATE);

        public static final ResourceKey<LootTable> ALCHEMY_SUPPLIES = crate(
                CrateTheme.ALCHEMY,
                JolCraftLootTableIds.ALCHEMY_SUPPLIES
        );

        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_EXCAVATION = crate(
                CrateTheme.ARCHAEOLOGY,
                JolCraftLootTableIds.DWARVEN_FORTRESS_EXCAVATION
        );

        public static final ResourceKey<LootTable> ARTISAN_SUPPLIES = crate(
                CrateTheme.ARTISAN,
                JolCraftLootTableIds.ARTISAN_SUPPLIES
        );

        public static final ResourceKey<LootTable> FARMING_SUPPLIES = crate(
                CrateTheme.GARDEN,
                JolCraftLootTableIds.FARMING_SUPPLIES
        );

        public static final ResourceKey<LootTable> MINING_CACHE = crate(
                CrateTheme.MINING,
                JolCraftLootTableIds.MINING_CACHE
        );

        public static final ResourceKey<LootTable> FISHING_LOOT = crate(
                CrateTheme.OCEAN,
                JolCraftLootTableIds.FISHING_LOOT
        );

        public static final ResourceKey<LootTable> BLACKSMITH_SUPPLIES = crate(
                CrateTheme.REINFORCED,
                JolCraftLootTableIds.BLACKSMITH_SUPPLIES
        );

        public static final ResourceKey<LootTable> MONSTER_SLAYER_LOOT = crate(
                CrateTheme.SLAYER,
                JolCraftLootTableIds.MONSTER_SLAYER_LOOT
        );

        public static final ResourceKey<LootTable> VAULT_LOOT = crate(
                CrateTheme.TREASURE,
                JolCraftLootTableIds.VAULT_LOOT
        );
    }

    /* ---------------------------------------------------------------------
     * Archaeology
     * ------------------------------------------------------------------ */

    public static final class Archaeology {

        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_COMMON =
                archaeology(JolCraftLootTableIds.DWARVEN_FORTRESS_COMMON);

        public static final ResourceKey<LootTable> DWARVEN_FORTRESS_RARE =
                archaeology(JolCraftLootTableIds.DWARVEN_FORTRESS_RARE);
    }

    /* ---------------------------------------------------------------------
     * Fishing
     * ------------------------------------------------------------------ */

    public static final class Fishing {

        public static final ResourceKey<LootTable> FISHING =
                fishing(JolCraftLootTableIds.FISHING);

        public static final ResourceKey<LootTable> JUNK =
                fishing(JolCraftLootTableIds.JUNK);

        public static final ResourceKey<LootTable> TREASURE =
                fishing(JolCraftLootTableIds.TREASURE);
    }

    /* ---------------------------------------------------------------------
     * Registration helpers
     * ------------------------------------------------------------------ */

    private static ResourceKey<LootTable> register(String path) {
        ResourceKey<LootTable> key = ResourceKey.create(
                Registries.LOOT_TABLE,
                JolCraft.location(path)
        );

        if (LOCATIONS.add(key)) {
            return key;
        }

        throw new IllegalArgumentException(key.location() + " is already a registered JolCraft loot table");
    }

    /**
     * Registers only the foldered key. The bare path is never a table of its own, so it must
     * not end up in {@link #LOCATIONS}.
     */
    private static ResourceKey<LootTable> inFolder(String folder, String path) {
        return register(JolCraftStrings.slashed(folder, path));
    }

    private static ResourceKey<LootTable> inject(String path){
        return inFolder(JolCraftDictionary.INJECT, path);
    }

    private static ResourceKey<LootTable> chest(String path){
        return inFolder(JolCraftDictionary.CHEST, path);
    }

    private static ResourceKey<LootTable> strongbox(String path){
        return inFolder(JolCraftDictionary.STRONGBOX, path);
    }

    private static ResourceKey<LootTable> crate(String path){
        return inFolder(JolCraftDictionary.CRATE, path);
    }

    private static ResourceKey<LootTable> crate(
            @NotNull CrateTheme theme,
            @NotNull String id
    ) {
        return register(
                JolCraftStrings.slashed(
                        JolCraftDictionary.CRATE,
                        theme.getId(),
                        id
                )
        );
    }

    private static ResourceKey<LootTable> archaeology(String path){
        return inFolder(JolCraftDictionary.ARCHAEOLOGY, path);
    }

    private static ResourceKey<LootTable> fishing(String path){
        return inFolder(JolCraftDictionary.FISHING, path);
    }

    public static Set<ResourceKey<LootTable>> all() {
        return IMMUTABLE_LOCATIONS;
    }
}
