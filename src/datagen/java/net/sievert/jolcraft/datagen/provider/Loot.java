package net.sievert.jolcraft.datagen.provider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import net.sievert.jolcraft.datagen.Completeness;
import net.sievert.jolcraft.datagen.Handmade;
import net.sievert.jolcraft.datagen.contribute.LootContributor;

/**
 * Every module's loot tables through vanilla's provider. Vanilla's block sub-provider fails the run for any known
 * block without a table, and the known blocks are the mod's own minus the hand-written ones; entities likewise.
 * The helpers contributors need are opened here.
 */
public final class Loot extends LootTableProvider {

    public Loot(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<LootContributor> contributors) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(registries -> new Blocks(registries, contributors), LootContextParamSets.BLOCK),
                new SubProviderEntry(registries -> new Entities(registries, contributors), LootContextParamSets.ENTITY)), lookup);
    }

    /** Block loot, with vanilla's helpers opened to contributors. */
    public static final class Blocks extends BlockLootSubProvider {

        private final List<LootContributor> contributors;

        Blocks(HolderLookup.Provider registries, List<LootContributor> contributors) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
            this.contributors = contributors;
        }

        @Override
        protected void generate() {
            contributors.forEach(contributor -> contributor.blockLoot(this));
        }

        @Override
        public void dropSelf(Block block) {
            super.dropSelf(block);
        }

        @Override
        public void dropOther(Block block, ItemLike drop) {
            super.dropOther(block, drop);
        }

        @Override
        public void dropWhenSilkTouch(Block block) {
            super.dropWhenSilkTouch(block);
        }

        @Override
        public void add(Block block, LootTable.Builder builder) {
            super.add(block, builder);
        }

        @Override
        public void add(Block block, Function<Block, LootTable.Builder> builder) {
            super.add(block, builder);
        }

        @Override
        public LootTable.Builder createSingleItemTable(ItemLike drop, NumberProvider count) {
            return super.createSingleItemTable(drop, count);
        }

        @Override
        public LootTable.Builder createSilkTouchOnlyTable(ItemLike drop) {
            return super.createSilkTouchOnlyTable(drop);
        }

        @Override
        public LootTable.Builder createSlabItemTable(Block slab) {
            return super.createSlabItemTable(slab);
        }

        @Override
        public LootTable.Builder createDoorTable(Block block) {
            return super.createDoorTable(block);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Completeness.ours(BuiltInRegistries.BLOCK).filter(Completeness.notExempt(Handmade::blockLoot)).map(holder -> (Block) holder.value()).toList();
        }
    }

    /** Entity loot, with vanilla's helpers opened to contributors. */
    public static final class Entities extends EntityLootSubProvider {

        private final List<LootContributor> contributors;

        Entities(HolderLookup.Provider registries, List<LootContributor> contributors) {
            super(FeatureFlags.REGISTRY.allFlags(), registries);
            this.contributors = contributors;
        }

        @Override
        public void generate() {
            contributors.forEach(contributor -> contributor.entityLoot(this));
        }

        @Override
        public void add(EntityType<?> type, LootTable.Builder builder) {
            super.add(type, builder);
        }

        @Override
        protected Stream<EntityType<?>> getKnownEntityTypes() {
            return Completeness.ours(BuiltInRegistries.ENTITY_TYPE).filter(Completeness.notExempt(Handmade::entityLoot)).map(holder -> (EntityType<?>) holder.value());
        }
    }
}
