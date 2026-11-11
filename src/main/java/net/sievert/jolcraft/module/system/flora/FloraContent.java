package net.sievert.jolcraft.module.system.flora;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.module.primitive.language.LanguageKey;
import net.sievert.jolcraft.stereotype.Content;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/** The blocks, items and feature types of the flora system, under the mod's namespace. The root registers them once on the mod bus. */
@Content
public final class FloraContent {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Dictionary.JOLCRAFT);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dictionary.JOLCRAFT);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Dictionary.JOLCRAFT);

    public static final DeferredBlock<VerdantSoilBlock> VERDANT_SOIL = BLOCKS.registerBlock(ModString.underscored(Dictionary.VERDANT, Dictionary.SOIL),
            VerdantSoilBlock::new, properties -> properties.mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.5F).sound(SoundType.MUD));
    public static final DeferredBlock<VerdantFarmlandBlock> VERDANT_FARMLAND = BLOCKS.registerBlock(ModString.underscored(Dictionary.VERDANT, Dictionary.FARMLAND),
            VerdantFarmlandBlock::new, properties -> properties.mapColor(MapColor.COLOR_LIGHT_GREEN).randomTicks().strength(0.6F).sound(SoundType.MUD)
                    .isViewBlocking((_, _, _) -> true).isSuffocating((_, _, _) -> true));

    public static final DeferredBlock<BarleyBlock> BARLEY_CROP = BLOCKS.registerBlock(ModString.underscored(Dictionary.BARLEY, Dictionary.CROP),
            BarleyBlock::new, properties -> properties.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<HayBlock> BARLEY_BLOCK = BLOCKS.registerBlock(ModString.underscored(Dictionary.BARLEY, Dictionary.BLOCK),
            HayBlock::new, properties -> properties.mapColor(MapColor.COLOR_YELLOW).strength(0.5F).sound(SoundType.GRASS)
                    .instrument(NoteBlockInstrument.BANJO).ignitedByLava());

    public static final DeferredBlock<HopsBlock> ASGARNIAN_HOPS_CROP = registerHopsCrop(HopsVariety.ASGARNIAN);
    public static final DeferredBlock<HopsBlock> DUSKHOLD_HOPS_CROP = registerHopsCrop(HopsVariety.DUSKHOLD);
    public static final DeferredBlock<HopsBlock> KRANDONIAN_HOPS_CROP = registerHopsCrop(HopsVariety.KRANDONIAN);
    public static final DeferredBlock<HopsBlock> YANILLIAN_HOPS_CROP = registerHopsCrop(HopsVariety.YANILLIAN);

    public static final DeferredBlock<BulbsBlock> DEEPSLATE_BULBS_CROP = BLOCKS.registerBlock(ModString.underscored(Dictionary.DEEPSLATE, Dictionary.BULBS, Dictionary.CROP),
            BulbsBlock::new, properties -> properties.mapColor(MapColor.DEEPSLATE).noCollision().randomTicks().strength(3.5F, 6.0F).sound(SoundType.DEEPSLATE)
                    .pushReaction(PushReaction.DESTROY).requiresCorrectToolForDrops());

    public static final DeferredBlock<BloodrootBlock> BLOODROOT = BLOCKS.registerBlock(Dictionary.BLOODROOT,
            BloodrootBlock::new, properties -> properties.mapColor(MapColor.DIRT).replaceable().noCollision().instabreak().sound(SoundType.HANGING_ROOTS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<BloomBlock> SKYBELL = registerBloom(Dictionary.SKYBELL, BlockTags.SUPPORTS_VEGETATION);
    public static final DeferredBlock<BloomBlock> CYANELLA = registerBloom(Dictionary.CYANELLA, FloraTags.SUPPORTS_CYANELLA);

    public static final DeferredBlock<DuskcapBlock> DUSKCAP = BLOCKS.registerBlock(Dictionary.DUSKCAP,
            DuskcapBlock::new, properties -> properties.mapColor(MapColor.COLOR_MAGENTA).noCollision().randomTicks().instabreak().sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<HugeMushroomBlock> DUSKCAP_BLOCK = registerHuge(ModString.underscored(Dictionary.DUSKCAP, Dictionary.BLOCK), MapColor.COLOR_MAGENTA);
    public static final DeferredBlock<HugeMushroomBlock> DUSKCAP_STEM = registerHuge(ModString.underscored(Dictionary.DUSKCAP, Dictionary.STEM), MapColor.COLOR_PURPLE);

    public static final DeferredBlock<FesterlingBlock> FESTERLING = BLOCKS.registerBlock(Dictionary.FESTERLING,
            FesterlingBlock::new, properties -> properties.mapColor(MapColor.TERRACOTTA_LIGHT_GREEN).noCollision().randomTicks().instabreak().sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<HugeMushroomBlock> FESTERLING_BLOCK = registerHuge(ModString.underscored(Dictionary.FESTERLING, Dictionary.BLOCK), MapColor.TERRACOTTA_LIGHT_GREEN);
    public static final DeferredBlock<HugeMushroomBlock> FESTERLING_STEM = registerHuge(ModString.underscored(Dictionary.FESTERLING, Dictionary.STEM), MapColor.GOLD);
    public static final DeferredBlock<FesterlingCropBlock> FESTERLING_CROP = BLOCKS.registerBlock(ModString.underscored(Dictionary.FESTERLING, Dictionary.CROP),
            FesterlingCropBlock::new, properties -> properties.mapColor(MapColor.TERRACOTTA_LIGHT_GREEN).noCollision().randomTicks().instabreak().sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<FlowerPotBlock> POTTED_SKYBELL = registerPot(SKYBELL);
    public static final DeferredBlock<FlowerPotBlock> POTTED_CYANELLA = registerPot(CYANELLA);
    public static final DeferredBlock<FlowerPotBlock> POTTED_DUSKCAP = registerPot(DUSKCAP);
    public static final DeferredBlock<FlowerPotBlock> POTTED_FESTERLING = registerPot(FESTERLING);

    public static final DeferredItem<BlockItem> VERDANT_SOIL_ITEM = ITEMS.registerSimpleBlockItem(VERDANT_SOIL);
    public static final DeferredItem<BlockItem> VERDANT_FARMLAND_ITEM = ITEMS.registerSimpleBlockItem(VERDANT_FARMLAND);

    public static final DeferredItem<BlockItem> BARLEY_SEEDS = registerSeeds(ModString.underscored(Dictionary.BARLEY, Dictionary.SEEDS), BARLEY_CROP, null);
    public static final DeferredItem<Item> BARLEY = ITEMS.registerSimpleItem(Dictionary.BARLEY);
    public static final DeferredItem<BlockItem> BARLEY_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(BARLEY_BLOCK);

    public static final DeferredItem<BlockItem> ASGARNIAN_SEEDS = registerHopsSeeds(HopsVariety.ASGARNIAN, ASGARNIAN_HOPS_CROP);
    public static final DeferredItem<BlockItem> DUSKHOLD_SEEDS = registerHopsSeeds(HopsVariety.DUSKHOLD, DUSKHOLD_HOPS_CROP);
    public static final DeferredItem<BlockItem> KRANDONIAN_SEEDS = registerHopsSeeds(HopsVariety.KRANDONIAN, KRANDONIAN_HOPS_CROP);
    public static final DeferredItem<BlockItem> YANILLIAN_SEEDS = registerHopsSeeds(HopsVariety.YANILLIAN, YANILLIAN_HOPS_CROP);

    public static final DeferredItem<Item> ASGARNIAN_HOPS = ITEMS.registerSimpleItem(ModString.underscored(Dictionary.ASGARNIAN, Dictionary.HOPS));
    public static final DeferredItem<Item> DUSKHOLD_HOPS = ITEMS.registerSimpleItem(ModString.underscored(Dictionary.DUSKHOLD, Dictionary.HOPS));
    public static final DeferredItem<Item> KRANDONIAN_HOPS = ITEMS.registerSimpleItem(ModString.underscored(Dictionary.KRANDONIAN, Dictionary.HOPS));
    public static final DeferredItem<Item> YANILLIAN_HOPS = ITEMS.registerSimpleItem(ModString.underscored(Dictionary.YANILLIAN, Dictionary.HOPS));

    /** The seeds of the bulbs: the bulbs themselves, which are planted. */
    public static final DeferredItem<BlockItem> DEEPSLATE_BULBS = registerSeeds(ModString.underscored(Dictionary.DEEPSLATE, Dictionary.BULBS), DEEPSLATE_BULBS_CROP,
            FloraHints.DEEPSLATE_BULBS);

    public static final DeferredItem<BlockItem> BLOODROOT_ITEM = ITEMS.registerSimpleBlockItem(BLOODROOT);
    public static final DeferredItem<BlockItem> SKYBELL_ITEM = ITEMS.registerSimpleBlockItem(SKYBELL);
    public static final DeferredItem<BlockItem> CYANELLA_ITEM = ITEMS.registerSimpleBlockItem(CYANELLA);
    public static final DeferredItem<BlockItem> DUSKCAP_ITEM = ITEMS.registerSimpleBlockItem(DUSKCAP);
    public static final DeferredItem<BlockItem> DUSKCAP_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(DUSKCAP_BLOCK);
    public static final DeferredItem<BlockItem> DUSKCAP_STEM_ITEM = ITEMS.registerSimpleBlockItem(DUSKCAP_STEM);
    public static final DeferredItem<BlockItem> FESTERLING_ITEM = ITEMS.registerSimpleBlockItem(FESTERLING);
    public static final DeferredItem<BlockItem> FESTERLING_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(FESTERLING_BLOCK);
    public static final DeferredItem<BlockItem> FESTERLING_STEM_ITEM = ITEMS.registerSimpleBlockItem(FESTERLING_STEM);

    public static final DeferredHolder<Feature<?>, HugeDuskcapFeature> HUGE_DUSKCAP = FEATURES.register(ModString.underscored(Dictionary.HUGE, Dictionary.DUSKCAP),
            () -> new HugeDuskcapFeature(HugeMushroomFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HugeFesterlingFeature> HUGE_FESTERLING = FEATURES.register(ModString.underscored(Dictionary.HUGE, Dictionary.FESTERLING),
            () -> new HugeFesterlingFeature(HugeMushroomFeatureConfiguration.CODEC));

    private FloraContent() {
    }

    /**
     * Registers the blocks, items and feature types, and the data map types of {@link FloraData}. At common setup the four
     * pots are added to the game's empty pot, which the game fills a plant at a time through its map of full pots.
     */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> {
            event.register(FloraData.PLANTS);
            event.register(FloraData.BONEMEAL);
        });
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
            FlowerPotBlock emptyPot = (FlowerPotBlock) Blocks.FLOWER_POT;
            emptyPot.addPlant(SKYBELL.getId(), POTTED_SKYBELL);
            emptyPot.addPlant(CYANELLA.getId(), POTTED_CYANELLA);
            emptyPot.addPlant(DUSKCAP.getId(), POTTED_DUSKCAP);
            emptyPot.addPlant(FESTERLING.getId(), POTTED_FESTERLING);
        }));
    }

    /** The crop block of a variety. */
    public static DeferredBlock<HopsBlock> crop(HopsVariety variety) {
        return switch (variety) {
            case ASGARNIAN -> ASGARNIAN_HOPS_CROP;
            case DUSKHOLD -> DUSKHOLD_HOPS_CROP;
            case KRANDONIAN -> KRANDONIAN_HOPS_CROP;
            case YANILLIAN -> YANILLIAN_HOPS_CROP;
        };
    }

    /** The seeds of a variety. */
    public static DeferredItem<BlockItem> seeds(HopsVariety variety) {
        return switch (variety) {
            case ASGARNIAN -> ASGARNIAN_SEEDS;
            case DUSKHOLD -> DUSKHOLD_SEEDS;
            case KRANDONIAN -> KRANDONIAN_SEEDS;
            case YANILLIAN -> YANILLIAN_SEEDS;
        };
    }

    /** The hops of a variety. */
    public static DeferredItem<Item> hops(HopsVariety variety) {
        return switch (variety) {
            case ASGARNIAN -> ASGARNIAN_HOPS;
            case DUSKHOLD -> DUSKHOLD_HOPS;
            case KRANDONIAN -> KRANDONIAN_HOPS;
            case YANILLIAN -> YANILLIAN_HOPS;
        };
    }

    /** A small flower that stands on the blocks of {@code ground}. */
    private static DeferredBlock<BloomBlock> registerBloom(String name, TagKey<Block> ground) {
        return BLOCKS.registerBlock(name, properties -> new BloomBlock(ground, properties),
                properties -> properties.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS)
                        .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    }

    /** A block of a huge mushroom, the cap or the stem, of the given colour. */
    private static DeferredBlock<HugeMushroomBlock> registerHuge(String name, MapColor color) {
        return BLOCKS.registerBlock(name, HugeMushroomBlock::new,
                properties -> properties.mapColor(color).instrument(NoteBlockInstrument.BASS).strength(0.2F).sound(SoundType.WOOD).ignitedByLava());
    }

    /** The pot holding {@code plant}, with the game's own flower-pot properties. */
    private static DeferredBlock<FlowerPotBlock> registerPot(DeferredBlock<? extends Block> plant) {
        return BLOCKS.registerBlock(ModString.underscored(Dictionary.POTTED, plant.getId().getPath()),
                properties -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, plant, properties),
                properties -> properties.instabreak().noOcclusion().pushReaction(PushReaction.DESTROY));
    }

    private static DeferredBlock<HopsBlock> registerHopsCrop(HopsVariety variety) {
        return BLOCKS.registerBlock(ModString.underscored(variety.word(), Dictionary.HOPS, Dictionary.CROP), HopsBlock::new,
                properties -> properties.mapColor(variety.color()).noCollision().randomTicks().instabreak().sound(SoundType.CROP)
                        .pushReaction(PushReaction.DESTROY));
    }

    private static DeferredItem<BlockItem> registerHopsSeeds(HopsVariety variety, DeferredBlock<HopsBlock> crop) {
        return registerSeeds(ModString.underscored(variety.word(), Dictionary.SEEDS), crop, FloraHints.HOPS_SEEDS);
    }

    /** A block item that plants {@code crop}, named as an item, with the hint as its default lore when it has one. */
    private static DeferredItem<BlockItem> registerSeeds(String name, DeferredBlock<? extends Block> crop, @Nullable LanguageKey hint) {
        return ITEMS.registerItem(name, properties -> {
            properties.useItemDescriptionPrefix();
            if (hint != null) {
                properties.component(DataComponents.LORE, new ItemLore(List.of(hintLine(hint))));
            }
            return new BlockItem(crop.get(), properties);
        });
    }

    /** One line of lore: gray and not italic, set on the line itself since lore lends its own purple italics to what a line leaves unset. */
    private static Component hintLine(LanguageKey hint) {
        return hint.text().withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false));
    }
}
