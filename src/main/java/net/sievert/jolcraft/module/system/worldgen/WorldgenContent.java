package net.sievert.jolcraft.module.system.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.sievert.jolcraft.stereotype.Content;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * The structure processor and placement modifier kinds of the worldgen system, under the mod's namespace. The root
 * registers them once on the mod bus; the game finds a kind by its holder's single type instance.
 */
@Content
public final class WorldgenContent {

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Dictionary.JOLCRAFT);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Dictionary.JOLCRAFT);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<AddLootTableProcessor>> ADD_LOOT_TABLE =
            PROCESSORS.register(ModString.underscored(Dictionary.ADD, Dictionary.LOOT, Dictionary.TABLE), () -> () -> AddLootTableProcessor.CODEC);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<RandomCobwebProcessor>> RANDOM_COBWEB =
            PROCESSORS.register(ModString.underscored(Dictionary.RANDOM, Dictionary.COBWEB), () -> () -> RandomCobwebProcessor.CODEC);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<RandomCaveInProcessor>> RANDOM_CAVE_IN =
            PROCESSORS.register(ModString.underscored(Dictionary.RANDOM, Dictionary.CAVE, Dictionary.IN), () -> () -> RandomCaveInProcessor.CODEC);
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<HangingProcessor>> HANGING =
            PROCESSORS.register(Dictionary.HANGING, () -> () -> HangingProcessor.CODEC);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<HeightFilter>> HEIGHT_FILTER =
            PLACEMENTS.register(ModString.underscored(Dictionary.HEIGHT, Dictionary.FILTER), () -> () -> HeightFilter.CODEC);

    private WorldgenContent() {
    }

    public static void register(IEventBus modBus) {
        PROCESSORS.register(modBus);
        PLACEMENTS.register(modBus);
    }
}
