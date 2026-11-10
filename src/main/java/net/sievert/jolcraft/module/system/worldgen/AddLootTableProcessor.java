package net.sievert.jolcraft.module.system.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * Gives every block of one kind in a structure piece a loot table, written as the game's {@code append_loot} writes
 * it: the state is kept, so facing, waterlogging and chest type survive, and the tag gets {@code LootTable} and a
 * {@code LootTableSeed} from the settings' random at the block, which placement replaces for a container. Any other
 * block passes. Per block, so it works under a capped processor too.
 */
@Actor
public final class AddLootTableProcessor extends StructureProcessor {

    public static final MapCodec<AddLootTableProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf(Dictionary.BLOCK).forGetter(processor -> processor.block),
            LootTable.KEY_CODEC.fieldOf(ModString.underscored(Dictionary.LOOT, Dictionary.TABLE)).forGetter(processor -> processor.lootTable)
    ).apply(instance, AddLootTableProcessor::new));

    private final Block block;
    private final ResourceKey<LootTable> lootTable;

    public AddLootTableProcessor(Block block, ResourceKey<LootTable> lootTable) {
        this.block = block;
        this.lootTable = lootTable;
    }

    @SuppressWarnings("deprecation")
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos targetPosition, BlockPos referencePos,
            StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo processed, StructurePlaceSettings settings) {
        if (!processed.state().is(block)) {
            return processed;
        }
        CompoundTag tag = processed.nbt() == null ? new CompoundTag() : processed.nbt().copy();
        tag.store(RandomizableContainer.LOOT_TABLE_TAG, LootTable.KEY_CODEC, lootTable);
        tag.putLong(RandomizableContainer.LOOT_TABLE_SEED_TAG, settings.getRandom(processed.pos()).nextLong());
        return new StructureTemplate.StructureBlockInfo(processed.pos(), processed.state(), tag);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return WorldgenContent.ADD_LOOT_TABLE.get();
    }
}
