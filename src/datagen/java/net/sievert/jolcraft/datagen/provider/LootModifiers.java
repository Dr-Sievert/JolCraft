package net.sievert.jolcraft.datagen.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.contribute.LootContributor;
import net.sievert.jolcraft.module.system.drop.ReplaceTableModifier;
import org.jetbrains.annotations.NotNull;

/**
 * The mod's global loot modifiers from every module's contributor: one file per modifier under the mod's {@code loot_modifiers}, every one of which the game reads; a name used twice is refused.
 * A modifier that adds or replaces with a table must name one written in this run by the loot provider, which runs before this one, or one in the game's data.
 */
public final class LootModifiers extends GlobalLootModifierProvider {

    private final List<LootContributor> contributors;
    private final Loot loot;
    private final ResourceManager data;
    private final Set<String> names = new HashSet<>();
    /** The table each adding or replacing modifier names, by the modifier's name. */
    private final Map<String, ResourceKey<LootTable>> tables = new LinkedHashMap<>();

    public LootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<LootContributor> contributors, Loot loot, ResourceManager data) {
        super(output, lookup, JolCraft.MOD_ID);
        this.contributors = contributors;
        this.loot = loot;
        this.data = data;
    }

    @Override
    public <T extends IGlobalLootModifier> void add(@NotNull String modifier, T instance, @NotNull List<ICondition> conditions) {
        if (!names.add(modifier)) {
            throw new IllegalStateException("the loot modifier " + modifier + " was added twice; every modifier has a name of its own");
        }
        switch (instance) {
            case AddTableLootModifier add -> tables.put(modifier, add.table());
            case ReplaceTableModifier replace -> tables.put(modifier, replace.table());
            default -> {
            }
        }
        super.add(modifier, instance, conditions);
    }

    @Override
    protected void start() {
        contributors.forEach(contributor -> contributor.modifiers(this));
        List<String> missing = new ArrayList<>();
        tables.forEach((modifier, table) -> {
            Identifier id = table.identifier();
            Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(), "loot_table/" + id.getPath() + ".json");
            if (!loot.written().contains(table) && data.getResource(file).isEmpty()) {
                missing.add(modifier + " names " + id);
            }
        });
        Completeness.require("loot tables of modifiers (neither written in this run nor in the game's data)", missing);
    }

    /** The registries at generation time, for modifiers whose conditions or contents look things up. */
    public HolderLookup.Provider registries() {
        return registries;
    }
}
