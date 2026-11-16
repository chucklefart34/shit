package com.dropzone.registry;

import com.dropzone.DropZone;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DropZone.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dropzone"))
            .icon(() -> new ItemStack(ModItems.ASSAULT_RIFLE.get()))
            .displayItems((params, out) -> ModItems.creativeOrder().forEach(item -> out.accept(item.get())))
            .build());

    private ModTabs() {}
}
