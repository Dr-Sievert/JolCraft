package net.sievert.jolcraft.datagen.provider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.contribute.LootContributor;
import org.jetbrains.annotations.NotNull;

/** The mod's global loot modifiers from every module's contributor: one file per modifier under the mod's {@code loot_modifiers}, every one of which the game reads; a name used twice is refused. */
public final class LootModifiers extends GlobalLootModifierProvider {

    private final List<LootContributor> contributors;
    private final Set<String> names = new HashSet<>();

    public LootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, List<LootContributor> contributors) {
        super(output, lookup, JolCraft.MOD_ID);
        this.contributors = contributors;
    }

    @Override
    public <T extends IGlobalLootModifier> void add(@NotNull String modifier, T instance, @NotNull List<ICondition> conditions) {
        if (!names.add(modifier)) {
            throw new IllegalStateException("the loot modifier " + modifier + " was added twice; every modifier has a name of its own");
        }
        super.add(modifier, instance, conditions);
    }

    @Override
    protected void start() {
        contributors.forEach(contributor -> contributor.modifiers(this));
    }

    /** The registries at generation time, for modifiers whose conditions or contents look things up. */
    public HolderLookup.Provider registries() {
        return registries;
    }
}
