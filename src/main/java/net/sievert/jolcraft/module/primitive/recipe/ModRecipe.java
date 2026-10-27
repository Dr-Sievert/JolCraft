package net.sievert.jolcraft.module.primitive.recipe;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.util.text.Dictionary;

/** {@link Recipe} over the game's recipe registries: the keys are kept until the game registers, then each gets its type, serializer and category. */
@Implementation
public final class ModRecipe implements Recipe {

    private final Map<Identifier, RecipeKey<?, ?>> declared = new LinkedHashMap<>();
    private boolean registered;
    private boolean synced;

    @Override
    public void declare(RecipeKey<?, ?> key) {
        if (registered) {
            throw new IllegalStateException(key + " was declared after the recipe kinds were registered");
        }
        if (key.id().getNamespace().equals(Dictionary.MINECRAFT)) {
            throw new IllegalArgumentException(key + " uses the game's namespace; a recipe kind's id is the mod's own");
        }
        if (declared.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException(key + " was declared twice");
        }
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the recipe kinds were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterEvent.class, this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.RECIPE_TYPE, helper -> declared.values().forEach(key -> helper.register(key.id(), key.buildType())));
        event.register(Registries.RECIPE_SERIALIZER, helper -> declared.values().forEach(key -> helper.register(key.id(), key.buildSerializer())));
        event.register(Registries.RECIPE_BOOK_CATEGORY, helper -> declared.values().forEach(key -> helper.register(key.id(), new RecipeBookCategory())));
    }

    @Override
    public void sync(IEventBus gameBus) {
        if (synced) {
            throw new IllegalStateException("the recipe kinds were attached to the game bus twice");
        }
        synced = true;
        gameBus.addListener(OnDatapackSyncEvent.class, event -> event.sendRecipes(declared.values().stream().<RecipeType<?>>map(RecipeKey::type).toList()));
    }

    /** How many kinds are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
