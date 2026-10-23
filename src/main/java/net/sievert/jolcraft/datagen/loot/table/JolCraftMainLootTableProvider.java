package net.sievert.jolcraft.datagen.loot.table;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.datagen.base.JolCraftDataDomain;
import net.sievert.jolcraft.datagen.base.JolCraftMainDataProvider;
import net.sievert.jolcraft.datagen.loot.table.subprovider.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class JolCraftMainLootTableProvider
        extends LootTableProvider
        implements JolCraftMainDataProvider<JolCraftMainLootTableProvider> {

    public JolCraftMainLootTableProvider(
            @NotNull PackOutput output,
            @NotNull CompletableFuture<HolderLookup.Provider> registries
    ) {
        super(
                output,
                Set.of(),
                List.of(
                        new SubProviderEntry(JolCraftArchaeologyLootTableProvider::new, LootContextParamSets.ARCHAEOLOGY),
                        new SubProviderEntry(JolCraftBlockLootTableProvider::new, LootContextParamSets.BLOCK),
                        new SubProviderEntry(JolCraftChestLootTableProvider::new, LootContextParamSets.CHEST),
                        new SubProviderEntry(JolCraftCrateLootTableProvider::new, LootContextParamSets.CHEST),
                        new SubProviderEntry(JolCraftEntityLootTableProvider::new, LootContextParamSets.ENTITY),
                        new SubProviderEntry(JolCraftStrongboxLootTableProvider::new, LootContextParamSets.CHEST),
                        new SubProviderEntry(JolCraftFishingLootTableProvider::new, LootContextParamSets.FISHING)
                ),
                registries
        );
    }

    @Override
    public @NotNull JolCraftDataDomain domain() {
        return JolCraftDataDomain.LOOT;
    }

    @Override
    public @NotNull String id() {
        return domain().getId();
    }

    @Override
    protected void validate(
            @NotNull WritableRegistry<LootTable> registry,
            @NotNull ValidationContext context,
            ProblemReporter.@NotNull Collector problems
    ) {
        // Vanilla's pass checks parameter sets and nested references against the generated
        // registry. Tables outside this mod (the vanilla fishing tables in the crate loot) are
        // not in it and resolve when datapacks load, so only their "unknown table" reports are
        // dropped; everything else still fails the run.
        super.validate(
                registry,
                new ValidationContext(
                        new ExternalTableFilter(problems),
                        LootContextParamSets.ALL_PARAMS,
                        context.resolver()
                ),
                problems
        );
    }

    private record ExternalTableFilter(
            ProblemReporter delegate
    ) implements ProblemReporter {

        /** Message prefix used by {@code NestedLootTable.validate} for an unresolved reference. */
        private static final String UNKNOWN_TABLE = "Unknown loot table called ";

        @Override
        public @NotNull ProblemReporter forChild(@NotNull String name) {
            return new ExternalTableFilter(delegate.forChild(name));
        }

        @Override
        public void report(@NotNull String message) {
            if (message.startsWith(UNKNOWN_TABLE)
                    && !message.startsWith(UNKNOWN_TABLE + JolCraft.MOD_ID + ":")) {
                return;
            }

            delegate.report(message);
        }
    }
}
