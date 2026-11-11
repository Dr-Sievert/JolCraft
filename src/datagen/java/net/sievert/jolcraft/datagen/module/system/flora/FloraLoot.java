package net.sievert.jolcraft.datagen.module.system.flora;

import java.util.stream.IntStream;

import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.LootContributor;
import net.sievert.jolcraft.datagen.provider.Loot;
import net.sievert.jolcraft.datagen.provider.LootModifiers;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.flora.BulbsBlock;
import net.sievert.jolcraft.module.system.flora.FesterlingCropBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.HopsBlock;
import net.sievert.jolcraft.module.system.flora.HopsVariety;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The drops of the flora system. Verdant soil drops itself; verdant farmland drops verdant soil, as vanilla's farmland
 * drops dirt. Barley drops as wheat and its block itself. A hops plant drops a seed, and when mature a second seed one
 * time in five and the hops, in the tall flowers' shape: a double plant rolls the table of the half that was broken
 * while the other half is still there, so every pool asks for exactly that. The bulbs drop one, and when mature one more
 * with the ore bonus of fortune and one more one time in five. Every crop is thinned by explosions as vanilla's are. Short
 * and tall grass also drop barley seeds one time in twenty, unless sheared, through one loot modifier that adds a table of
 * the mod's. The wild plants drop themselves and their pots the pot and the plant; a huge mushroom block drops as the game's own, its mushroom in
 * place of the game's, and a stem only with silk touch; the festerling crop drops one festerling at its last age and nothing before.
 */
@Contributes(Dictionary.FLORA)
public final class FloraLoot implements LootContributor {

    private static final float EXTRA_SEED = 0.2F;
    private static final float GRASS_SEEDS = 0.05F;

    private static final ResourceKey<LootTable> GRASS_SEEDS_TABLE = Ids.key(Registries.LOOT_TABLE, Dictionary.JOLCRAFT,
            Dictionary.BARLEY, Dictionary.SEEDS, Dictionary.FROM, Dictionary.GRASS);

    @Override
    public void blockLoot(Loot.Blocks loot) {
        loot.dropSelf(FloraContent.VERDANT_SOIL.get());
        loot.dropOther(FloraContent.VERDANT_FARMLAND.get(), FloraContent.VERDANT_SOIL.get());

        Block barley = FloraContent.BARLEY_CROP.get();
        loot.add(barley, loot.createCropDrops(barley, FloraContent.BARLEY.get(), FloraContent.BARLEY_SEEDS.get(),
                atAge(barley, CropBlock.AGE, CropBlock.MAX_AGE)));
        loot.dropSelf(FloraContent.BARLEY_BLOCK.get());

        for (HopsVariety variety : HopsVariety.values()) {
            hops(loot, FloraContent.crop(variety).get(), FloraContent.seeds(variety).get(), FloraContent.hops(variety).get());
        }
        bulbs(loot);
        wild(loot);
    }

