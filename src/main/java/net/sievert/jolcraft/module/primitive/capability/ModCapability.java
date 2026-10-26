package net.sievert.jolcraft.module.primitive.capability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.stereotype.Implementation;

/** {@link Capability} over the game's capability registration: the providers are kept until the game asks for them. */
@Implementation
public final class ModCapability implements Capability {

    private final List<Consumer<RegisterCapabilitiesEvent>> declared = new ArrayList<>();
    private boolean registered;

    @Override
    public <T, C, B extends BlockEntity> void blockEntity(BlockCapability<T, C> capability, Supplier<? extends BlockEntityType<@NotNull B>> type, ICapabilityProvider<? super B, C, T> provider) {
        declare(event -> event.registerBlockEntity(capability, type.get(), provider));
    }

    @Override
    public <T, C, E extends Entity> void entity(EntityCapability<T, C> capability, Supplier<? extends EntityType<@NotNull E>> type, ICapabilityProvider<? super E, C, T> provider) {
        declare(event -> event.registerEntity(capability, type.get(), provider));
    }

    @Override
    public <T, C> void item(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items) {
        if (items.length == 0) {
            throw new IllegalArgumentException("an item capability provider needs at least one item");
        }
        declare(event -> event.registerItem(capability, provider, items));
    }

    private void declare(Consumer<RegisterCapabilitiesEvent> declaration) {
        if (registered) {
            throw new IllegalStateException("a capability provider was declared after the capabilities were registered");
        }
        declared.add(declaration);
    }

    @Override
    public void register(IEventBus modBus) {
        if (registered) {
            throw new IllegalStateException("the capabilities were registered twice");
        }
        registered = true;
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> declared.forEach(declaration -> declaration.accept(event)));
    }

    /** How many providers are declared, for the root's log line. */
    public int declared() {
        return declared.size();
    }
}
