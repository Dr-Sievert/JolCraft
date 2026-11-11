package net.sievert.jolcraft.module.system.flora;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.datamaps.AdvancedDataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapValueMerger;
import net.neoforged.neoforge.registries.datamaps.DataMapValueRemover;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The data the flora system reads, as data map types. {@link FloraContent} registers them.
 */
@Value
public final class FloraData {

    /** The items that plant a block when used on one, and the block each plants. Synced to clients, who predict the click from it. */
    public static final DataMapType<Item, Plants> PLANTS = DataMapType.builder(
            Ids.of(Dictionary.JOLCRAFT, Dictionary.PLANTS), Registries.ITEM, Plants.CODEC).synced(Plants.CODEC, true).build();

    /**
     * What else bonemeal grows on a block, as rows of {@link Sprout}. Read on the server only, so not synced. An entry is a
     * list, and the lists of several data packs are joined, so a pack adds rows without replacing the mod's.
     */
    public static final AdvancedDataMapType<Block, List<Sprout>, DataMapValueRemover.Default<List<Sprout>, Block>> BONEMEAL = AdvancedDataMapType.builder(
            Ids.of(Dictionary.JOLCRAFT, Dictionary.BONEMEAL), Registries.BLOCK, Sprout.CODEC.listOf()).merger(DataMapValueMerger.listMerger()).build();

    private FloraData() {
    }

    /** The block an item plants. */
    public record Plants(Block block) {

        public static final Codec<Plants> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf(Dictionary.BLOCK).forGetter(Plants::block)
        ).apply(instance, Plants::new));
    }

    /**
     * One thing bonemeal grows above a block: a placed feature, which brings its own chance, height and spread, and the
     * biomes it grows in when a tag is given (everywhere when not). The feature must carry no biome filter, since it is
     * placed outside world generation, where that filter throws.
     */
    public record Sprout(ResourceKey<PlacedFeature> feature, Optional<TagKey<Biome>> biome) {

        public static final Codec<Sprout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.PLACED_FEATURE).fieldOf(Dictionary.FEATURE).forGetter(Sprout::feature),
                TagKey.hashedCodec(Registries.BIOME).optionalFieldOf(Dictionary.BIOME).forGetter(Sprout::biome)
        ).apply(instance, Sprout::new));
    }
}
