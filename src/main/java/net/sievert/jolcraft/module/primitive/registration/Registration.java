package net.sievert.jolcraft.module.primitive.registration;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Registries of the mod's own: custom registries, created when the game asks for them, and datapack registries,
 * whose entries come from data files and are synced to clients when asked. Modules declare theirs while they are
 * built; the root then calls {@link #register(IEventBus)} once, and a declaration after that is an error. Content
 * that goes into vanilla's registries needs none of this: it is a {@code DeferredRegister} in a {@code @Content}
 * class, which also works for a custom registry declared here, by its key.
 */
@Primitive(Dictionary.REGISTRATION)
public interface Registration extends Module {

    /**
     * A registry of the mod's own; {@code synced} sends its contents to clients. The game creates it when it asks
     * for new registries, so the supplier answers from then on and throws before.
     */
    <T> Supplier<Registry<T>> custom(ResourceKey<Registry<T>> key, boolean synced);

    /**
     * A datapack registry: its entries are the JSON files under
     * {@code data/<entry namespace>/<registry namespace>/<registry path>/} (the registry's namespace folder is omitted
     * only for the game's own registries), decoded with {@code codec}; with a {@code network} codec they are synced to
     * clients as well.
     */
    <T> void datapack(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> network);

    /** Attaches every declaration to the mod bus. The root calls it once, after every module is built. */
    void register(IEventBus modBus);
}
