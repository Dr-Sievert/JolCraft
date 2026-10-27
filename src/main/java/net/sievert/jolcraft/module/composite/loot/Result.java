package net.sievert.jolcraft.module.composite.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.loot.Loot;
import net.sievert.jolcraft.module.primitive.loot.Shape;
import net.sievert.jolcraft.module.primitive.registration.Ids;
import net.sievert.jolcraft.module.primitive.registration.Registration;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Data-described yields rolled against a loot context, for any system: a bounty's reward, a station's output, a
 * dwarf's gift, a hand interaction's upshot. A root declares its {@link YieldKinds} once, under its own namespace,
 * and the composite registers the four kinds of its own (item pool, entity, sound, effect) and every kind and hook a
 * module declares while it is built; the root attaches it to the mod bus once. A system then hands {@link #apply} the
 * yields its data holds and a context of the shape the data was validated against, and acts on the {@link Delivery}:
 * nothing here spawns, plays or gives. Built from {@code loot}, {@code registration} and {@code log}.
 */
@Composite(Dictionary.RESULT)
public final class Result implements Module {

    private final Loot loot;
    private final Registration registration;
    private final Log log;
    private final Map<Identifier, YieldKind> kinds = new LinkedHashMap<>();
    private final Map<Identifier, ResultHook> hooks = new LinkedHashMap<>();
    private final Set<MapCodec<?>> codecs = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ResultHook> hookInstances = Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable YieldKinds declared;
    private @Nullable Supplier<Registry<ResultHook>> hookRegistry;
    private boolean registered;

    public Result(Loot loot, Registration registration, Log log) {
        this.loot = loot;
        this.registration = registration;
        this.log = log.tagged(this);
    }

    /**
     * Declares the root's yield registries, once, and the composite's own four kinds under their namespace; before
     * {@link Registration#register}, since the registries go through it.
     */
    public void declare(YieldKinds yieldKinds) {
        open(yieldKinds.id());
        if (declared != null) {
            throw new IllegalStateException(yieldKinds + " was declared while " + declared + " already is; a root declares one yield kinds");
        }
        declared = yieldKinds;
        registration.custom(yieldKinds.kinds(), false);
        hookRegistry = registration.custom(yieldKinds.hooks(), false);
        String namespace = yieldKinds.id().getNamespace();
        declare(YieldKind.of(Ids.of(namespace, Dictionary.ITEM), ItemYield.CODEC));
        declare(YieldKind.of(Ids.of(namespace, Dictionary.ENTITY), EntityYield.CODEC));
        declare(YieldKind.of(Ids.of(namespace, Dictionary.SOUND), SoundYield.CODEC));
        declare(YieldKind.of(Ids.of(namespace, Dictionary.EFFECT), EffectYield.CODEC));
    }

    /** Declares a kind of a module's own; only while modules are built. */
    public void declare(YieldKind kind) {
        open(kind.id());
        if (kinds.containsKey(kind.id())) {
            throw new IllegalStateException(kind + " was declared twice");
        }
        if (codecs.contains(kind.codec())) {
            throw new IllegalStateException(kind + " shares its codec with another kind; the game refuses one codec under two ids");
        }
        kinds.put(kind.id(), kind); // nothing is stored before both checks pass
        codecs.add(kind.codec());
    }

    /** Declares a hook under the id data names it by; only while modules are built. */
    public void declare(Identifier id, ResultHook hook) {
        open(id);
        if (hooks.containsKey(id)) {
            throw new IllegalStateException(id + " was declared twice");
        }
        if (hookInstances.contains(hook)) {
            throw new IllegalStateException(id + " names a hook already declared under another id; the game refuses one value under two ids");
        }
        hooks.put(id, hook);
        hookInstances.add(hook);
    }

    private void open(Identifier id) {
        if (registered) {
            throw new IllegalStateException(id + " was declared after the results were registered");
        }
    }

    /** Attaches the registration of kinds and hooks to the mod bus. The root calls it once, after every module is built. */
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the results were registered twice");
        }
        if (declared == null) {
            throw new IllegalStateException("no yield kinds are declared; the root declares its own through Result before registering");
        }
        registered = true;
        YieldKinds yieldKinds = declared;
        modBus.addListener(RegisterEvent.class, event -> {
            event.register(yieldKinds.kinds(), helper -> kinds.forEach((id, kind) -> helper.register(id, kind.codec())));
            event.register(yieldKinds.hooks(), helper -> hooks.forEach(helper::register));
        });
    }

    /** How many kinds and hooks are declared, the composite's own kinds included; the root logs it. */
    public int declared() {
        return kinds.size() + hooks.size();
    }

    /**
     * Rolls the yields against the context, after every hook they name agreed, and answers what to act on; a hook's
     * refusal answers a refused, empty delivery.
     */
    public Delivery apply(List<? extends Yield> yields, LootContext context) {
        for (Yield yield : yields) {
            for (Identifier id : yield.hooks()) {
                if (!hook(id).canApply(context)) {
                    log.debug("{} refused {}", id, yield);
                    return Delivery.REFUSED;
                }
            }
        }
        List<Yielded> yielded = new ArrayList<>();
        for (Yield yield : yields) {
            for (Yielded rolled : yield.roll(context)) {
                Optional<Yielded> kept = Optional.of(rolled);
                for (Identifier id : yield.hooks()) {
                    kept = kept.flatMap(current -> hook(id).apply(context, current));
                }
                kept.ifPresent(yielded::add);
            }
        }
        return new Delivery(yielded, false);
    }

    /** Rolls one of the weighted yields, chosen with the context's random; nothing when the list is empty. */
    public Delivery apply(WeightedList<? extends Yield> choice, LootContext context) {
        Optional<? extends Yield> chosen = choice.getRandom(context.getRandom());
        return chosen.isPresent() ? apply(List.of(chosen.get()), context) : new Delivery(List.of(), false);
    }

    /** The same with the context built here under the shape the yields were validated against. */
    public Delivery apply(List<? extends Yield> yields, ServerLevel level, Shape shape, Consumer<LootParams.Builder> params) {
        return apply(yields, loot.context(level, shape, params));
    }

    private ResultHook hook(Identifier id) {
        if (hookRegistry == null) {
            throw new IllegalStateException("no yield kinds are declared; the root declares its own through Result before anything is applied");
        }
        return hookRegistry.get().getOptional(id).orElseThrow(() -> new IllegalArgumentException(
                id + " is no hook declared through Result; a yield names hooks a module declared while it was built, or it is applied before the game registered them"));
    }
}
