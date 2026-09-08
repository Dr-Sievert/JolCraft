package net.sievert.jolcraft.world.item.custom.container;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.sievert.jolcraft.util.client.JolCraftColors;
import net.sievert.jolcraft.world.item.component.JolCraftDataComponents;
import net.sievert.jolcraft.world.item.client.tooltip.coin.CoinPouchTooltip;
import net.sievert.jolcraft.world.item.JolCraftItems;
import net.sievert.jolcraft.world.sound.JolCraftSounds;
import net.sievert.jolcraft.world.sound.util.JolCraftSoundHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoinPouchItem extends Item {

    public static final int MAX_COINS = 999;

    private static final int BAR_COLOR = JolCraftColors.argb("E8C143");

    public CoinPouchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // Runs on both sides so the client can predict the stack move (vanilla BundleItem does the
    // same), but the sounds and the menu sync are server-only or they fire twice for the actor.
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack cursor, Slot slot, ClickAction action, Player player, SlotAccess access) {

        boolean server = !player.level().isClientSide;

        if (action == ClickAction.SECONDARY && cursor.isEmpty()) {
            int current = pouch.getOrDefault(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);
            if (current > 0) {
                int toGive = Math.min(64, current);
                ItemStack out = new ItemStack(JolCraftItems.GOLD_COIN.get(), toGive);
                pouch.set(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), current - toGive);
                access.set(out);

                if (server) {
                    if (current == 1) {
                        playPouchInsertSound(player);
                    } else {
                        playStackSound(player);
                    }

                    broadcastChangesOnContainerMenu(player);
                }

                player.awardStat(Stats.ITEM_USED.get(this));
                return true;
            }
        }

        ItemStack slotStack = access.get();

        if (isGoldCoin(slotStack)) {
            int current = pouch.getOrDefault(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);
            int addable = 0;
            if (action == ClickAction.PRIMARY) {
                addable = Math.min(MAX_COINS - current, slotStack.getCount());
            } else if (action == ClickAction.SECONDARY) {
                addable = Math.min(MAX_COINS - current, 1);
            }
            if (addable > 0) {
                boolean wasEmpty = (current == 0);
                pouch.set(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), current + addable);
                slotStack.shrink(addable);
                access.set(slotStack.isEmpty() ? ItemStack.EMPTY : slotStack);

                if (server) {
                    if (addable == 1 && wasEmpty) {
                        playPouchInsertSound(player);
                    } else if (addable == 1) {
                        playSingleSound(player);
                    } else {
                        playStackSound(player);
                    }

                    broadcastChangesOnContainerMenu(player);
                }
            } else if (server) {
                playInsertFailSound(player);
            }
            return true;
        }
        return false;
    }

    // Mutation and sound are server-only; the client just evaluates the same preconditions so the
    // arm swing matches. Without this it shrank its own coin stacks and called drop().
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pouch = player.getItemInHand(hand);
        int current = pouch.getOrDefault(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);

        if (player.isShiftKeyDown()) {
            if (current <= 0) {
                if (!level.isClientSide) playInsertFailSound(player);
                return InteractionResultHolder.pass(pouch);
            }

            if (level.isClientSide) return InteractionResultHolder.success(pouch);

            int toGive = Math.min(64, current);

            pouch.set(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), current - toGive);

            ItemStack out = new ItemStack(JolCraftItems.GOLD_COIN.get(), toGive);

            player.getInventory().add(out);
            if (!out.isEmpty()) {
                player.drop(out, false);
            }

            if (current == 1) {
                playPouchInsertSound(player);
            } else {
                playStackSound(player);
            }

            broadcastChangesOnContainerMenu(player);
            player.awardStat(Stats.ITEM_USED.get(this));

            return InteractionResultHolder.success(pouch);
        }

        int canAdd = MAX_COINS - current;

        if (canAdd <= 0 || !hasGoldCoinsInInventory(player)) {
            if (!level.isClientSide) playInsertFailSound(player);
            return InteractionResultHolder.pass(pouch);
        }

        if (level.isClientSide) return InteractionResultHolder.success(pouch);

        int added = tryConsumeGoldCoinsFromInventory(player, canAdd);
        if (added <= 0) {
            playInsertFailSound(player);
            return InteractionResultHolder.pass(pouch);
        }

        boolean wasEmpty = (current == 0);
        pouch.set(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), current + added);

        if (added == 1 && wasEmpty) {
            playPouchInsertSound(player);
        } else if (added == 1) {
            playSingleSound(player);
        } else {
            playStackSound(player);
        }

        broadcastChangesOnContainerMenu(player);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(pouch);
    }

    public static boolean isGoldCoin(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == JolCraftItems.GOLD_COIN.get();
    }

    // Read-only companion to tryConsumeGoldCoinsFromInventory so the client can predict use().
    private static boolean hasGoldCoinsInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isGoldCoin(player.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private int tryConsumeGoldCoinsFromInventory(Player player, int max) {
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (isGoldCoin(slotStack)) {
                int toTake = Math.min(slotStack.getCount(), max - removed);
                slotStack.shrink(toTake);
                removed += toTake;
                if (removed >= max) break;
            }
        }
        return removed;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int amount = stack.getOrDefault(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);
        return amount > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int amount = stack.getOrDefault(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);
        return Math.min(13, (int) ((amount / (float) MAX_COINS) * 13));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        int amount = stack.getOrDefault(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);
        return amount > 0
                ? Optional.of(new CoinPouchTooltip(amount))
                : Optional.empty();
    }

    private static void playStackSound(Player player) {
        JolCraftSoundHelper.player(
                player,
                JolCraftSounds.COIN_STACK.get(),
                0.8F + player.level().random.nextFloat() * 0.2F,
                1.0F + player.level().random.nextFloat() * 0.2F
        );
    }

    private static void playSingleSound(Player player) {
        JolCraftSoundHelper.player(
                player,
                JolCraftSounds.COIN_SINGLE.get(),
                0.8F + player.level().random.nextFloat() * 0.2F,
                1.0F + player.level().random.nextFloat() * 0.2F
        );
    }

    private static void playPouchInsertSound(Player player) {
        JolCraftSoundHelper.player(player, SoundEvents.BUNDLE_INSERT, 0.8F, 1.3F);
    }

    private void playInsertFailSound(Player player) {
        JolCraftSoundHelper.player(player, SoundEvents.BOOK_PUT, 0.4F, 1.3F);
    }


    // slotsChanged(playerInventory) only re-evaluates crafting results for the menu's own
    // container, so it never synced anything here. broadcastChanges() is what actually syncs.
    private void broadcastChangesOnContainerMenu(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        menu.broadcastChanges();
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        // Also reached via DwarfMerchantResultSlot, so only initialise a pouch that has no amount
        // yet; a recipe or trade handing out a filled one must not be silently emptied.
        if (stack.has(JolCraftDataComponents.COIN_POUCH_AMOUNT.get())) {
            return;
        }

        stack.set(JolCraftDataComponents.COIN_POUCH_AMOUNT.get(), 0);
    }

    public static int insertCoins(ItemStack pouch, int amount, Player player) {
        if (amount <= 0 || !pouch.is(JolCraftItems.COIN_POUCH.get())) {
            return 0;
        }

        int current = pouch.getOrDefault(
                JolCraftDataComponents.COIN_POUCH_AMOUNT.get(),
                0
        );

        int inserted = Math.min(amount, MAX_COINS - current);

        if (inserted <= 0) {
            return 0;
        }

        pouch.set(
                JolCraftDataComponents.COIN_POUCH_AMOUNT.get(),
                current + inserted
        );

        if (inserted == 1 && current == 0) {
            playPouchInsertSound(player);
        } else if (inserted == 1) {
            playSingleSound(player);
        } else {
            playStackSound(player);
        }

        return inserted;
    }
}
