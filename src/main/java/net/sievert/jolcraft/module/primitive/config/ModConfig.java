package net.sievert.jolcraft.module.primitive.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import net.sievert.jolcraft.stereotype.Implementation;

/**
 * {@link Config} over NeoForge's {@code ModConfigSpec}. The game's own class of the same name,
 * {@code net.neoforged.fml.config.ModConfig}, is used only here, by its full name, for the file kinds.
 */
@Implementation
public final class ModConfig implements Config {

    private final Map<File, List<ConfigKey<?>>> declared = new EnumMap<>(File.class);
    private final Map<List<String>, File> paths = new HashMap<>();
    private final Map<ConfigKey<?>, ConfigValue<?>> values = new HashMap<>();
    private final Map<ConfigKey<?>, ModConfigSpec> specs = new HashMap<>();
    private boolean built;

    @Override
    public <T> void declare(File file, ConfigKey<T> key) {
        if (built) {
            throw new IllegalStateException(key.path() + " was declared after the config files were built");
        }
        paths.forEach((other, owner) -> {
            if (other.equals(key.path())) {
                throw new IllegalStateException(key.path() + " was declared twice: in " + owner + " and " + file);
            }
            if (overlap(other, key.path())) {
                throw new IllegalStateException(key.path() + " overlaps " + other + " declared in " + owner
                        + ": a setting's path may not be a section of another's");
            }
        });
        paths.put(key.path(), file);
        declared.computeIfAbsent(file, kind -> new ArrayList<>()).add(key);
    }

    /** Whether one path is a prefix of the other, so one setting would sit where the other's section is. */
    private static boolean overlap(List<String> one, List<String> other) {
        int shared = Math.min(one.size(), other.size());
        return one.subList(0, shared).equals(other.subList(0, shared));
    }

    @Override
    public void register(ModContainer container) {
        build(container.getModId()).forEach((file, spec) -> container.registerConfig(net.neoforged.fml.config.ModConfig.Type.valueOf(file.name()), spec));
    }

    @Override
    public <T> T get(ConfigKey<T> key) {
        @SuppressWarnings("unchecked")
        ConfigValue<T> known = (ConfigValue<T>) values.get(key);
        ModConfigSpec spec = specs.get(key);
        boolean loaded = spec != null && spec.isLoaded();
        if (paths.get(key.path()) == File.STARTUP && !loaded) {
            throw new IllegalStateException(key.path() + " is a startup setting read before the root registered the config files;"
                    + " read it from the registry events on, not while a module is built");
        }
        if (known == null) {
            if (built) {
                throw new IllegalStateException(key.path() + " was never declared; a module declares its keys while it is built");
            }
            return key.fallback();
        }
        return loaded ? known.get() : key.fallback();
    }

    /** How many settings are declared, for the root's log line. */
    public int declared() {
        return paths.size();
    }

    /**
     * Builds one spec per file that has settings: each setting inside its sections, every level with its translation
     * key under the namespace, startup settings marked as needing a game restart. Package-private so a test can
     * build without a mod container.
     */
    Map<File, ModConfigSpec> build(String namespace) {
        if (built) {
            throw new IllegalStateException("the config files were built twice");
        }
        built = true;
        Map<File, ModConfigSpec> files = new EnumMap<>(File.class);
        declared.forEach((file, keys) -> {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            Map<ConfigKey<?>, ConfigValue<?>> defined = new HashMap<>();
            for (ConfigKey<?> key : keys) {
                List<String> sections = key.sections();
                for (int depth = 0; depth < sections.size(); depth++) {
                    builder.translation(ConfigKey.translation(namespace, key.path().subList(0, depth + 1).toArray(String[]::new)))
                            .push(List.of(sections.get(depth)));
                }
                builder.comment(key.comment()).translation(key.translation(namespace));
                if (file == File.STARTUP) {
                    builder.gameRestart();
                }
                defined.put(key, key.defineValue(builder, key.name()));
                builder.pop(sections.size());
            }
            ModConfigSpec spec = builder.build();
            defined.forEach((key, configValue) -> {
                values.put(key, configValue);
                specs.put(key, spec);
            });
            files.put(file, spec);
        });
        return files;
    }
}
