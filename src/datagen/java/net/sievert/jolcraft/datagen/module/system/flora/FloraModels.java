package net.sievert.jolcraft.datagen.module.system.flora;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import net.sievert.jolcraft.datagen.Contributes;
import net.sievert.jolcraft.datagen.contribute.ModelContributor;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.system.flora.BulbsBlock;
import net.sievert.jolcraft.module.system.flora.FesterlingCropBlock;
import net.sievert.jolcraft.module.system.flora.FloraContent;
import net.sievert.jolcraft.module.system.flora.HopsBlock;
import net.sievert.jolcraft.module.system.flora.HopsVariety;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The models of the flora system. Verdant soil is a plain cube and verdant farmland the game's farmland template with the
 * soil for its dirt and its own texture for its top: one model for every moisture, since the block is always moist.
 * Barley is the game's crop template at its eight stages and its block the hay bale's pillar. Each hops block is a
 * dispatch over age and half onto the crop template, the lower half at its age and the upper half at the stage its age
 * shows; the bulbs' block state names the ten models made by hand. Seeds, hops and bulbs are flat items, barley held like a
 * tool, and a block item is its block's model. The wild plants are the game's plant with its pot, bloodroot a cross with a flat item, the festerling
 * crop a cross at each of its four stages, and each huge mushroom block the game's twelve-part mushroom with the mushroom's own inside texture.
 */
@Contributes(Dictionary.FLORA)
public final class FloraModels implements ModelContributor {

