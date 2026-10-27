package net.sievert.jolcraft.module.composite.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.capability.Capability;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.recipe.Fate;
import net.sievert.jolcraft.module.primitive.recipe.ItemInput;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Items counted, taken and given against a holder's item handler: a player or one of their hands through the game's
 * player inventory wrapper, an entity or a block through the game's capability lookups, found when an operation
 * runs. Taking is the recipe primitive's {@link ItemInput} values matched against the holder, pure, and the match
 * paid, each fate through {@link Fate#apply} so the roll and the creative rule live in one place. Every operation
 * runs on the server thread; one that changes something opens its own root transaction and commits or rolls back
 * before it answers, so a caller never sees a transaction and the change is all or nothing over the inventory's
 * contents, while counting and matching are pure; the game
 * refuses a root inside another, so a module's container must not call this from its own {@code setItem},
 * {@code setChanged} or {@code onTransfer}. Overflow is dropped at the holder after the commit. A module's own container block entity
 * joins through {@link #exposeBlockEntity}, declared while the module is built through {@code capability}, which
 * the root registers once. Built from {@code capability} and {@code log}; design/inventory.md.
 */
@Composite(Dictionary.INVENTORY)
public final class Inventory implements Module {

    private final Capability capability;
    private final Log log;
    private int exposed;

    public Inventory(Capability capability, Log log) {
        this.capability = capability;
        this.log = log.tagged(this);
    }

    /**
     * Declares a module's container block entity as an item handler under {@code Capabilities.Item.BLOCK}, wrapped
     * the way the game wraps its own containers (a {@link WorldlyContainer} with the side it is seen from), so this
     * composite, hoppers and other mods reach it alike. The type is asked for when the game registers, so a holder of
     * the module's own is fine. Only while modules are built.
     */
    public <B extends BlockEntity & Container> void exposeBlockEntity(Supplier<? extends BlockEntityType<@NotNull B>> type) {
        capability.blockEntity(Capabilities.Item.BLOCK, type, (entity, side) -> entity instanceof WorldlyContainer worldly
                ? new WorldlyContainerWrapper(worldly, side)
                : VanillaContainerWrapper.of(entity));
        exposed++;
    }

    /** How many holders are exposed, for the root's log line. */
    public int declared() {
        return exposed;
    }

    /** The items of the holder the predicate accepts, summed over its slots; the predicate sees a one-item stack of each resource, the game's own shared one, which it must neither keep nor change. Pure. */
    public int count(Holder holder, Predicate<ItemStack> predicate) {
        requireServerThread(holder);
        ResourceHandler<ItemResource> handler = holder.taking().orElse(null);
        if (handler == null) {
            return 0;
        }
        long total = 0;
        for (int index = 0, size = handler.size(); index < size; index++) {
            ItemResource resource = handler.getResource(index);
            if (!resource.isEmpty() && resource.test(predicate)) {
                total += handler.getAmountAsLong(index);
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Whether the holder has at least the amount of items the predicate accepts. */
    public boolean has(Holder holder, Predicate<ItemStack> predicate, int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("a holder has at least one item or none, not " + amount);
        }
        return count(holder, predicate) >= amount;
    }

    /** Gives one stack; see {@link #give(Holder, List)}. */
    public List<ItemStack> give(Holder holder, ItemStack stack) {
        return give(holder, List.of(stack));
    }

    /**
     * Gives the stacks to the holder, each inserted stacking into its handler, and drops what does not fit at the
     * holder after the commit. The given stacks are never changed and an empty one is skipped. Answers the stacks
     * dropped as overflow, split by their max stack size, as copies of what the item entities hold, none when
     * everything fit; a drop the game refuses is logged at warn, since those items then exist nowhere: they did not
     * fit and the game would not toss them.
     */
    public List<ItemStack> give(Holder holder, List<ItemStack> stacks) {
        requireServerThread(holder);
        List<ItemStack> overflow = new ArrayList<>();
        int inserted = 0;
        try (Transaction transaction = Transaction.openRoot()) {
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    continue;
                }
                ItemResource resource = ItemResource.of(stack);
                int accepted = holder.insert(resource, stack.getCount(), transaction);
                inserted += accepted;
                split(resource, stack.getCount() - accepted, stack.getMaxStackSize(), overflow);
            }
            transaction.commit();
        }
        drop(holder, overflow);
        log.debug("gave {} items to {}, {} stacks dropped", inserted, holder, overflow.size());
        return List.copyOf(overflow);
    }

    /**
     * Matches the inputs against the holder, pure: for each input in order, the first resource of the holder in index
     * order that the input's predicate accepts and whose total across the holder, less what earlier inputs of this
     * match claimed, covers the fate ({@link ItemInput#matches} on a stack of that total, so a consume needs its
     * largest amount and a damage a damageable item); one resource per input, never two kinds mixed, one pass with no
     * backtracking, so a recipe whose inputs overlap lists the narrower first. Empty when an input finds nothing, or
     * the holder has no handler and inputs to match; no inputs match trivially.
     */
    public Optional<Match> match(Holder holder, List<ItemInput> inputs) {
        requireServerThread(holder);
        ResourceHandler<ItemResource> handler = holder.taking().orElse(null);
        if (handler == null) {
            return inputs.isEmpty() ? Optional.of(new Match(holder, List.of())) : Optional.empty();
        }
        Map<ItemResource, Integer> available = available(handler);
        List<Match.Found> found = new ArrayList<>();
        for (ItemInput input : inputs) {
            Match.Found hit = find(input, available);
            if (hit == null) {
                return Optional.empty();
            }
            found.add(hit);
            available.merge(hit.resource(), -claimed(input.fate()), Integer::sum);
        }
        return Optional.of(new Match(holder, found));
    }

    /**
     * Pays the match's fates in one transaction, in this order: the holder's coverage of the match is checked again;
     * every consume is rolled through {@link Fate#apply} on a scratch stack, the one roll and creative rule, and that
     * amount extracted across the holder's slots, its crafting remainder (a bucket's empty bucket) noted, one per item;
     * every damage extracts its one item; a shortfall anywhere (a sided handler refusing through its face) leaves
     * without a commit, nothing changed, and answers false; only then each damage's copy is worn through
     * {@link Fate#apply}, the game's own wear with its break sound and criterion, which no rollback undoes, hence the
     * order, and put back at its slot when it survived, elsewhere in the holder when that slot refuses it, else as
     * overflow; the remainders are given to the holder, so a creative payer, who consumed nothing, gets none; then
     * the commit, and the overflow drops at the holder. A keep does nothing.
     */
    public boolean pay(Match match) {
        Holder holder = match.holder();
        requireServerThread(holder);
        ResourceHandler<ItemResource> handler = holder.taking().orElse(null);
        if (handler == null) {
            return match.found().isEmpty();
        }
        if (!covers(handler, match)) {
            return false;
        }
        ServerLevel level = holder.level();
        ServerPlayer payer = holder.payer();
        List<Worn> worn = new ArrayList<>();
        List<ItemStack> remainders = new ArrayList<>();
        List<ItemStack> overflow = new ArrayList<>();
        try (Transaction transaction = Transaction.openRoot()) {
            for (Match.Found found : match.found()) {
                Fate fate = found.input().fate();
                switch (fate.kind()) {
                    case KEEP -> { }
                    case CONSUME -> {
                        int rolled = fate.apply(found.resource().toStack(claimed(fate)), level, payer);
                        if (rolled > 0) {
                            if (handler.extract(found.resource(), rolled, transaction) != rolled) {
                                return false;
                            }
                            ItemStackTemplate remainder = found.resource().toStack().getCraftingRemainder();
                            ItemStack one = remainder == null ? ItemStack.EMPTY : remainder.create();
                            if (!one.isEmpty()) {
                                remainders.add(one.copyWithCount(one.getCount() * rolled));
                            }
                        }
                    }
                    case DAMAGE -> {
                        int index = ResourceHandlerUtil.indexOf(handler, found.resource());
                        if (index < 0 || handler.extract(index, found.resource(), 1, transaction) != 1) {
                            return false;
                        }
                        worn.add(new Worn(found, index));
                    }
                }
            }
            for (Worn each : worn) {
                ItemStack copy = each.found().resource().toStack();
                each.found().input().fate().apply(copy, level, payer);
                if (!copy.isEmpty()) {
                    ItemResource changed = ItemResource.of(copy);
                    if (handler.insert(each.index(), changed, 1, transaction) == 0 && holder.insert(changed, 1, transaction) == 0) {
                        overflow.add(copy);
                    }
                }
            }
            for (ItemStack remainder : remainders) {
                ItemResource resource = ItemResource.of(remainder);
                split(resource, remainder.getCount() - holder.insert(resource, remainder.getCount(), transaction), remainder.getMaxStackSize(), overflow);
            }
            transaction.commit();
        }
        drop(holder, overflow);
        log.debug("{} paid {} inputs, {} stacks dropped", holder, match.found().size(), overflow.size());
        return true;
    }

    /** {@link #match} then {@link #pay}, for a host with no hooks between: the match, present only when it was paid. */
    public Optional<Match> take(Holder holder, List<ItemInput> inputs) {
        Optional<Match> match = match(holder, inputs);
        return match.isPresent() && pay(match.get()) ? match : Optional.empty();
    }

    /** What an input claims of its resource when matched: a consume its largest amount, a damage or a keep the one item it uses. */
    static int claimed(Fate fate) {
        return switch (fate.kind()) {
            case CONSUME -> fate.amount().maxInclusive();
            case DAMAGE, KEEP -> 1;
        };
    }

    /** Each resource of the handler with its total, in index order. */
    private static Map<ItemResource, Integer> available(ResourceHandler<ItemResource> handler) {
        Map<ItemResource, Integer> available = new LinkedHashMap<>();
        for (int index = 0, size = handler.size(); index < size; index++) {
            ItemResource resource = handler.getResource(index);
            if (!resource.isEmpty()) {
                available.merge(resource, handler.getAmountAsInt(index), (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
            }
        }
        return available;
    }

    private static Match.@Nullable Found find(ItemInput input, Map<ItemResource, Integer> available) {
        for (Map.Entry<ItemResource, Integer> entry : available.entrySet()) {
            int left = entry.getValue();
            if (left > 0 && input.matches(entry.getKey().toStack(left))) {
                return new Match.Found(input, entry.getKey(), left);
            }
        }
        return null;
    }

    /** Whether the handler still covers every found resource of the match, claims taken in order. */
    private static boolean covers(ResourceHandler<ItemResource> handler, Match match) {
        Map<ItemResource, Integer> available = available(handler);
        for (Match.Found found : match.found()) {
            int left = available.getOrDefault(found.resource(), 0);
            if (left < claimed(found.input().fate()) || !found.input().matches(found.resource().toStack(left))) {
                return false;
            }
            available.put(found.resource(), left - claimed(found.input().fate()));
        }
        return true;
    }

    private static void split(ItemResource resource, int amount, int maxStackSize, List<ItemStack> into) {
        for (int left = amount; left > 0; left -= Math.min(left, maxStackSize)) {
            into.add(resource.toStack(Math.min(left, maxStackSize)));
        }
    }

    /** Drops the overflow at the holder, after a commit, as copies; a drop the game refuses is logged, since those items then exist nowhere. */
    private void drop(Holder holder, List<ItemStack> overflow) {
        for (ItemStack stack : overflow) {
            if (!holder.drop(stack.copy())) {
                log.warn("{} could not be dropped at {}: it did not fit and the game refused to toss it, so it is lost", stack, holder);
            }
        }
    }

    private static void requireServerThread(Holder holder) {
        if (!holder.level().getServer().isSameThread()) {
            throw new IllegalStateException("an inventory operation runs on the server thread, not on " + Thread.currentThread().getName());
        }
    }

    /**
     * Where items are counted, taken and given: a value a system builds per call, which finds its handler when an
     * operation runs, so it stays valid while a chest is broken or a capability invalidated. A holder without a
     * handler counts nothing, matches nothing and takes every given stack as overflow. A {@link Player}'s counting and
     * taking read the main slots and the offhand, never the worn armor; a {@link Hand} is that one slot; an {@link Entity} is what the
     * game gives the entity, for a living one without a provider of its own its hands and armor, so a give to it
     * equips; a {@link Block}'s side bounds which slots are read and what may be put in through that face, while a
     * sided container may still refuse to give up what it shows, so a count or a match can include what a pay then
     * answers false for.
     */
    public sealed interface Holder permits Holder.Player, Holder.Hand, Holder.Entity, Holder.Block {

        /** The level the holder is in; a holder is built on the server. */
        ServerLevel level();

        /** The handler counting and taking read, resolved now; empty when the holder has none. */
        Optional<ResourceHandler<ItemResource>> taking();

        /** Inserts up to the amount of the resource the holder's own way, answering what was inserted. */
        int insert(ItemResource resource, int amount, TransactionContext transaction);

        /** Drops the stack at the holder, after a commit; false when the game refused and the items are gone. */
        boolean drop(ItemStack stack);

        /** Who pays a fate here, for the creative rule; nobody for an entity or a block. */
        @Nullable ServerPlayer payer();

        /** A player: given through the game's own place-back order (a hand already holding the resource, then the main slots), the overflow tossed and targeted at them with no pickup delay, as {@code /give} does. */
        record Player(ServerPlayer player) implements Holder {

            @Override
            public ServerLevel level() {
                return player.level();
            }

            @Override
            public Optional<ResourceHandler<ItemResource>> taking() {
                PlayerInventoryWrapper wrapper = PlayerInventoryWrapper.of(player);
                return Optional.of(new CombinedResourceHandler<>(List.of(wrapper.getMainSlots(), wrapper.getHandSlot(InteractionHand.OFF_HAND))));
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                return PlayerInventoryWrapper.of(player).insert(resource, amount, transaction);
            }

            @Override
            public boolean drop(ItemStack stack) {
                return dropTargeted(player, stack);
            }

            @Override
            public ServerPlayer payer() {
                return player;
            }

            @Override
            public @NotNull String toString() {
                return "player " + player.getName().getString();
            }
        }

        /** One of a player's hands: counted and taken from that slot alone, given into it first and then as the player, dropped as the player. */
        record Hand(ServerPlayer player, InteractionHand hand) implements Holder {

            @Override
            public ServerLevel level() {
                return player.level();
            }

            @Override
            public Optional<ResourceHandler<ItemResource>> taking() {
                return Optional.of(PlayerInventoryWrapper.of(player).getHandSlot(hand));
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                PlayerInventoryWrapper wrapper = PlayerInventoryWrapper.of(player);
                int inHand = wrapper.getHandSlot(hand).insert(resource, amount, transaction);
                return inHand + wrapper.insert(resource, amount - inHand, transaction);
            }

            @Override
            public boolean drop(ItemStack stack) {
                return dropTargeted(player, stack);
            }

            @Override
            public ServerPlayer payer() {
                return player;
            }

            @Override
            public @NotNull String toString() {
                return hand.name().toLowerCase(java.util.Locale.ROOT) + " of player " + player.getName().getString();
            }
        }

        /** An entity other than a player, through {@code Capabilities.Item.ENTITY}; the overflow spawns at it. */
        record Entity(net.minecraft.world.entity.Entity entity) implements Holder {

            public Entity {
                if (entity instanceof net.minecraft.world.entity.player.Player) {
                    throw new IllegalArgumentException("a player is held as Inventory.Holder.Player, not as an entity: " + entity.getName().getString());
                }
                if (!(entity.level() instanceof ServerLevel)) {
                    throw new IllegalArgumentException("a holder is built on the server; " + entity + " is on a client level");
                }
            }

            @Override
            public ServerLevel level() {
                return (ServerLevel) entity.level();
            }

            @Override
            public Optional<ResourceHandler<ItemResource>> taking() {
                return Optional.ofNullable(entity.getCapability(Capabilities.Item.ENTITY));
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                return ResourceHandlerUtil.insertStacking(entity.getCapability(Capabilities.Item.ENTITY), resource, amount, transaction);
            }

            @Override
            public boolean drop(ItemStack stack) {
                return entity.spawnAtLocation(level(), stack) != null;
            }

            @Override
            public @Nullable ServerPlayer payer() {
                return null;
            }

            @Override
            public @NotNull String toString() {
                return entity.getType().toShortString() + " " + entity.getUUID();
            }
        }

        /** A block, through {@code Capabilities.Item.BLOCK} seen from the side, or from within for null; the overflow is an item entity at its centre with the default pickup delay. */
        record Block(ServerLevel level, BlockPos pos, @Nullable Direction side) implements Holder {

            @Override
            public Optional<ResourceHandler<ItemResource>> taking() {
                return Optional.ofNullable(level.getCapability(Capabilities.Item.BLOCK, pos, side));
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                return ResourceHandlerUtil.insertStacking(level.getCapability(Capabilities.Item.BLOCK, pos, side), resource, amount, transaction);
            }

            @Override
            public boolean drop(ItemStack stack) {
                ItemEntity dropped = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                dropped.setDefaultPickUpDelay();
                return level.addFreshEntity(dropped);
            }

            @Override
            public @Nullable ServerPlayer payer() {
                return null;
            }

            @Override
            public @NotNull String toString() {
                return "block at " + pos.toShortString() + (side == null ? "" : " from " + side.getSerializedName());
            }
        }

        /** The player's own toss, then no pickup delay and the entity targeted at them, so only they pick it up and as soon as there is room. */
        private static boolean dropTargeted(ServerPlayer player, ItemStack stack) {
            ItemEntity dropped = player.drop(stack, false);
            if (dropped == null) {
                return false;
            }
            dropped.setNoPickUpDelay();
            dropped.setTarget(player.getUUID());
            return true;
        }
    }

    /**
     * What a match found: its holder and, per input in order, the resource and the count of it available to that
     * input; {@link #stacks()} are what a host puts into its loot context, so hooks and conditions read what was
     * matched. Built and paid in one call on the server thread.
     */
    public record Match(Holder holder, List<Found> found) {

        public Match {
            found = List.copyOf(found);
        }

        /** The matched stacks, one per input in order, fresh copies with the count available to each. */
        public List<ItemStack> stacks() {
            return found.stream().map(Found::stack).toList();
        }

        /** One input's find: the resource and the count of it this input could use. */
        public record Found(ItemInput input, ItemResource resource, int available) {

            public ItemStack stack() {
                return resource.toStack(available);
            }
        }
    }

    private record Worn(Match.Found found, int index) {
    }
}
