package net.sievert.jolcraft.datagen.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.Handmade;
import net.sievert.jolcraft.datagen.contribute.LootContributor;
import org.jetbrains.annotations.NotNull;

/**
 * Every module's loot tables through vanilla's provider. Vanilla's block sub-provider fails the run for any known
 * block without a table, and the known blocks are the mod's own minus the hand-written ones; entities likewise.
 * The tables that are neither a block's nor an entity type's are added with their context through {@link Tables}:
 * the provider holds one sub-provider per context the game registers and each writes the tables added under its
 * own. The helpers contributors need are opened here, and the keys of every table written in this run are kept
 * for the loot modifier provider, which runs after this one.
 */
public final class Loot extends LootTableProvider {

    private static final List<ContextKeySet> CONTEXTS = contexts();

    private final Run run;

    public Loot(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<LootContributor> contributors) {
        this(output, lookup, new Run(contributors));
    }

    private Loot(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, Run run) {
        super(output, Set.of(), entries(run), lookup);
        this.run = run;
    }

    /** The keys of every table this provider wrote, blocks, entities and added tables; complete once the provider has run. */
    public Set<ResourceKey<LootTable>> written() {
        return Collections.unmodifiableSet(run.written);
    }

    private static List<SubProviderEntry> entries(Run run) {
        List<SubProviderEntry> entries = new ArrayList<>();
        entries.add(new SubProviderEntry(registries -> run.recording(new Blocks(registries, run.contributors)), LootContextParamSets.BLOCK));
        entries.add(new SubProviderEntry(registries -> run.recording(new Entities(registries, run.contributors)), LootContextParamSets.ENTITY));
        for (ContextKeySet context : CONTEXTS) {
            entries.add(new SubProviderEntry(registries -> run.recording(output -> run.tables(registries).emit(context, output)), context));
        }
        return entries;
    }

