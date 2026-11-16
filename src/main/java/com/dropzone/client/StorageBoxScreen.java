package com.dropzone.client;

import com.dropzone.network.C2SBoxClick;
import com.dropzone.network.DZNetwork;
import com.dropzone.storage.BoxEntry;
import com.dropzone.storage.StorageBoxMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Scrolling 9-wide grid of stored item types with big counts, over the normal player inventory. */
public class StorageBoxScreen extends AbstractContainerScreen<StorageBoxMenu> {
    private static final ResourceLocation BG = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int GRID_X = 8, GRID_Y = 18;

    private int scrollRow;

    public StorageBoxScreen(StorageBoxMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + StorageBoxMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("⇩"), b -> send(-1, StorageBoxMenu.DEPOSIT_ALL))
                .bounds(leftPos + imageWidth - 19, topPos + 4, 12, 11)
                .tooltip(Tooltip.create(Component.translatable("screen.dropzone.box.deposit")))
                .build());
    }

    private List<BoxEntry> entries() {
        return menu.clientEntries();
    }

    private int maxScroll() {
        int rows = Mth.positiveCeilDiv(Math.max(entries().size() + 1, menu.tier().types), StorageBoxMenu.COLS);
        return Math.max(0, rows - StorageBoxMenu.ROWS);
    }

    /** Entry index under the mouse, -1 for an empty cell, -2 if not over the grid. */
    private int cellAt(double mx, double my) {
        int x = (int) Math.floor(mx) - leftPos - GRID_X;
        int y = (int) Math.floor(my) - topPos - GRID_Y;
        if (x < 0 || y < 0 || x >= StorageBoxMenu.COLS * 18 || y >= StorageBoxMenu.ROWS * 18) return -2;
        int idx = (y / 18 + scrollRow) * StorageBoxMenu.COLS + x / 18;
        return idx < entries().size() ? idx : -1;
    }

    private void send(int index, int action) {
        DZNetwork.sendToServer(new C2SBoxClick(menu.containerId, index, action));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int rowsH = StorageBoxMenu.ROWS * 18 + 17;
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, rowsH);
        g.blit(BG, leftPos, topPos + rowsH, 0, 126, imageWidth, 96);

        // scrollbar beside the grid
        int max = maxScroll();
        int trackX = leftPos + imageWidth + 1;
        int trackY = topPos + GRID_Y;
        int trackH = StorageBoxMenu.ROWS * 18;
        g.fill(trackX, trackY, trackX + 5, trackY + trackH, 0xFF373737);
        int knobH = max == 0 ? trackH : Math.max(10, trackH * StorageBoxMenu.ROWS / (StorageBoxMenu.ROWS + max));
        int knobY = max == 0 ? trackY : trackY + (trackH - knobH) * scrollRow / max;
        g.fill(trackX + 1, knobY, trackX + 4, knobY + knobH, 0xFFC6C6C6);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        List<BoxEntry> list = entries();
        int capacity = menu.tier().types;
        int hovered = cellAt(mouseX, mouseY);
        for (int r = 0; r < StorageBoxMenu.ROWS; r++) {
            for (int c = 0; c < StorageBoxMenu.COLS; c++) {
                int idx = (r + scrollRow) * StorageBoxMenu.COLS + c;
                int x = leftPos + GRID_X + c * 18;
                int y = topPos + GRID_Y + r * 18;
                if (idx >= capacity) {
                    g.fill(x, y, x + 16, y + 16, 0xA0303030); // beyond this tier's type limit
                    continue;
                }
                if (idx < list.size()) {
                    BoxEntry e = list.get(idx);
                    g.renderItem(e.proto, x, y);
                    g.renderItemDecorations(font, e.proto, x, y, shortCount(e.count));
                }
                if (idx == hoveredCellIndex(mouseX, mouseY)) {
                    g.fillGradient(x, y, x + 16, y + 16, 300, 0x80FFFFFF, 0x80FFFFFF);
                }
            }
        }

        if (hovered >= 0 && menu.getCarried().isEmpty()) {
            BoxEntry e = list.get(hovered);
            List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(minecraft, e.proto));
            tip.add(Component.translatable("screen.dropzone.box.stored", String.format(Locale.ROOT, "%,d", e.count),
                    String.format(Locale.ROOT, "%,d", menu.tier().perType)).withStyle(ChatFormatting.YELLOW));
            tip.add(Component.translatable("screen.dropzone.box.help").withStyle(ChatFormatting.DARK_GRAY));
            g.renderTooltip(font, tip, Optional.empty(), mouseX, mouseY);
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }

    private int hoveredCellIndex(double mx, double my) {
        int x = (int) Math.floor(mx) - leftPos - GRID_X;
        int y = (int) Math.floor(my) - topPos - GRID_Y;
        if (x < 0 || y < 0 || x >= StorageBoxMenu.COLS * 18 || y >= StorageBoxMenu.ROWS * 18) return -1;
        return (y / 18 + scrollRow) * StorageBoxMenu.COLS + x / 18;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        String types = entries().size() + "/" + menu.tier().types;
        g.drawString(font, types, imageWidth - 22 - font.width(types), titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int cell = cellAt(mx, my);
        if (cell != -2) {
            if (hoveredCellIndex(mx, my) >= menu.tier().types) return true;
            int action = hasShiftDown() ? StorageBoxMenu.SHIFT
                    : button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? StorageBoxMenu.RIGHT : StorageBoxMenu.LEFT;
            if (action == StorageBoxMenu.SHIFT && cell < 0) return true;
            send(cell, action);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, maxScroll());
        return true;
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        // the scrollbar sits just outside the panel; don't treat it as "drop the held item"
        if (mx >= left + imageWidth && mx <= left + imageWidth + 7 && my >= top + GRID_Y && my <= top + GRID_Y + StorageBoxMenu.ROWS * 18) {
            return false;
        }
        return super.hasClickedOutside(mx, my, left, top, button);
    }

    static String shortCount(int n) {
        if (n < 1000) return Integer.toString(n);
        if (n < 10000) {
            String s = String.format(Locale.ROOT, "%.1fk", n / 1000.0D);
            return s.endsWith(".0k") ? (n / 1000) + "k" : s;
        }
        return (n / 1000) + "k";
    }
}
