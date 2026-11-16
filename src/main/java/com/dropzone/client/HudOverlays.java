package com.dropzone.client;

import com.dropzone.DZConfig;
import com.dropzone.item.GunItem;
import com.dropzone.saddle.CannonSaddle;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class HudOverlays {
    private static final ResourceLocation SCOPE = new ResourceLocation("minecraft", "textures/misc/spyglass_scope.png");

    /** Magazine / reserve counter beside the hotbar. */
    public static final IGuiOverlay AMMO = (ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) return;

        int x = width / 2 + 98;
        int y = height - 20;
        g.renderItem(new ItemStack(gun.stats().ammo().get()), x, y - 1);
        Component text;
        int color;
        if (GunItem.isReloading(stack)) {
            text = Component.translatable("hud.dropzone.reloading");
            color = 0xFFAA00;
        } else {
            int mag = ClientEvents.displayedAmmo(stack, gun);
            String reserve = GunItem.hasInfiniteAmmo(player) ? "∞" : Integer.toString(gun.countReserve(player, stack));
            text = Component.literal(mag + " / " + reserve);
            color = mag == 0 ? 0xFF5555 : mag <= gun.stats().magSize() / 4 ? 0xFFFF55 : 0xFFFFFF;
        }
        g.drawString(mc.font, text, x + 19, y + 4, color, true);
    };

    /** Full-screen scope when aiming a sniper. */
    public static final IGuiOverlay SCOPE_OVERLAY = (ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) -> {
        if (!ClientEvents.isScoped()) return;
        RenderSystem.enableBlend();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        g.blit(SCOPE, x, y, -90, 0.0F, 0.0F, size, size, size, size);
        g.fill(0, 0, width, y, -90, 0xFF000000);
        g.fill(0, y + size, width, height, -90, 0xFF000000);
        g.fill(0, y, x, y + size, -90, 0xFF000000);
        g.fill(x + size, y, width, y + size, -90, 0xFF000000);
        int cx = width / 2;
        int cy = height / 2;
        g.fill(cx - size / 3, cy, cx + size / 3, cy + 1, 0xC0000000);
        g.fill(cx, cy - size / 3, cx + 1, cy + size / 3, 0xC0000000);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFF2020);
        RenderSystem.disableBlend();
    };

    /** "Day N" in the top-left corner. */
    public static final IGuiOverlay DAY = (ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui || mc.options.renderDebug || !DZConfig.DAY_HUD.get()) return;
        long day = mc.level.getDayTime() / 24000L + 1L;
        Component text = Component.translatable("hud.dropzone.day", day);
        int w = mc.font.width(text);
        g.fill(2, 2, 2 + w + 6, 2 + 13, 0x80000000);
        g.drawString(mc.font, text, 5, 5, 0xFFD966, true);
    };

    /** Cannon mode and reload bar while riding a saddled mob. */
    public static final IGuiOverlay CANNON = (ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        Entity mount = player.getVehicle();
        if (mount == null || !CannonSaddle.has(mount) || mount.getFirstPassenger() != player) return;
        boolean destroy = CannonSaddle.destructive(mount);
        Component mode = Component.translatable(destroy ? "hud.dropzone.cannon_destroy" : "hud.dropzone.cannon_damage")
                .withStyle(destroy ? ChatFormatting.RED : ChatFormatting.GOLD);
        Component line = Component.translatable("hud.dropzone.cannon", mode, Keys.CANNON_MODE.getTranslatedKeyMessage());
        int y = height - 59;
        g.drawCenteredString(mc.font, line, width / 2, y, 0xFFFFFF);
        float ready = ClientEvents.cannonReadiness();
        int barW = 60;
        int x0 = width / 2 - barW / 2;
        g.fill(x0, y + 11, x0 + barW, y + 13, 0xA0000000);
        g.fill(x0, y + 11, x0 + Math.round(barW * ready), y + 13, ready >= 1.0F ? 0xFFFF7A1A : 0xFF8A5A3A);
    };

    private HudOverlays() {}
}
