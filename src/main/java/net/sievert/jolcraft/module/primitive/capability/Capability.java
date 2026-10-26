package net.sievert.jolcraft.module.primitive.capability;

import java.util.function.Supplier;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * What the mod's things expose through the game's capabilities: an item handler on a bench's block entity, a
 * value on an entity, a behaviour on an item. A module declares its providers while it is built; the root calls
 * {@link #register(IEventBus)} once and the game asks the providers from then on. Looking a capability up is the
 * game's own: {@code entity.getCapability(...)}, {@code level.getCapability(...)}, {@code stack.getCapability(...)}.
 */
@Primitive(Dictionary.CAPABILITY)
public interface Capability extends Module {

    /** A provider for the block entities of a type; the type is asked for when the game registers, so a holder of the mod's own is fine. Only while modules are built. */
    <T, C, B extends BlockEntity> void blockEntity(BlockCapability<T, C> capability, Supplier<? extends BlockEntityType<@NotNull B>> type, ICapabilityProvider<? super B, C, T> provider);

    /** A provider for the entities of a type; the type is asked for when the game registers, so a holder of the mod's own is fine. Only while modules are built. */
    <T, C, E extends Entity> void entity(EntityCapability<T, C> capability, Supplier<? extends EntityType<@NotNull E>> type, ICapabilityProvider<? super E, C, T> provider);

    /** A provider for stacks of the items. Only while modules are built. */
    <T, C> void item(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items);

    /** Registers every declared provider with the game. The root calls it once. */
    void register(IEventBus modBus);
}