    @Override
    public void models(BlockModelGenerators blocks, ItemModelGenerators items) {
        Block soil = FloraContent.VERDANT_SOIL.get();
        blocks.createTrivialCube(soil);
        blocks.registerSimpleItemModel(soil, ModelLocationUtils.getModelLocation(soil));

        Block farmland = FloraContent.VERDANT_FARMLAND.get();
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.DIRT, TextureMapping.getBlockTexture(soil))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(farmland));
        Identifier model = ModelTemplates.FARMLAND.create(farmland, textures, blocks.modelOutput);
        blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(farmland, BlockModelGenerators.plainVariant(model)));
        blocks.registerSimpleItemModel(farmland, model);

        barley(blocks, items);
        for (HopsVariety variety : HopsVariety.values()) {
            hops(blocks, items, variety);
        }
        bulbs(blocks, items);
        wild(blocks, items);
    }

    private static void barley(BlockModelGenerators blocks, ItemModelGenerators items) {
        // the crop template also writes the flat model of the seeds, the crop's item
        blocks.createCropBlock(FloraContent.BARLEY_CROP.get(), CropBlock.AGE, 0, 1, 2, 3, 4, 5, 6, 7);
        items.generateFlatItem(FloraContent.BARLEY.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        Block hay = FloraContent.BARLEY_BLOCK.get();
        blocks.createRotatedPillarWithHorizontalVariant(hay, TexturedModel.COLUMN, TexturedModel.COLUMN_HORIZONTAL);
        blocks.registerSimpleItemModel(hay, ModelLocationUtils.getModelLocation(hay));
    }

    private static void hops(BlockModelGenerators blocks, ItemModelGenerators items, HopsVariety variety) {
        Block crop = FloraContent.crop(variety).get();
        Identifier[] bottoms = new Identifier[HopsBlock.MAX_AGE + 1];
        for (int age = 0; age < bottoms.length; age++) {
            bottoms[age] = stage(blocks, variety, Dictionary.BOTTOM, age);
        }
        Identifier[] tops = new Identifier[HopsBlock.upperStage(HopsBlock.MAX_AGE) + 1];
        for (int stage = 0; stage < tops.length; stage++) {
            tops[stage] = stage(blocks, variety, Dictionary.TOP, stage);
        }
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(crop).with(PropertyDispatch.initial(HopsBlock.AGE, HopsBlock.HALF)
                .generate((age, half) -> BlockModelGenerators.plainVariant(half == DoubleBlockHalf.LOWER ? bottoms[age] : tops[HopsBlock.upperStage(age)]))));
        items.generateFlatItem(FloraContent.seeds(variety).get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(FloraContent.hops(variety).get(), ModelTemplates.FLAT_ITEM);
    }

    /** The crop model of one stage of one half, over the texture the variety keeps from the old mod. */
    private static Identifier stage(BlockModelGenerators blocks, HopsVariety variety, String half, int stage) {
        String texture = ModString.underscored(variety.word(), Dictionary.CROP, half, Dictionary.STAGE + stage);
        Identifier model = Ids.path(Dictionary.JOLCRAFT, ModString.slashed(Dictionary.BLOCK, ModString.underscored(variety.word(), Dictionary.HOPS, Dictionary.CROP, half, Dictionary.STAGE + stage)));
        return ModelTemplates.CROP.create(model, TextureMapping.crop(new Material(Ids.path(Dictionary.JOLCRAFT, ModString.slashed(Dictionary.BLOCK, texture)))), blocks.modelOutput);
    }

    private static void bulbs(BlockModelGenerators blocks, ItemModelGenerators items) {
        String folder = ModString.underscored(Dictionary.DEEPSLATE, Dictionary.BULBS);
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(FloraContent.DEEPSLATE_BULBS_CROP.get()).with(PropertyDispatch.initial(BulbsBlock.AGE)
                .generate(age -> BlockModelGenerators.plainVariant(Ids.path(Dictionary.JOLCRAFT, ModString.slashed(Dictionary.BLOCK, folder,
                        ModString.underscored(folder, Dictionary.CROP, Dictionary.STAGE + age)))))));
        items.generateFlatItem(FloraContent.DEEPSLATE_BULBS.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void wild(BlockModelGenerators blocks, ItemModelGenerators items) {
        BlockModelGenerators.PlantType plain = BlockModelGenerators.PlantType.NOT_TINTED;
        blocks.createPlantWithDefaultItem(FloraContent.SKYBELL.get(), FloraContent.POTTED_SKYBELL.get(), plain);
        blocks.createPlantWithDefaultItem(FloraContent.CYANELLA.get(), FloraContent.POTTED_CYANELLA.get(), plain);
        blocks.createPlantWithDefaultItem(FloraContent.DUSKCAP.get(), FloraContent.POTTED_DUSKCAP.get(), plain);
        blocks.createPlantWithDefaultItem(FloraContent.FESTERLING.get(), FloraContent.POTTED_FESTERLING.get(), plain);

        blocks.createCrossBlock(FloraContent.BLOODROOT.get(), plain);
        items.generateFlatItem(FloraContent.BLOODROOT_ITEM.get(), ModelTemplates.FLAT_ITEM);

        festerlingCrop(blocks);

        Identifier duskcapInside = inside(blocks, Dictionary.DUSKCAP);
        mushroomBlock(blocks, FloraContent.DUSKCAP_BLOCK.get(), duskcapInside);
        mushroomBlock(blocks, FloraContent.DUSKCAP_STEM.get(), duskcapInside);
        Identifier festerlingInside = inside(blocks, Dictionary.FESTERLING);
        mushroomBlock(blocks, FloraContent.FESTERLING_BLOCK.get(), festerlingInside);
        mushroomBlock(blocks, FloraContent.FESTERLING_STEM.get(), festerlingInside);
    }

    /**
     * The crop's four stages, each the game's cross over its own texture, as the game's aged cross block writes them. That
     * generator also writes a flat model for the block's item, and the crop has none.
     */
    private static void festerlingCrop(BlockModelGenerators blocks) {
        Block crop = FloraContent.FESTERLING_CROP.get();
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(crop).with(PropertyDispatch.initial(FesterlingCropBlock.AGE).generate(age -> {
            String suffix = "_" + Dictionary.STAGE + age;
            TextureMapping textures = TextureMapping.cross(TextureMapping.getBlockTexture(crop, suffix));
            return BlockModelGenerators.plainVariant(ModelTemplates.CROSS.createWithSuffix(crop, suffix, textures, blocks.modelOutput));
        })));
    }

    /** The model of a mushroom's inside, the face a cap block shows where another block of the mushroom is: the mushroom's own texture, shared by its cap and its stem. */
    private static Identifier inside(BlockModelGenerators blocks, String mushroom) {
        String name = ModString.underscored(mushroom, Dictionary.BLOCK, Dictionary.INSIDE);
        Identifier model = Ids.path(Dictionary.JOLCRAFT, ModString.slashed(Dictionary.BLOCK, name));
        Material texture = new Material(Ids.path(Dictionary.JOLCRAFT, ModString.slashed(Dictionary.BLOCK, name)));
        return ModelTemplates.SINGLE_FACE.create(model, TextureMapping.defaultTexture(texture), blocks.modelOutput);
    }

    /**
     * The game's mushroom block, as it writes it: a multipart of a face of the block's skin where the face is set and the
     * inside where it is not, and an inventory cube for the item. The game names its own inside model; this names the mushroom's.
     */
    private static void mushroomBlock(BlockModelGenerators blocks, Block block, Identifier inside) {
        MultiVariant skin = BlockModelGenerators.plainVariant(ModelTemplates.SINGLE_FACE.create(block, TextureMapping.defaultTexture(block), blocks.modelOutput));
        MultiVariant skinless = BlockModelGenerators.plainVariant(inside);
        blocks.blockStateOutput.accept(MultiPartGenerator.multiPart(block)
                .with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), skin)
                .with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), skin.with(BlockModelGenerators.Y_ROT_90).with(BlockModelGenerators.UV_LOCK))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), skin.with(BlockModelGenerators.Y_ROT_180).with(BlockModelGenerators.UV_LOCK))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), skin.with(BlockModelGenerators.Y_ROT_270).with(BlockModelGenerators.UV_LOCK))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), skin.with(BlockModelGenerators.X_ROT_270).with(BlockModelGenerators.UV_LOCK))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, true), skin.with(BlockModelGenerators.X_ROT_90).with(BlockModelGenerators.UV_LOCK))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false), skinless)
                .with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, false), skinless.with(BlockModelGenerators.Y_ROT_90))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, false), skinless.with(BlockModelGenerators.Y_ROT_180))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, false), skinless.with(BlockModelGenerators.Y_ROT_270))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.UP, false), skinless.with(BlockModelGenerators.X_ROT_270))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, false), skinless.with(BlockModelGenerators.X_ROT_90)));
        blocks.registerSimpleItemModel(block, TexturedModel.CUBE.createWithSuffix(block, "_inventory", blocks.modelOutput));
    }
}
