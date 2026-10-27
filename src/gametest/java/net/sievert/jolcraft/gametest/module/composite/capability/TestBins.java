package net.sievert.jolcraft.gametest.module.composite.capability;

import java.util.Iterator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.gametest.JolCraftGameTests;
import net.sievert.jolcraft.module.composite.capability.Inventory;

/**
 * The game-test mod's bin: a block whose block entity is a three-slot container and nothing else, which NeoForge
 * gives no item handler of its own, so the game-test mod exposes it through the inventory composite in its root.
 */
public final class TestBins {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JolCraftGameTests.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JolCraftGameTests.MOD_ID);
    public static final DeferredBlock<BinBlock> BIN_BLOCK = BLOCKS.registerBlock("bin", BinBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BinBlockEntity>> BIN = BLOCK_ENTITIES.register("bin",
            () -> new BlockEntityType<>(BinBlockEntity::new, BIN_BLOCK.get()));

    private TestBins() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void declare(Inventory inventory) {
        inventory.exposeBlockEntity(BIN);
    }

    public static final class BinBlock extends BaseEntityBlock {

        private static final MapCodec<BinBlock> CODEC = simpleCodec(BinBlock::new);

        BinBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected @NotNull MapCodec<BinBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BinBlockEntity(pos, state);
        }
    }

    /** Three slots over a {@link SimpleContainer}; a plain {@link BlockEntity} that is a {@link Container}, as a module's station might be. */
    public static final class BinBlockEntity extends BlockEntity implements Container {

        public static final int SLOTS = 3;

        private final SimpleContainer items = new SimpleContainer(SLOTS);

        BinBlockEntity(BlockPos pos, BlockState state) {
            super(BIN.get(), pos, state);
        }

        @Override
        public int getContainerSize() {
            return SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return items.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return items.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.setItem(slot, stack);
            setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            items.clearContent();
        }

        @Override
        public @NotNull Iterator<ItemStack> iterator() {
            return items.iterator();
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            ContainerHelper.saveAllItems(output, items.getItems());
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            ContainerHelper.loadAllItems(input, items.getItems());
        }
    }
}
