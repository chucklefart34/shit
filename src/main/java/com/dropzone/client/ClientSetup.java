package com.dropzone.client;

import com.dropzone.DropZone;
import com.dropzone.entity.CannonFireballEntity;
import com.dropzone.entity.GrenadeEntity;
import com.dropzone.entity.RocketEntity;
import com.dropzone.item.GunItem;
import com.dropzone.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import com.dropzone.registry.ModBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = DropZone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private static GliderModel gliderModel;

    private static SaddleCannonModel saddleModel;

    public static SaddleCannonModel saddleModel() {
        if (saddleModel == null) {
            saddleModel = new SaddleCannonModel(Minecraft.getInstance().getEntityModels().bakeLayer(SaddleCannonModel.LAYER));
        }
        return saddleModel;
    }

    public static GliderModel gliderModel() {
        if (gliderModel == null) {
            gliderModel = new GliderModel(Minecraft.getInstance().getEntityModels().bakeLayer(GliderModel.LAYER));
        }
        return gliderModel;
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModBlocks.STORAGE_BOX_MENU.get(), StorageBoxScreen::new));
        GunItem.clientGameTime = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.level != null ? mc.level.getGameTime() : 0L;
        };
    }

    /** Swap the carved block's placeholder model for the slicing model. */
    @SubscribeEvent
    public static void onModelBake(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        var key = new net.minecraft.client.resources.model.ModelResourceLocation(
                new net.minecraft.resources.ResourceLocation(DropZone.MODID, "carved_block"), "");
        var original = event.getModels().get(key);
        if (original != null) event.getModels().put(key, new CarvedBakedModel(original));
    }

    /** Grass, leaves etc. keep their biome tint when carved. */
    @SubscribeEvent
    public static void onBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof com.dropzone.hammer.CarvedBlockEntity c) {
                return Minecraft.getInstance().getBlockColors().getColor(c.source(), level, pos, tint);
            }
            return -1;
        }, ModBlocks.CARVED_BLOCK.get());
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(Keys.RELOAD);
        event.register(Keys.EMOTE_WHEEL);
        event.register(Keys.CANNON_MODE);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROCKET.get(), ctx -> new ThrownItemRenderer<RocketEntity>(ctx, 1.4F, true));
        event.registerEntityRenderer(ModEntities.GRENADE.get(), ctx -> new ThrownItemRenderer<GrenadeEntity>(ctx, 0.75F, false));
        event.registerEntityRenderer(ModEntities.CANNON_FIREBALL.get(), ctx -> new ThrownItemRenderer<CannonFireballEntity>(ctx, 3.0F, true));
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GliderModel.LAYER, GliderModel::createLayer);
        event.registerLayerDefinition(SaddleCannonModel.LAYER, SaddleCannonModel::createLayer);
    }

    @SubscribeEvent
    public static void onResourceReload(EntityRenderersEvent.AddLayers event) {
        gliderModel = null; // re-bake after resource reloads
        saddleModel = null;
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("scope", HudOverlays.SCOPE_OVERLAY);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "ammo", HudOverlays.AMMO);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "cannon", HudOverlays.CANNON);
        event.registerAboveAll("day_counter", HudOverlays.DAY);
    }

    private ClientSetup() {}
}