    private static LootItemCondition.Builder atAge(Block block, IntegerProperty property, int age) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(property, age));
    }

    private static LootItemCondition.Builder half(Block block, DoubleBlockHalf half) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, half));
    }

    /** That the block at the offset is this block's other half, as the tall flowers ask it. */
    private static LootItemCondition.Builder other(HolderGetter<Block> blocks, Block block, DoubleBlockHalf half, int offset) {
        return LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, half))), new BlockPos(0, offset, 0));
    }

    /**
     * The tall flowers' condition for the hops: the lower half of a plant still single, or the lower half with its upper
     * half above, or the upper half with its lower half below. The half broken second finds the first one gone and drops nothing.
     */
    private static LootItemCondition.Builder whole(HolderGetter<Block> blocks, Block block) {
        LootItemCondition.Builder single = AnyOfCondition.anyOf(IntStream.range(0, HopsBlock.DOUBLE_AGE)
                .mapToObj(age -> atAge(block, HopsBlock.AGE, age)).toArray(LootItemCondition.Builder[]::new));
        return AnyOfCondition.anyOf(
                AllOfCondition.allOf(half(block, DoubleBlockHalf.LOWER), single),
                AllOfCondition.allOf(half(block, DoubleBlockHalf.LOWER), other(blocks, block, DoubleBlockHalf.UPPER, 1)),
                AllOfCondition.allOf(half(block, DoubleBlockHalf.UPPER), other(blocks, block, DoubleBlockHalf.LOWER, -1)));
    }

    private static void hops(Loot.Blocks loot, Block block, Item seeds, Item hops) {
        HolderGetter<Block> blocks = loot.registries().lookupOrThrow(Registries.BLOCK);
        LootItemCondition.Builder whole = whole(blocks, block);
        LootItemCondition.Builder mature = atAge(block, HopsBlock.AGE, HopsBlock.MAX_AGE);
        loot.add(block, loot.applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().when(whole).add(LootItem.lootTableItem(seeds)))
                .withPool(LootPool.lootPool().when(whole).when(mature).when(LootItemRandomChanceCondition.randomChance(EXTRA_SEED)).add(LootItem.lootTableItem(seeds)))
                .withPool(LootPool.lootPool().when(whole).when(mature).add(LootItem.lootTableItem(hops)))));
    }

    private static void bulbs(Loot.Blocks loot) {
        Block block = FloraContent.DEEPSLATE_BULBS_CROP.get();
        Item bulbs = FloraContent.DEEPSLATE_BULBS.get();
        LootItemCondition.Builder mature = atAge(block, BulbsBlock.AGE, BulbsBlock.MAX_AGE);
        HolderLookup.Provider registries = loot.registries();
        loot.add(block, loot.applyExplosionDecay(block, LootTable.lootTable()
                .withPool(LootPool.lootPool().add(LootItem.lootTableItem(bulbs)))
                .withPool(LootPool.lootPool().when(mature).add(LootItem.lootTableItem(bulbs)
                        .apply(ApplyBonusCount.addOreBonusCount(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE)))))
                .withPool(LootPool.lootPool().when(mature).when(LootItemRandomChanceCondition.randomChance(EXTRA_SEED)).add(LootItem.lootTableItem(bulbs)))));
    }

    private static void wild(Loot.Blocks loot) {
        loot.dropSelf(FloraContent.BLOODROOT.get());
        loot.dropSelf(FloraContent.SKYBELL.get());
        loot.dropSelf(FloraContent.CYANELLA.get());
        loot.dropSelf(FloraContent.DUSKCAP.get());
        loot.dropSelf(FloraContent.FESTERLING.get());
        loot.dropPottedContents(FloraContent.POTTED_SKYBELL.get());
        loot.dropPottedContents(FloraContent.POTTED_CYANELLA.get());
        loot.dropPottedContents(FloraContent.POTTED_DUSKCAP.get());
        loot.dropPottedContents(FloraContent.POTTED_FESTERLING.get());

        loot.add(FloraContent.DUSKCAP_BLOCK.get(), loot.createMushroomBlockDrop(FloraContent.DUSKCAP_BLOCK.get(), FloraContent.DUSKCAP_ITEM.get()));
        loot.dropWhenSilkTouch(FloraContent.DUSKCAP_STEM.get());
        loot.add(FloraContent.FESTERLING_BLOCK.get(), loot.createMushroomBlockDrop(FloraContent.FESTERLING_BLOCK.get(), FloraContent.FESTERLING_ITEM.get()));
        loot.dropWhenSilkTouch(FloraContent.FESTERLING_STEM.get());

        // the planted rotten flesh is never given back: only a grown crop drops anything
        Block crop = FloraContent.FESTERLING_CROP.get();
        loot.add(crop, LootTable.lootTable().withPool(LootPool.lootPool().when(atAge(crop, FesterlingCropBlock.AGE, FesterlingCropBlock.MAX_AGE))
                .setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(FloraContent.FESTERLING_ITEM.get()))));
    }

    @Override
    public void tables(Loot.Tables tables) {
        tables.add(GRASS_SEEDS_TABLE, LootContextParamSets.BLOCK, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(FloraContent.BARLEY_SEEDS.get()))));
    }

    @Override
    public void modifiers(LootModifiers modifiers) {
        LootItemCondition[] conditions = {
                AnyOfCondition.anyOf(LootTableIdCondition.builder(Blocks.SHORT_GRASS.getLootTable().orElseThrow().identifier()),
                        LootTableIdCondition.builder(Blocks.TALL_GRASS.getLootTable().orElseThrow().identifier())).build(),
                LootItemRandomChanceCondition.randomChance(GRASS_SEEDS).build(),
                MatchTool.toolMatches(ItemPredicate.Builder.item().of(modifiers.registries().lookupOrThrow(Registries.ITEM), Items.SHEARS)).invert().build()
        };
        modifiers.add(ModString.underscored(Dictionary.BARLEY, Dictionary.SEEDS, Dictionary.FROM, Dictionary.GRASS),
                new AddTableLootModifier(conditions, IGlobalLootModifier.DEFAULT_PRIORITY, GRASS_SEEDS_TABLE));
    }
}
