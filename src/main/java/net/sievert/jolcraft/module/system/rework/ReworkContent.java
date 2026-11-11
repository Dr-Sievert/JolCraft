package net.sievert.jolcraft.module.system.rework;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.sievert.jolcraft.stereotype.Content;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The rework kind's serializer under the mod's namespace, in the game's serializer registry: the recipe's type is the
 * game's crafting type, so the serializer is all the kind registers. The stream codec is the data codec under the
 * registries, used when a mod asks for the crafting recipes to be sent. The root registers it once on the mod bus.
 */
@Content
public final class ReworkContent {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Dictionary.JOLCRAFT);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReworkRecipe>> REWORK = SERIALIZERS.register(Dictionary.REWORK,
            () -> new RecipeSerializer<>(ReworkRecipe.CODEC, ByteBufCodecs.fromCodecWithRegistries(ReworkRecipe.CODEC.codec())));

    private ReworkContent() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
