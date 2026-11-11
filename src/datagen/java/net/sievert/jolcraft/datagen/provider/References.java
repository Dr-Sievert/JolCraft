package net.sievert.jolcraft.datagen.provider;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import net.sievert.jolcraft.JolCraft;
import net.sievert.jolcraft.module.primitive.log.Log;
import org.jetbrains.annotations.NotNull;

/**
 * The last check of the data run: a client file of the mod that names something which does not exist fails the run,
 * where the game would show a magenta cube in {@code runClient}. Every block state, model and item definition of the
 * mod's namespace is read, those generated in this run from the pack output and the hand-made ones in
 * {@code src/main/resources} through the client resource manager. A block state or item definition must name models
 * that exist; a model must have a parent that exists ({@code builtin/} parents always do) and textures that exist
 * (a {@code #reference} to another slot is not a file). The generated {@code sounds.json}, if there is one, must have
 * an {@code .ogg} for every sound file in the mod's namespace. A model exists when it was generated in this run or the
 * manager has it; the data run's manager holds the game's own models and textures too, so a reference into
 * {@code minecraft} is checked like any other. Sound files of other namespaces are not checked, since the game's sounds
 * are not shipped as files. This provider must be registered after every provider that writes a client file.
 */
public final class References implements DataProvider {

    private final PackOutput output;
    private final ResourceManager assets;
    private final Log log;

    private final List<String> problems = new ArrayList<>();
    private Set<Identifier> generatedModels = Set.of();
    private int files;
    private int references;

    public References(PackOutput output, ResourceManager assets, Log log) {
        this.output = output;
        this.assets = assets;
        this.log = log;
    }

