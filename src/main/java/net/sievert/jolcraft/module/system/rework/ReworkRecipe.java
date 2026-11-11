package net.sievert.jolcraft.module.system.rework;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.util.text.Dictionary;
import net.sievert.jolcraft.util.text.ModString;

/**
 * A shapeless crafting recipe that reworks one base item into its result. The base stack's patch, what it changed
 * against its item's defaults, removals included, is carried over: whole, only the types {@code keep} names (none when
 * the list is empty), or all but those {@code remove} names. The result template's own components then go on over it,
 * as {@link ItemStackTemplate#apply} puts them. A craft whose single result is the base unchanged is no match, as in
 * the game's transmute, and so is a result the game refuses. The base's crafting remainder is given back unless
 * {@code base_remainder} is false. The game decodes the recipe and calls it, from the crafting table and the crafter;
 * design/rework.md.
 */
@Actor
public final class ReworkRecipe extends NormalCraftingRecipe {

    public static final MapCodec<ReworkRecipe> CODEC = RecordCodecBuilder.<ReworkRecipe>mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            Ingredient.CODEC.fieldOf(Dictionary.BASE).forGetter(ReworkRecipe::base),
            // The grid's size is the game's to set, so it is read when a recipe is first read, as the shapeless codec reads it.
            Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(0, ShapedRecipePattern.getMaxWidth() * ShapedRecipePattern.getMaxHeight() - 1))
                    .optionalFieldOf(Dictionary.INGREDIENTS, List.of()).forGetter(ReworkRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf(Dictionary.RESULT).forGetter(ReworkRecipe::result),
            DataComponentType.PERSISTENT_CODEC.listOf().optionalFieldOf(Dictionary.KEEP).forGetter(ReworkRecipe::keep),
            DataComponentType.PERSISTENT_CODEC.listOf().optionalFieldOf(Dictionary.REMOVE, List.of()).forGetter(ReworkRecipe::remove),
            Codec.BOOL.optionalFieldOf(ModString.underscored(Dictionary.BASE, Dictionary.REMAINDER), true).forGetter(ReworkRecipe::baseRemainder)
    ).apply(instance, ReworkRecipe::new)).validate(ReworkRecipe::checked);

    private final Ingredient base;
    private final List<Ingredient> ingredients;
    private final ItemStackTemplate result;
    private final Optional<List<DataComponentType<?>>> keep;
    private final List<DataComponentType<?>> remove;
    private final boolean baseRemainder;
    /** The base first, then the other ingredients: what the stacks are assigned to, and what the recipe book places. */
    private final List<Ingredient> all;

    public ReworkRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, Ingredient base, List<Ingredient> ingredients,
                        ItemStackTemplate result, Optional<List<DataComponentType<?>>> keep, List<DataComponentType<?>> remove, boolean baseRemainder) {
        super(commonInfo, bookInfo);
        this.base = base;
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
        this.keep = keep.map(List::copyOf);
        this.remove = List.copyOf(remove);
        this.baseRemainder = baseRemainder;
        List<Ingredient> assigned = new ArrayList<>(this.ingredients.size() + 1);
        assigned.add(base);
        assigned.addAll(this.ingredients);
        this.all = List.copyOf(assigned);
    }

    /** What the codec refuses beyond each field's own rule: a list of kept types beside a list of removed ones. */
    private static DataResult<ReworkRecipe> checked(ReworkRecipe recipe) {
        return recipe.keep.isPresent() && !recipe.remove.isEmpty()
                ? DataResult.error(() -> Dictionary.KEEP + " and " + Dictionary.REMOVE + " cannot be used together")
                : DataResult.success(recipe);
    }

    public Ingredient base() {
        return base;
    }

    public List<Ingredient> ingredients() {
        return ingredients;
    }

    public ItemStackTemplate result() {
        return result;
    }

    /** The only component types carried over, when present; an empty list carries nothing. */
    public Optional<List<DataComponentType<?>>> keep() {
        return keep;
    }

    /** The component types not carried over, when {@link #keep()} is absent. */
    public List<DataComponentType<?>> remove() {
        return remove;
    }

    /** Whether the base's crafting remainder is given back, as every other stack's is. */
    public boolean baseRemainder() {
        return baseRemainder;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int slot = baseSlot(input);
        if (slot < 0) {
            return false;
        }
        ItemStack baseStack = input.getItem(slot);
        ItemStack made = made(baseStack);
        return !made.isEmpty() && !(made.getCount() == 1 && ItemStack.isSameItemSameComponents(baseStack, made));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        int slot = baseSlot(input);
        return slot < 0 ? ItemStack.EMPTY : made(input.getItem(slot));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainders = CraftingRecipe.defaultCraftingReminder(input);
        if (!baseRemainder) {
            int slot = baseSlot(input);
            if (slot >= 0) {
                remainders.set(slot, ItemStack.EMPTY);
            }
        }
        return remainders;
    }

    /**
     * The input slot of the stack assigned to the base, or -1 when the grid holds another count of stacks or they
     * cannot be assigned. The matcher answers one assignment, the same for the same grid, so matching, assembling and
     * the remainders agree on the base; its indices count the non-empty stacks and are mapped back to the slots here.
     */
    private int baseSlot(CraftingInput input) {
        if (input.ingredientCount() != all.size()) {
            return -1;
        }
        List<ItemStack> stacks = new ArrayList<>(all.size());
        int[] slots = new int[all.size()];
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
                slots[stacks.size()] = slot;
                stacks.add(stack);
            }
        }
        int[] assigned = RecipeMatcher.findMatches(stacks, all);
        if (assigned == null) {
            return -1;
        }
        for (int index = 0; index < assigned.length; index++) {
            if (assigned[index] == 0) {
                return slots[index];
            }
        }
        return -1;
    }

    /**
     * The result for a base stack: the carried patch, then the template's own components, made as the template makes a
     * stack. Checked without the template's warning, since the carry comes from a player's stack and every change of the
     * grid asks: a stack the game refuses is no result.
     */
    private ItemStack made(ItemStack baseStack) {
        ItemStack made = new ItemStack(result.item(), result.count(), carried(baseStack.getComponentsPatch()));
        made.applyComponents(result.components());
        return ItemStack.validateStrict(made).isSuccess() ? made : ItemStack.EMPTY;
    }

    private DataComponentPatch carried(DataComponentPatch patch) {
        if (keep.isPresent()) {
            List<DataComponentType<?>> kept = keep.get();
            return patch.forget(type -> !kept.contains(type));
        }
        return remove.isEmpty() ? patch : patch.forget(remove::contains);
    }

    @Override
    public RecipeSerializer<ReworkRecipe> getSerializer() {
        return ReworkContent.REWORK.get();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(all);
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(all.stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