    /** The game's registered context sets, which its class holds as public constants; the registry itself is private. */
    private static List<ContextKeySet> contexts() {
        List<ContextKeySet> found = new ArrayList<>();
        for (Field field : LootContextParamSets.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == ContextKeySet.class) {
                try {
                    found.add((ContextKeySet) field.get(null));
                } catch (IllegalAccessException unreadable) {
                    throw new IllegalStateException("the loot context " + field.getName() + " could not be read", unreadable);
                }
            }
        }
        return List.copyOf(found);
    }

    /** What one data run shares between the sub-providers: the tables the contributors added, once, and the keys written. */
    private static final class Run {

        private final List<LootContributor> contributors;
        private final Set<ResourceKey<LootTable>> written = new HashSet<>();
        private Tables tables;

        Run(List<LootContributor> contributors) {
            this.contributors = contributors;
        }

        /** The contributors' added tables, collected on first ask with the registries of the run. */
        synchronized Tables tables(HolderLookup.Provider registries) {
            if (tables == null) {
                tables = new Tables(registries);
                contributors.forEach(contributor -> contributor.tables(tables));
            }
            return tables;
        }

        LootTableSubProvider recording(LootTableSubProvider inner) {
            return output -> inner.generate((key, builder) -> {
                written.add(key);
                output.accept(key, builder);
            });
        }
    }

    /** Loot tables of any context, such as a chest, a fishing table or a table a loot modifier adds. */
    public static final class Tables {

        private final HolderLookup.Provider registries;
        private final Map<ContextKeySet, Map<ResourceKey<LootTable>, LootTable.Builder>> added = new LinkedHashMap<>();
        private final Set<ResourceKey<LootTable>> keys = new HashSet<>();

        Tables(HolderLookup.Provider registries) {
            this.registries = registries;
        }

        /** The registries at generation time, for functions and conditions that look things up, such as looting or fortune. */
        public HolderLookup.Provider registries() {
            return registries;
        }

        /** Adds a table of the mod's namespace under the context its parameters are drawn from: one of the game's own sets. */
        public void add(ResourceKey<LootTable> key, ContextKeySet context, LootTable.Builder builder) {
            if (!key.identifier().getNamespace().equals(JolCraft.MOD_ID)) {
                throw new IllegalArgumentException("the loot table " + key.identifier() + " is outside the mod's namespace");
            }
            if (!CONTEXTS.contains(context)) {
                throw new IllegalArgumentException("the loot table " + key.identifier() + " names a context that is not one of the game's registered sets: " + context);
            }
            if (!keys.add(key)) {
                throw new IllegalStateException("the loot table " + key.identifier() + " was added twice");
            }
            added.computeIfAbsent(context, _ -> new LinkedHashMap<>()).put(key, builder);
        }

        void emit(ContextKeySet context, BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            added.getOrDefault(context, Map.of()).forEach(output);
        }
    }

    /** Block loot, with vanilla's helpers opened to contributors. */
    public static final class Blocks extends BlockLootSubProvider {

        private final List<LootContributor> contributors;

        Blocks(HolderLookup.Provider registries, List<LootContributor> contributors) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
            this.contributors = contributors;
        }

        /** The registries at generation time, for functions and conditions that look things up, such as looting or fortune. */
        public HolderLookup.Provider registries() {
            return registries;
        }

        @Override
        protected void generate() {
            contributors.forEach(contributor -> contributor.blockLoot(this));
        }

        @Override
        public void dropSelf(@NotNull Block block) {
            super.dropSelf(block);
        }

        @Override
        public void dropOther(@NotNull Block block, @NotNull ItemLike drop) {
            super.dropOther(block, drop);
        }

        @Override
        public void dropWhenSilkTouch(@NotNull Block block) {
            super.dropWhenSilkTouch(block);
        }

        @Override
        public void dropPottedContents(@NotNull Block potted) {
            super.dropPottedContents(potted);
        }

        @Override
        public LootTable.@NotNull Builder createMushroomBlockDrop(@NotNull Block original, @NotNull ItemLike drop) {
            return super.createMushroomBlockDrop(original, drop);
        }

        @Override
        public void add(@NotNull Block block, LootTable.@NotNull Builder builder) {
            super.add(block, builder);
        }

        @Override
        public void add(@NotNull Block block, @NotNull Function<Block, LootTable.Builder> builder) {
            super.add(block, builder);
        }

        @Override
        public LootTable.@NotNull Builder createSingleItemTable(@NotNull ItemLike drop, @NotNull NumberProvider count) {
            return super.createSingleItemTable(drop, count);
        }

        @Override
        public LootTable.@NotNull Builder createSilkTouchOnlyTable(@NotNull ItemLike drop) {
            return super.createSilkTouchOnlyTable(drop);
        }

        @Override
        public LootTable.@NotNull Builder createSlabItemTable(@NotNull Block slab) {
            return super.createSlabItemTable(slab);
        }

        @Override
        public LootTable.@NotNull Builder createDoorTable(@NotNull Block block) {
            return super.createDoorTable(block);
        }

        @Override
        public <T extends FunctionUserBuilder<T>> @NotNull T applyExplosionDecay(@NotNull ItemLike type, @NotNull FunctionUserBuilder<T> builder) {
            return super.applyExplosionDecay(type, builder);
        }

        @Override
        public LootTable.@NotNull Builder createCropDrops(@NotNull Block original, @NotNull Item cropDrop, @NotNull Item seedDrop, LootItemCondition.@NotNull Builder isMaxAge) {
            return super.createCropDrops(original, cropDrop, seedDrop, isMaxAge);
        }

        @Override
        protected @NotNull Iterable<Block> getKnownBlocks() {
            return Completeness.ours(BuiltInRegistries.BLOCK).filter(Completeness.notExempt(Handmade::blockLoot)).map(Holder.Reference::value).toList();
        }
    }

    /** Entity loot, with vanilla's helpers opened to contributors. */
    public static final class Entities extends EntityLootSubProvider {

        private final List<LootContributor> contributors;

        Entities(HolderLookup.Provider registries, List<LootContributor> contributors) {
            super(FeatureFlags.REGISTRY.allFlags(), registries);
            this.contributors = contributors;
        }

        /** The registries at generation time, for functions and conditions that look things up, such as looting or fortune. */
        public HolderLookup.Provider registries() {
            return registries;
        }

        @Override
        public void generate() {
            contributors.forEach(contributor -> contributor.entityLoot(this));
        }

        @Override
        public void add(@NotNull EntityType<?> type, LootTable.@NotNull Builder builder) {
            super.add(type, builder);
        }

        @Override
        protected @NotNull Stream<EntityType<?>> getKnownEntityTypes() {
            return Completeness.ours(BuiltInRegistries.ENTITY_TYPE).filter(Completeness.notExempt(Handmade::entityLoot)).map(Holder.Reference::value);
        }
    }
}