    @Override
    public CompletableFuture<?> run(@NotNull CachedOutput cache) {
        Path root = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(JolCraft.MOD_ID);
        generatedModels = generated(root, "models").keySet();
        check("block state", "blockstates", root, this::checkModelsNamed);
        check("item definition", "items", root, this::checkModelsNamed);
        check("model", "models", root, this::checkModel);
        checkSounds(root);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(problems.size() + " broken client references: " + String.join("; ", problems));
        }
        log.info("{} client files read, {} references found, every model, parent, texture and sound file exists", files, references);
        return CompletableFuture.completedFuture(null);
    }

    @FunctionalInterface
    private interface Checker {

        void check(String what, Identifier id, JsonElement json);
    }

    /** Reads every file of a folder, generated or hand-made, and hands each to {@code checker}. */
    private void check(String what, String folder, Path root, Checker checker) {
        Map<Identifier, Path> generated = generated(root, folder);
        Map<Identifier, JsonElement> found = new LinkedHashMap<>();
        generated.forEach((id, file) -> found.put(id, parse(id, file)));
        assets.listResources(folder, id -> id.getNamespace().equals(JolCraft.MOD_ID) && id.getPath().endsWith(".json")).forEach((file, resource) -> {
            Identifier id = file.withPath(path -> path.substring(folder.length() + 1, path.length() - ".json".length()));
            if (!found.containsKey(id)) {
                try (Reader reader = resource.openAsReader()) {
                    found.put(id, JsonParser.parseReader(reader));
                } catch (IOException | RuntimeException unreadable) {
                    problems.add(what + " " + id + " could not be read: " + unreadable.getMessage());
                }
            }
        });
        found.forEach((id, json) -> {
            files++;
            checker.check(what, id, json);
        });
    }

    /** The files of a generated folder by the identifier they answer to. */
    private Map<Identifier, Path> generated(Path root, String folder) {
        Path directory = root.resolve(folder);
        Map<Identifier, Path> found = new LinkedHashMap<>();
        if (Files.isDirectory(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.filter(file -> file.toString().endsWith(".json")).sorted().forEach(file -> {
                    String relative = directory.relativize(file).toString().replace('\\', '/');
                    found.put(Identifier.fromNamespaceAndPath(JolCraft.MOD_ID, relative.substring(0, relative.length() - ".json".length())), file);
                });
            } catch (IOException unreadable) {
                throw new IllegalStateException("the generated " + folder + " could not be listed", unreadable);
            }
        }
        return found;
    }

    private JsonElement parse(Identifier id, Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader);
        } catch (IOException | RuntimeException unreadable) {
            problems.add(file + " (" + id + ") could not be read: " + unreadable.getMessage());
            return new JsonObject();
        }
    }

    /** A block state or item definition: every string under a {@code model} key, or a special model's {@code base}, however deep, is a model. */
    private void checkModelsNamed(String what, Identifier id, JsonElement json) {
        models(json, named -> {
            references++;
            requireModel(what, id, "model", named);
        });
    }

    private void models(JsonElement json, Consumer<String> found) {
        switch (json) {
            case JsonObject object -> object.entrySet().forEach(entry -> {
                if ((entry.getKey().equals("model") || entry.getKey().equals("base")) && entry.getValue().isJsonPrimitive()) {
                    found.accept(entry.getValue().getAsString());
                } else {
                    models(entry.getValue(), found);
                }
            });
            case JsonArray array -> array.forEach(element -> models(element, found));
            default -> {
            }
        }
    }

    /** A model: its parent is a model or a built-in one, and each of its textures that is not a reference to a slot is a file. */
    private void checkModel(String what, Identifier id, JsonElement json) {
        if (!(json instanceof JsonObject model)) {
            problems.add(what + " " + id + " is not a JSON object");
            return;
        }
        if (model.get("parent") instanceof JsonPrimitive parent) {
            references++;
            Identifier named = parseId(what, id, "parent", parent.getAsString());
            if (named != null && !named.getPath().startsWith("builtin/")) {
                requireModel(what, id, "parent", parent.getAsString());
            }
        }
        if (model.get("textures") instanceof JsonObject textures) {
            textures.entrySet().forEach(slot -> {
                String texture = switch (slot.getValue()) {
                    case JsonPrimitive primitive -> primitive.getAsString();
                    case JsonObject object when object.get("sprite") instanceof JsonPrimitive sprite -> sprite.getAsString();
                    default -> null;
                };
                if (texture != null && !texture.startsWith("#")) {
                    references++;
                    Identifier named = parseId(what, id, "texture " + slot.getKey(), texture);
                    if (named != null && assets.getResource(named.withPath(path -> "textures/" + path + ".png")).isEmpty()) {
                        problems.add(what + " " + id + " names the missing texture " + named + " (textures/" + named.getPath() + ".png) in " + slot.getKey());
                    }
                }
            });
        }
    }

    private void requireModel(String what, Identifier id, String as, String named) {
        Identifier model = parseId(what, id, as, named);
        if (model != null && !generatedModels.contains(model)
                && assets.getResource(model.withPath(path -> "models/" + path + ".json")).isEmpty()) {
            problems.add(what + " " + id + " names the missing " + as + " " + model + " (models/" + model.getPath() + ".json)");
        }
    }

    private Identifier parseId(String what, Identifier id, String as, String text) {
        Identifier parsed = Identifier.tryParse(text);
        if (parsed == null) {
            problems.add(what + " " + id + " has the " + as + " \"" + text + "\", which is no identifier");
        }
        return parsed;
    }

    /** The mod's {@code sounds.json}: each sound file it names in the mod's namespace is in the manager. */
    private void checkSounds(Path root) {
        Path file = root.resolve("sounds.json");
        if (!Files.isRegularFile(file)) {
            return;
        }
        files++;
        Set<String> checked = new HashSet<>();
        if (parse(Identifier.fromNamespaceAndPath(JolCraft.MOD_ID, "sounds"), file) instanceof JsonObject definitions) {
            definitions.entrySet().forEach(definition -> {
                if (definition.getValue() instanceof JsonObject body && body.get("sounds") instanceof JsonArray sounds) {
                    sounds.forEach(sound -> soundFile(definition.getKey(), sound, checked));
                }
            });
        }
    }

    private void soundFile(String event, JsonElement sound, Set<String> checked) {
        String named = switch (sound) {
            case JsonPrimitive primitive -> primitive.getAsString();
            case JsonObject object when object.get("name") instanceof JsonPrimitive name
                    && !(object.get("type") instanceof JsonPrimitive type && type.getAsString().equals("event")) -> name.getAsString();
            default -> null;
        };
        Identifier id = named == null ? null : Identifier.tryParse(named);
        if (id == null || !id.getNamespace().equals(JolCraft.MOD_ID) || !checked.add(named)) {
            return;
        }
        references++;
        if (assets.getResource(id.withPath(path -> "sounds/" + path + ".ogg")).isEmpty()) {
            problems.add("sound " + event + " names the missing sound file " + id + " (sounds/" + id.getPath() + ".ogg)");
        }
    }

    @Override
    public @NotNull String getName() {
        return "Client references";
    }
}
