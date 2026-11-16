package com.dropzone.storage;

import com.dropzone.registry.ModBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Storage box + its next upgrade, anywhere in the grid = next-tier box with the same contents and name. */
public class BoxUpgradeRecipe extends CustomRecipe {
    public BoxUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private record Parts(ItemStack box, BoxUpgradeItem upgrade) {}

    private static Parts find(CraftingContainer grid) {
        ItemStack box = ItemStack.EMPTY;
        BoxUpgradeItem upgrade = null;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof StorageBoxItem && box.isEmpty()) box = s;
            else if (s.getItem() instanceof BoxUpgradeItem u && upgrade == null) upgrade = u;
            else return null;
        }
        if (box.isEmpty() || upgrade == null) return null;
        StorageBoxBlock block = (StorageBoxBlock) ((StorageBoxItem) box.getItem()).getBlock();
        return block.tier() == upgrade.from() ? new Parts(box, upgrade) : null;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return find(grid) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        Parts parts = find(grid);
        if (parts == null) return ItemStack.EMPTY;
        Block next = switch (parts.upgrade.from()) {
            case COPPER -> ModBlocks.IRON_BOX.get();
            case IRON -> ModBlocks.GOLD_BOX.get();
            case GOLD -> ModBlocks.DIAMOND_BOX.get();
            case DIAMOND -> null;
        };
        if (next == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(next);
        CompoundTag tag = parts.box.getTag();
        if (tag != null) out.setTag(tag.copy());
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModBlocks.BOX_UPGRADE_RECIPE.get();
    }
}
