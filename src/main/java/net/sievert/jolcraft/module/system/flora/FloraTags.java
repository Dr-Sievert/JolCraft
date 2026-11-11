package net.sievert.jolcraft.module.system.flora;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/** The tags the flora system reads; its tag contributor and any data pack fill them. */
@Value
public final class FloraTags {

    /** The ground on which a plant ignores its light rule and grows faster: verdant soil and verdant farmland, and whatever a data pack adds. */
    public static final TagKey<Block> VERDANT = Ids.tag(Registries.BLOCK, Dictionary.JOLCRAFT, Dictionary.VERDANT);
    /** The ground the deepslate bulbs stand on: deepslate and verdant soil. */
    public static final TagKey<Block> SUPPORTS_DEEPSLATE_BULBS = Ids.tag(Registries.BLOCK, Dictionary.JOLCRAFT, Dictionary.SUPPORTS, Dictionary.DEEPSLATE, Dictionary.BULBS);
    /** The ground cyanella stands on: warped nylium and verdant soil. */
    public static final TagKey<Block> SUPPORTS_CYANELLA = Ids.tag(Registries.BLOCK, Dictionary.JOLCRAFT, Dictionary.SUPPORTS, Dictionary.CYANELLA);
    /** The ground the festerling and its crop stand on, besides the top of an upright log: verdant soil and what lets a mushroom ignore light. */
    public static final TagKey<Block> SUPPORTS_FESTERLING = Ids.tag(Registries.BLOCK, Dictionary.JOLCRAFT, Dictionary.SUPPORTS, Dictionary.FESTERLING);
    /** The ceilings under which bonemeal spreads bloodroot: what vegetation stands on. */
    public static final TagKey<Block> BLOODROOT_SPREADS_UNDER = Ids.tag(Registries.BLOCK, Dictionary.JOLCRAFT, Dictionary.BLOODROOT, Dictionary.SPREADS, Dictionary.UNDER);
    /** The four hops. */
    public static final TagKey<Item> HOPS = Ids.tag(Registries.ITEM, Dictionary.JOLCRAFT, Dictionary.HOPS);
    /** The four hops seeds. */
    public static final TagKey<Item> HOPS_SEEDS = Ids.tag(Registries.ITEM, Dictionary.JOLCRAFT, Dictionary.HOPS, Dictionary.SEEDS);

    /** The biomes the skybell patch generates in: mountains and hills. */
    public static final TagKey<Biome> GROWS_SKYBELL = Ids.tag(Registries.BIOME, Dictionary.JOLCRAFT, Dictionary.GROWS, Dictionary.SKYBELL);
    /** The biomes where bonemeal on grass sprouts a skybell: mountains, hills, forests and taigas. */
    public static final TagKey<Biome> SPROUTS_SKYBELL = Ids.tag(Registries.BIOME, Dictionary.JOLCRAFT, Dictionary.SPROUTS, Dictionary.SKYBELL);

    private FloraTags() {
    }
}
