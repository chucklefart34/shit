package com.dropzone.client;

import com.dropzone.emote.Emote;
import com.dropzone.network.C2SEmote;
import com.dropzone.network.DZNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Radial emote picker. Hold the key, point, release to play. Tap the key to keep it open and click instead.
 * Number keys 1-8 also work; clicking the centre stops the current emote.
 */
public class EmoteWheelScreen extends Screen {
    private static final Emote[] EMOTES = Emote.values();
    private static final float SEG = (float) (Math.PI * 2.0D / EMOTES.length);

    private int hovered = -1;
    private boolean centerHovered;
    private int ticksOpen;
    private boolean clickMode;

    public EmoteWheelScreen() {
        super(Component.translatable("screen.dropzone.emotes"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ticksOpen++;
        if (clickMode || minecraft == null) return;
        InputConstants.Key key = Keys.EMOTE_WHEEL.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM) {
            clickMode = true;
            return;
        }
        boolean down = InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
        if (!down) {
            if (ticksOpen <= 5 && hovered < 0) clickMode = true; // quick tap: stay open
            else if (hovered >= 0) select(hovered);
            else onClose();
        }
    }

    private float outerRadius() {
        return Math.min(width, height) * 0.36F;
    }

    private float innerRadius() {
        return outerRadius() * 0.38F;
    }

    private void updateHover(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0D;
        double dy = mouseY - height / 2.0D;
        double dist = Math.sqrt(dx * dx + dy * dy);
        centerHovered = dist < innerRadius();
        if (dist < innerRadius() * 0.75D) {
            hovered = -1;
            return;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D; // 0 = straight up, clockwise
        if (angle < 0) angle += Math.PI * 2.0D;
        hovered = (int) Math.floor((angle + SEG / 2.0D) / SEG) % EMOTES.length;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateHover(mouseX, mouseY);
        float cx = width / 2.0F;
        float cy = height / 2.0F;
        float outer = outerRadius();
        float inner = innerRadius();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mat = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float gap = 0.025F;
        for (int i = 0; i < EMOTES.length; i++) {
            boolean hot = i == hovered;
            int color = hot ? 0xE03B82F6 : 0xB0101418;
            float r = hot ? outer + 6 : outer;
            float a0 = i * SEG - SEG / 2.0F - (float) Math.PI / 2.0F + gap;
            float a1 = a0 + SEG - gap * 2.0F;
            int steps = 10;
            for (int s = 0; s < steps; s++) {
                float b0 = Mth.lerp(s / (float) steps, a0, a1);
                float b1 = Mth.lerp((s + 1) / (float) steps, a0, a1);
                vertex(buf, mat, cx, cy, inner, b0, color);
                vertex(buf, mat, cx, cy, r, b0, color);
                vertex(buf, mat, cx, cy, r, b1, color);
                vertex(buf, mat, cx, cy, inner, b1, color);
            }
        }
        // Centre disc.
        int centerColor = centerHovered ? 0xE0B91C1C : 0xB0101418;
        int steps = 32;
        for (int s = 0; s < steps; s++) {
            float b0 = (float) (s * Math.PI * 2.0D / steps);
            float b1 = (float) ((s + 1) * Math.PI * 2.0D / steps);
            vertex(buf, mat, cx, cy, 0, b0, centerColor);
            vertex(buf, mat, cx, cy, inner - 4, b0, centerColor);
            vertex(buf, mat, cx, cy, inner - 4, b1, centerColor);
            vertex(buf, mat, cx, cy, 0, b1, centerColor);
        }
        Tesselator.getInstance().end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        for (int i = 0; i < EMOTES.length; i++) {
            float mid = i * SEG - (float) Math.PI / 2.0F;
            float lr = (inner + outer) / 2.0F;
            int x = Math.round(cx + Mth.cos(mid) * lr);
            int y = Math.round(cy + Mth.sin(mid) * lr);
            int textColor = i == hovered ? 0xFFFFFF : 0xD0D0D0;
            g.drawCenteredString(font, Component.translatable(EMOTES[i].translationKey()), x, y - 6, textColor);
            g.drawCenteredString(font, Component.literal(String.valueOf(i + 1)), x, y + 5, 0x808080);
        }

        Component center = hovered >= 0 ? Component.translatable(EMOTES[hovered].translationKey())
                : Component.translatable(centerHovered ? "screen.dropzone.emotes.stop" : "screen.dropzone.emotes");
        g.drawCenteredString(font, center, (int) cx, (int) cy - 4, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private static void vertex(BufferBuilder buf, Matrix4f mat, float cx, float cy, float radius, float angle, int argb) {
        float x = cx + Mth.cos(angle) * radius;
        float y = cy + Mth.sin(angle) * radius;
        buf.vertex(mat, x, y, 0.0F)
                .color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .endVertex();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        updateHover(mouseX, mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hovered >= 0) {
                select(hovered);
                return true;
            }
            if (centerHovered) {
                DZNetwork.sendToServer(new C2SEmote(-1));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + EMOTES.length) {
            select(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        if (clickMode && ticksOpen > 2 && Keys.EMOTE_WHEEL.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void select(int index) {
        DZNetwork.sendToServer(new C2SEmote(EMOTES[index].id()));
        onClose();
    }
}
