package net.sievert.jolcraft.config.custom.brewing;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.data.language.JolCraftDictionary;
import net.sievert.jolcraft.util.JolCraftStrings;
import net.sievert.jolcraft.util.log.JolCraftLogTags;
import net.sievert.jolcraft.util.log.JolCraftLogs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class CorruptionEffectsConfigManager
        extends SimplePreparableReloadListener<CorruptionEffectsConfig> {

    public static final ResourceLocation FILE =
            JolCraft.location(
                    JolCraftStrings.slashed(
                            JolCraftDictionary.CONFIG,
                            JolCraftDictionary.BREWING,
                            JolCraftStrings.underscored(
                                    JolCraftDictionary.CORRUPTION,
                                    JolCraftStrings.plural(JolCraftDictionary.EFFECT)
                            )
                    ) + ".json"
            );

    public static final CorruptionEffectsConfigManager INSTANCE =
            new CorruptionEffectsConfigManager();

    private volatile CorruptionEffectsConfig config = new CorruptionEffectsConfig(List.of());

    private CorruptionEffectsConfigManager() {}

    public List<CorruptionEffectsConfig.WeightedEffect> entries() {
        return config.effects();
    }

    // Datapack reload listener, so on a dedicated server the client never runs apply(), yet
    // JeiCorruptionHelper reads entries() client-side. See ClientboundConfigSyncPacket.

    public @NotNull CorruptionEffectsConfig snapshot() {
        return config;
    }

    public void applySynced(@NotNull CorruptionEffectsConfig synced) {
        config = synced;
    }

    public void reset() {
        config = new CorruptionEffectsConfig(List.of());
    }

    public boolean hasEligibleEffect(
            @Nullable Holder<MobEffect> excludedEffect
    ) {
        for (CorruptionEffectsConfig.WeightedEffect entry : config.effects()) {
            if (!entry.effect().getEffect().equals(excludedEffect)) {
                return true;
            }
        }

        return false;
    }

    public Optional<MobEffectInstance> roll(
            @Nullable Holder<MobEffect> excludedEffect
    ) {
        List<CorruptionEffectsConfig.WeightedEffect> eligible =
                new ArrayList<>();

        long totalWeight = 0L;

        for (CorruptionEffectsConfig.WeightedEffect entry : config.effects()) {
            if (entry.effect().getEffect().equals(excludedEffect)) {
                continue;
            }

            eligible.add(entry);
            totalWeight += entry.weight();
        }

        if (eligible.isEmpty() || totalWeight <= 0L) {
            return Optional.empty();
        }

        long roll =
                ThreadLocalRandom.current()
                        .nextLong(
                                totalWeight
                        );

        for (CorruptionEffectsConfig.WeightedEffect entry : eligible) {
            roll -= entry.weight();

            if (roll < 0L) {
                return Optional.of(
                        entry.copyEffect()
                );
            }
        }

        return Optional.of(
                eligible.getLast()
                        .copyEffect()
        );
    }

    @Override
    protected @NotNull CorruptionEffectsConfig prepare(
            ResourceManager manager,
            @NotNull ProfilerFiller profiler
    ) {
        Optional<Resource> resource =
                manager.getResource(
                        FILE
                );

        if (resource.isEmpty()) {
            return CorruptionEffectsConfig.defaults();
        }

        try (Reader reader = resource.orElseThrow().openAsReader()) {
            JsonElement json =
                    JsonParser.parseReader(
                            reader
                    );

            DataResult<CorruptionEffectsConfig> result =
                    CorruptionEffectsConfig.CODEC.parse(
                            getRegistryLookup().createSerializationContext(
                                    JsonOps.INSTANCE
                            ),
                            json
                    );

            if (result.error().isPresent() || result.result().isEmpty()) {
                // Log and fall back, as for a missing file: one bad datapack file must not
                // reject the whole reload. Same policy as JolCraftCodecConfigManager.
                JolCraftLogs.error(
                        JolCraftLogTags.CONFIG,
                        "Ignored corruption effects config '{}': {}",
                        FILE,
                        result.error().map(DataResult.Error::message).orElse("no result")
                );

                return CorruptionEffectsConfig.defaults();
            }

            return result.result().get();
        } catch (IOException | JsonParseException exception) {
            JolCraftLogs.error(
                    JolCraftLogTags.CONFIG,
                    "Ignored corruption effects config '{}': {}",
                    FILE,
                    exception.toString()
            );

            return CorruptionEffectsConfig.defaults();
        }
    }

    @Override
    protected void apply(
            @NotNull CorruptionEffectsConfig prepared,
            @NotNull ResourceManager manager,
            @NotNull ProfilerFiller profiler
    ) {
        config = prepared;
    }
}
