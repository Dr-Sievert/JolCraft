package net.sievert.jolcraft.module.composite.loot;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import net.sievert.jolcraft.stereotype.Value;

/**
 * What applying yields produced: the values rolled, in order, after the hooks had their say, and whether a hook
 * refused the whole, in which case nothing was rolled. The system that asked acts on it: hands the stacks over,
 * spawns the entities, plays the sounds, applies the effects, reads its own kinds through {@link #of}.
 */
@Value
public record Delivery(List<Yielded> yielded, boolean refused) {

    public static final Delivery REFUSED = new Delivery(List.of(), true);

    public Delivery {
        yielded = List.copyOf(yielded);
    }

    /** The rolled values of one kind, in order. */
    public <T extends Yielded> List<T> of(Class<T> kind) {
        return yielded.stream().filter(kind::isInstance).map(kind::cast).toList();
    }

    public List<ItemStack> stacks() {
        return of(Yielded.Stack.class).stream().map(Yielded.Stack::stack).toList();
    }

    public List<Yielded.Entities> entities() {
        return of(Yielded.Entities.class);
    }

    public List<Yielded.Sound> sounds() {
        return of(Yielded.Sound.class);
    }

    public List<MobEffectInstance> effects() {
        return of(Yielded.Effect.class).stream().map(Yielded.Effect::effect).toList();
    }

    public boolean isEmpty() {
        return yielded.isEmpty();
    }
}
