package com.dropzone.registry;

import com.dropzone.DropZone;
import com.dropzone.hammer.CarvedBlock;
import com.dropzone.hammer.CarvedBlockEntity;
import com.dropzone.storage.BoxTier;
import com.dropzone.storage.BoxUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import com.dropzone.storage.StorageBoxBlock;
import com.dropzone.storage.StorageBoxBlockEntity;
import com.dropzone.storage.StorageBoxMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DropZone.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DropZone.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DropZone.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DropZone.MODID);

    public static final RegistryObject<StorageBoxBlock> DIAMOND_BOX = box(BoxTier.DIAMOND, MapColor.DIAMOND, null);
    public static final RegistryObject<StorageBoxBlock> GOLD_BOX = box(BoxTier.GOLD, MapColor.GOLD, DIAMOND_BOX);
    public static final RegistryObject<StorageBoxBlock> IRON_BOX = box(BoxTier.IRON, MapColor.METAL, GOLD_BOX);
    public static final RegistryObject<StorageBoxBlock> COPPER_BOX = box(BoxTier.COPPER, MapColor.COLOR_ORANGE, IRON_BOX);

    /** Hammer-carved eighths. Deliberately has no item. */
    public static final RegistryObject<CarvedBlock> CARVED_BLOCK = BLOCKS.register("carved_block",
            () -> new CarvedBlock(BlockBehaviour.Properties.of().strength(1.5F).noOcclusion().dynamicShape()));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CarvedBlockEntity>> CARVED_BE = BLOCK_ENTITIES.register("carved_block",
            () -> BlockEntityType.Builder.of(CarvedBlockEntity::new, CARVED_BLOCK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<StorageBoxBlockEntity>> STORAGE_BOX_BE = BLOCK_ENTITIES.register("storage_box",
            () -> BlockEntityType.Builder.of(StorageBoxBlockEntity::new,
                    COPPER_BOX.get(), IRON_BOX.get(), GOLD_BOX.get(), DIAMOND_BOX.get()).build(null));

    public static final RegistryObject<MenuType<StorageBoxMenu>> STORAGE_BOX_MENU = MENUS.register("storage_box",
            () -> IForgeMenuType.create(StorageBoxMenu::new));

    public static final RegistryObject<RecipeSerializer<BoxUpgradeRecipe>> BOX_UPGRADE_RECIPE = RECIPES.register("box_upgrade",
            () -> new SimpleCraftingRecipeSerializer<>(BoxUpgradeRecipe::new));

    private static RegistryObject<StorageBoxBlock> box(BoxTier tier, MapColor color, Supplier<? extends Block> next) {
        return BLOCKS.register(tier.id + "_storage_box", () -> new StorageBoxBlock(tier, next,
                BlockBehaviour.Properties.of().mapColor(color).strength(2.5F, 6.0F).sound(SoundType.COPPER)));
    }

    private ModBlocks() {}
}
