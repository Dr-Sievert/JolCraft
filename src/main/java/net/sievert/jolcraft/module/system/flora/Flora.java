package net.sievert.jolcraft.module.system.flora;

import java.util.List;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.Nullable;

import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.tab.Tab;
import net.sievert.jolcraft.module.system.catalog.CatalogKeys;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The plants and their ground. On {@link FloraTags#VERDANT} blocks a plant ignores its light rule and grows faster,
 * answered through the four {@link GrowthEvent} kinds the crop and mushroom mixins post. The listeners read only the
 * level the event hands them, so they are right on a worldgen thread and on the client, where placement is predicted.
 * An item with an entry in {@link FloraData#PLANTS} plants its block when used on one, on both sides: the client
 * consumes the click and shows the block with the server's update, the server places it. A block with an entry in
 * {@link FloraData#BONEMEAL} grows more when bonemeal is used on it. The blocks and the tags that
 * say what stands on them are {@link FloraContent}'s and the tag contributor's; where a plant may stand is the block's
 * own {@code canSurvive}. Design in design/flora.md.
 */
@ModSystem(Dictionary.FLORA)
public final class Flora implements Module {

    /** The brightness a crop reads at least on verdant ground, enough to grow by. */
    public static final int VERDANT_LIGHT = 9;
    /** What a crop's growth speed is multiplied by on verdant ground. */
    public static final float VERDANT_SPEED = 1.5F;
    /** A mushroom's chance to spread on a random tick over verdant ground, in place of the game's 1 in 25. */
    public static final float VERDANT_SPREAD = 0.06F;
    /** A mushroom's chance to grow huge from bonemeal on verdant ground, in place of the game's 0.4. */
    public static final float VERDANT_BONEMEAL = 0.6F;

    private final Log log;

    public Flora(Tab tab, Log log) {
        this.log = log.tagged(this);
        tab.add(CatalogKeys.TAB, FloraContent.BARLEY_SEEDS, FloraContent.BARLEY, FloraContent.BARLEY_BLOCK_ITEM,
                FloraContent.ASGARNIAN_SEEDS, FloraContent.DUSKHOLD_SEEDS, FloraContent.KRANDONIAN_SEEDS, FloraContent.YANILLIAN_SEEDS,
                FloraContent.ASGARNIAN_HOPS, FloraContent.DUSKHOLD_HOPS, FloraContent.KRANDONIAN_HOPS, FloraContent.YANILLIAN_HOPS,
                FloraContent.SKYBELL_ITEM, FloraContent.CYANELLA_ITEM, FloraContent.BLOODROOT_ITEM,
                FloraContent.DUSKCAP_ITEM, FloraContent.DUSKCAP_BLOCK_ITEM, FloraContent.DUSKCAP_STEM_ITEM,
                FloraContent.FESTERLING_ITEM, FloraContent.FESTERLING_BLOCK_ITEM, FloraContent.FESTERLING_STEM_ITEM,
                FloraContent.DEEPSLATE_BULBS, FloraContent.VERDANT_SOIL_ITEM, FloraContent.VERDANT_FARMLAND_ITEM);
    }

    /** Adds the system's listeners to the game bus. */
    public void register(IEventBus gameBus) {
        gameBus.addListener(this::onLight);
        gameBus.addListener(this::onSpeed);
        gameBus.addListener(this::onSpread);
        gameBus.addListener(this::onBonemeal);
        gameBus.addListener(this::onUseItemOnBlock);
        gameBus.addListener(this::onBonemealUsed);
    }

    private static boolean verdant(GrowthEvent event) {
        return event.below().is(FloraTags.VERDANT);
    }

    private void onLight(GrowthEvent.Light event) {
        if (verdant(event)) {
            event.atLeast(VERDANT_LIGHT);
        }
    }

    private void onSpeed(GrowthEvent.Speed event) {
        if (verdant(event)) {
            event.scale(VERDANT_SPEED);
        }
    }

    private void onSpread(GrowthEvent.Spread event) {
        if (verdant(event)) {
            event.setChance(VERDANT_SPREAD);
        }
    }

    private void onBonemeal(GrowthEvent.Bonemeal event) {
        if (verdant(event)) {
            event.setChance(VERDANT_BONEMEAL);
        }
    }

    /**
     * Plants the block of an item with a {@link FloraData#PLANTS} entry, where the item's own use would run, after the
     * block has had its turn. It asks what a block item asks, on both sides: the block is enabled, the place context can
     * take a block, the block has a placement state and the state survives there, unobstructed, and the player may use
     * the item there in adventure mode. When anything fails the event is left alone and the game goes on to the item's own use.
     * Cancelling the event skips what the game does after it and before a block item's own placement, in the stack's
     * {@code useOn} and {@code CommonHooks.onPlaceItemIntoWorld}: the adventure-mode check (asked above), the block-place
     * event that lets a protection mod refuse, and the item's use statistic, which this does itself on the server. The
     * client consumes the click and shows the block with the server's update.
     */
    private void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
            return;
        }
        ItemStack stack = event.getItemStack();
        FloraData.Plants plants = stack.typeHolder().getData(FloraData.PLANTS);
        if (plants == null) {
            return;
        }
        Level level = event.getLevel();
        Player player = event.getPlayer();
        UseOnContext context = event.getUseOnContext();
        Block block = plants.block();
        if (!block.isEnabled(level.enabledFeatures())) {
            return;
        }
        if (player != null && !player.getAbilities().mayBuild && !stack.canPlaceOnBlockInAdventureMode(new BlockInWorld(level, event.getPos(), false))) {
            return;
        }
        BlockPlaceContext placement = new BlockPlaceContext(context);
        if (!placement.canPlace()) {
            return;
        }
        BlockState state = block.getStateForPlacement(placement);
        BlockPos pos = placement.getClickedPos();
        if (state == null || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.placementContext(player))) {
            return;
        }
        if (level.isClientSide()) {
            event.cancelWithResult(InteractionResult.SUCCESS);
            return;
        }
        event.cancelWithResult(plant(level, player, stack, context.getClickedFace(), pos, state));
    }

    /** Places the block as a block item does, after the entity-place event; answers what the click came to. */
    private InteractionResult plant(Level level, @Nullable Player player, ItemStack stack, Direction face, BlockPos pos, BlockState state) {
        BlockSnapshot before = BlockSnapshot.create(level.dimension(), level, pos, Block.UPDATE_ALL_IMMEDIATE);
        if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }
        if (EventHooks.onBlockPlace(player, before, face)) {
            level.restoringBlockSnapshots = true;
            before.restore(before.getFlags() | Block.UPDATE_CLIENTS);
            level.restoringBlockSnapshots = false;
            log.debug("{} was refused planting {} at {}", name(player), state.getBlock().getDescriptionId(), pos);
            return InteractionResult.FAIL;
        }
        BlockState placed = level.getBlockState(pos);
        if (placed.is(state.getBlock())) {
            placed.getBlock().setPlacedBy(level, pos, placed, player, stack);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
            }
        }
        SoundType sound = placed.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placed));
        Item item = stack.getItem();
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(item));
        }
        stack.consume(1, player);
        log.debug("{} planted {} at {} with {}", name(player), placed.getBlock().getDescriptionId(), pos, item.getDescriptionId());
        return InteractionResult.SUCCESS;
    }

    /**
     * Grows what else the block's {@link FloraData#BONEMEAL} entry names when bonemeal is used on it, on the server and for
     * a valid target only: the game consumes nothing on an invalid one, and a row must not grow there for free. For each
     * row whose biome tag holds at the block, or that has none, the placed feature is placed at the position above the
     * block with the level's chunk generator and random, as the game's grass and nylium place theirs; the feature brings
     * its own chance, height and spread. It is placed without a biome check, so a row's feature carries no biome filter.
     * The event is left alone and the game's own bonemeal goes on after it, with one exception. The game asks the block
     * again whether it is a target after this event, and a sprout may have taken the room the block's own growth needs,
     * the cell above a grass block say: the game would then refuse the bonemeal, consume nothing, and the sprout would
     * have grown for free. In that case the bonemeal went into the sprout: it is consumed here and the event answered as
     * a success, which is what the game does for a use it accepts.
     */
    private void onBonemealUsed(BonemealEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.isValidBonemealTarget()) {
            return;
        }
        List<FloraData.Sprout> sprouts = event.getState().typeHolder().getData(FloraData.BONEMEAL);
        if (sprouts == null) {
            return;
        }
        BlockPos pos = event.getPos();
        Registry<PlacedFeature> features = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for (FloraData.Sprout sprout : sprouts) {
            if (sprout.biome().isPresent() && !level.getBiome(pos).is(sprout.biome().get())) {
                continue;
            }
            features.get(sprout.feature()).ifPresentOrElse(
                    feature -> feature.value().place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos.above()),
                    () -> log.warn("bonemeal on {} names the placed feature {}, which the level lacks", event.getState().getBlock().getDescriptionId(), sprout.feature().identifier()));
        }
        BlockState now = level.getBlockState(pos);
        if (!(now.getBlock() instanceof BonemealableBlock target && target.isValidBonemealTarget(level, pos, now))) {
            event.getStack().shrink(1);
            event.setSuccessful(true);
        }
    }

    private static String name(@Nullable Player player) {
        return player == null ? "no player" : player.getName().getString();
    }
}
