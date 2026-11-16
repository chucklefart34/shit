package com.dropzone;

import com.dropzone.network.DZNetwork;
import com.dropzone.registry.ModBlocks;
import com.dropzone.registry.ModEntities;
import com.dropzone.registry.ModItems;
import com.dropzone.registry.ModLoot;
import com.dropzone.registry.ModSounds;
import com.dropzone.registry.ModTabs;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DropZone.MODID)
public class DropZone {
    public static final String MODID = "dropzone";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DropZone() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModBlocks.MENUS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModTabs.TABS.register(modBus);
        ModLoot.LOOT_MODIFIERS.register(modBus);

        modBus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DZConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(DZNetwork::register);
    }
}
