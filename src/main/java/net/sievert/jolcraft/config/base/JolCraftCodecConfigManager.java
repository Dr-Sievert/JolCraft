package net.sievert.jolcraft.config.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.sievert.jolcraft.util.log.JolCraftLogTags;
import net.sievert.jolcraft.util.log.JolCraftLogs;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base for JolCraft's data-driven config managers.
 *
 * These are datapack reload listeners, so on a dedicated server the client never runs apply().
 * Anything read client-side must be pushed over with ClientboundConfigSyncPacket; syncPayload(),
 * applySynced() and reset() are that path.
 *
 * Bad or unknown files are logged and skipped, as vanilla json listeners do, so one broken file in
 * any datapack cannot reject the whole reload or keep a dedicated server from starting. A skipped
 * entry falls back to the manager's defaults.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class JolCraftCodecConfigManager<K, V> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    private final Codec<V> codec;

    private volatile Map<ResourceLocation, V> byId = Map.of();

    protected JolCraftCodecConfigManager(Codec<V> codec, String directory) {
        super(GSON, directory);
        this.codec = codec;
    }

    public final Codec<Map<ResourceLocation, V>> syncCodec() {
        return Codec.unboundedMap(ResourceLocation.CODEC, this.codec);
    }

    @Nullable
    protected abstract K keyFromId(ResourceLocation id);

    protected abstract void replaceAll(Map<K, V> values);

    @Override
    protected final void apply(
            Map<ResourceLocation, JsonElement> prepared,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        // LinkedHashMap, not Map.copyOf: that is unordered and salted per JVM run, which would make
        // the synced payload differ between restarts.
        Map<ResourceLocation, V> parsed = new LinkedHashMap<>();
        Map<K, V> loaded = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            K key = keyFromId(id);
            if (key == null) {
                JolCraftLogs.error(
                        JolCraftLogTags.CONFIG,
                        "Ignored config '{}' in '{}': unknown id",
                        id,
                        getName()
                );
                continue;
            }

            DataResult<V> result = codec.parse(JsonOps.INSTANCE, json);
            if (result.error().isPresent() || result.result().isEmpty()) {
                JolCraftLogs.error(
                        JolCraftLogTags.CONFIG,
                        "Ignored config '{}' in '{}': {}",
                        id,
                        getName(),
                        result.error().map(DataResult.Error::message).orElse("no result")
                );
                continue;
            }

            V value = result.result().get();

            parsed.put(id, value);
            loaded.put(key, value);
        }

        this.byId = Collections.unmodifiableMap(parsed);
        replaceAll(Collections.unmodifiableMap(loaded));
    }

    public final Map<ResourceLocation, V> syncPayload() {
        return this.byId;
    }

    /**
     * Applies a snapshot received from the server. Unknown ids are skipped like in apply(), but only
     * logged at debug level: the server already reported them, and a hard failure here would drop
     * the player from the server.
     */
    public final void applySynced(Map<ResourceLocation, V> values) {
        Map<ResourceLocation, V> parsed = new LinkedHashMap<>();
        Map<K, V> loaded = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, V> entry : values.entrySet()) {
            ResourceLocation id = entry.getKey();

            K key = keyFromId(id);
            if (key == null) {
                JolCraftLogs.debug(
                        JolCraftLogTags.NETWORK,
                        "Ignored unknown synced config id '{}' in '{}'",
                        id,
                        getName()
                );
                continue;
            }

            parsed.put(id, entry.getValue());
            loaded.put(key, entry.getValue());
        }

        this.byId = Collections.unmodifiableMap(parsed);
        replaceAll(Collections.unmodifiableMap(loaded));
    }

    public final void reset() {
        this.byId = Map.of();
        replaceAll(Map.of());
    }
}
