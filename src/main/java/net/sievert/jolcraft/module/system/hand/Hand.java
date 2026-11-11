package net.sievert.jolcraft.module.system.hand;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.composite.capability.Inventory;
import net.sievert.jolcraft.module.composite.loot.Delivery;
import net.sievert.jolcraft.module.composite.loot.Result;
import net.sievert.jolcraft.module.composite.loot.SoundYield;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.recipe.Recipe;
import net.sievert.jolcraft.module.system.outcome.Outcome;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Two-hand interaction recipes ({@link HandRecipe}), fired on the server when a player uses the main hand's item while
 * the offhand holds the other. Among the recipes that match, one that requires sneaking wins, else the first in the
 * map's order. A firing pays both hands in one transaction before the outcome system delivers the results; a hook's
 * refusal pays nothing. On every path after a match both items get the recipe's cooldown and the use is consumed, so
 * the item's own use and the offhand's never run; only a firing swings, the tool's hand. Design in design/hand.md.
 */
@ModSystem(Dictionary.HAND)
public final class Hand implements Module {

    private final Result result;
    private final Inventory inventory;
    private final Outcome outcome;
    private final Log log;

    public Hand(Recipe recipe, Result result, Inventory inventory, Outcome outcome, Log log) {
        recipe.declare(HandKeys.RECIPE);
        this.result = result;
        this.inventory = inventory;
        this.outcome = outcome;
        this.log = log.tagged(this);
    }

    /** Adds the system's listener to the game bus, low, so a use another listener cancels first is never seen. */
    public void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOW, this::onUse);
    }

    private void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemCooldowns cooldowns = player.getCooldowns();
        if (main.isEmpty() || off.isEmpty() || cooldowns.isOnCooldown(main) || cooldowns.isOnCooldown(off)) {
            return;
        }
        ServerLevel level = player.level();
        RecipeHolder<HandRecipe> chosen = choose(level, new HandInput(main, off, player.isSecondaryUseActive()));
        if (chosen == null) {
            return;
        }
        HandRecipe recipe = chosen.value();
        Optional<InteractionHand> orientation = recipe.orientation(main, off);
        if (orientation.isEmpty()) {
            return;
        }
        InteractionHand toolHand = orientation.get();
        Identifier mainGroup = cooldowns.getCooldownGroup(main);
        Identifier offGroup = cooldowns.getCooldownGroup(off);
        Optional<Inventory.Match> tool = inventory.match(new Inventory.Holder.Hand(player, toolHand), List.of(recipe.tool()));
        Optional<Inventory.Match> target = inventory.match(new Inventory.Holder.Hand(player, other(toolHand)), List.of(recipe.target()));
        if (tool.isEmpty() || target.isEmpty()) {
            return;
        }
        Consumer<LootParams.Builder> params = context(player, tool.get(), target.get());
        Delivery delivery = result.apply(recipe.results(), level, HandContext.SHAPE, params);
        String name = player.getName().getString();
        if (delivery.refused()) {
            recipe.failure().ifPresent(sound -> play(sound, player, params));
            log.debug("{} was refused for {}", chosen.id().identifier(), name);
        } else if (!inventory.pay(List.of(tool.get(), target.get()))) {
            recipe.failure().ifPresent(sound -> play(sound, player, params));
            log.warn("{} matched for {} but could not be paid; something changed a hand between the match and the pay", chosen.id().identifier(), name);
        } else {
            outcome.deliver(delivery, player);
            (delivery.isEmpty() ? recipe.failure() : recipe.success()).ifPresent(sound -> play(sound, player, params));
            player.swing(toolHand, true);
            log.debug("{} fired for {} with the tool in the {}, {} values delivered", chosen.id().identifier(), name, toolHand, delivery.yielded().size());
        }
        cooldowns.addCooldown(mainGroup, recipe.cooldown());
        cooldowns.addCooldown(offGroup, recipe.cooldown());
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    /** The first matching recipe that requires sneaking, else the first that matches, in the map's order; none when nothing matches. */
    private static @Nullable RecipeHolder<HandRecipe> choose(ServerLevel level, HandInput input) {
        @Nullable RecipeHolder<HandRecipe> first = null;
        for (RecipeHolder<HandRecipe> holder : HandKeys.RECIPE.all(level).toList()) {
            if (holder.value().matches(input, level)) {
                if (holder.value().sneaking()) {
                    return holder;
                }
                if (first == null) {
                    first = holder;
                }
            }
        }
        return first;
    }

    /** The context's parameters: the player, their position and the match's copies of the two stacks. */
    private static Consumer<LootParams.Builder> context(ServerPlayer player, Inventory.Match tool, Inventory.Match target) {
        ItemStack toolStack = tool.stacks().getFirst();
        ItemStack targetStack = target.stacks().getFirst();
        return builder -> builder.withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.TOOL, toolStack)
                .withParameter(HandContext.TARGET, targetStack);
    }

    /** Rolls a sound of the recipe's own against the same context and delivers it the way the results are. */
    private void play(SoundYield sound, ServerPlayer player, Consumer<LootParams.Builder> params) {
        outcome.deliver(result.apply(List.of(sound), player.level(), HandContext.SHAPE, params), player);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }
}
