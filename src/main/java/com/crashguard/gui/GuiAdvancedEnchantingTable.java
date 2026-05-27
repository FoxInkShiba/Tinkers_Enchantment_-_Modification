package com.crashguard.gui;

import com.crashguard.container.ContainerAdvancedEnchantingTable;
import com.crashguard.tileentity.TileEntityAdvancedEnchantingTable;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;

public class GuiAdvancedEnchantingTable extends GuiContainer {

    private TileEntityAdvancedEnchantingTable tile;
    private ContainerAdvancedEnchantingTable container;

    private static final int BUTTON_SIZE = 18;
    private static final int PREV_BTN_X = 201;
    private static final int PREV_BTN_Y = 17;
    private static final int NEXT_BTN_X = 201;
    private static final int NEXT_BTN_Y = 71;

    private boolean hoverPrev = false;
    private boolean hoverNext = false;

    public GuiAdvancedEnchantingTable(ContainerAdvancedEnchantingTable container, TileEntityAdvancedEnchantingTable tile) {
        super(container);
        this.tile = tile;
        this.container = container;
        this.xSize = 220;
        this.ySize = 200;
    }

    @Override
    public void initGui() {
        super.initGui();
        updateSlotPositions();
    }

    private void updateSlotPositions() {
        int scrollRow = tile.getScrollRow();
        container.updateBookSlotPositions(scrollRow);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 整体背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFC6C6C6);

        // 附魔书区域背景
        drawRect(guiLeft + 29, guiTop + 17, guiLeft + 29 + 162, guiTop + 17 + 54, 0xFF8B8B8B);

        // 背包区域背景
        drawRect(guiLeft + 8, guiTop + 108, guiLeft + 8 + 162, guiTop + 108 + 54, 0xFF8B8B8B);

        // ========== 槽位格子 ==========
        // 固定槽位
        drawSlot(guiLeft + 8, guiTop + 17);   // 材料槽（上）
        drawSlot(guiLeft + 8, guiTop + 44);   // 工具槽
        drawSlot(guiLeft + 8, guiTop + 71);   // 材料槽（下）
        drawSlot(guiLeft + 201, guiTop + 44); // 输出槽

        // 附魔书槽位（27个）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiLeft + 29 + col * 18, guiTop + 17 + row * 18);
            }
        }

        // 背包槽位（27个）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiLeft + 8 + col * 18, guiTop + 108 + row * 18);
            }
        }

        // 快捷栏（9个）
        for (int col = 0; col < 9; col++) {
            drawSlot(guiLeft + 8 + col * 18, guiTop + 166);
        }

        // ========== 文字 ==========
        fontRenderer.drawString("Material", guiLeft + 8, guiTop + 7, 0x404040);
        fontRenderer.drawString("Tool", guiLeft + 8, guiTop + 34, 0x404040);
        fontRenderer.drawString("Material", guiLeft + 8, guiTop + 61, 0x404040);
        fontRenderer.drawString("Output", guiLeft + 201, guiTop + 34, 0x404040);
        fontRenderer.drawString("Enchantments", guiLeft + 29, guiTop + 7, 0x404040);
        fontRenderer.drawString("Inventory", guiLeft + 8, guiTop + 98, 0x404040);

        // ========== 页码 ==========
        int scrollRow = tile.getScrollRow();
        int maxRow = tile.getMaxScrollRow();
        int currentPage = scrollRow / 3 + 1;
        int totalPages = maxRow / 3 + 1;
        String pageText = currentPage + "/" + totalPages;
        int pageTextWidth = fontRenderer.getStringWidth(pageText);
        fontRenderer.drawString(pageText, guiLeft + 210 - pageTextWidth / 2, guiTop + 95, 0x888888);

        // ========== 滚动按钮 ==========
        // 向上
        drawRect(guiLeft + PREV_BTN_X, guiTop + PREV_BTN_Y,
                guiLeft + PREV_BTN_X + BUTTON_SIZE, guiTop + PREV_BTN_Y + BUTTON_SIZE,
                hoverPrev ? 0xFFAAAAAA : 0xFF666666);
        drawRect(guiLeft + PREV_BTN_X + 1, guiTop + PREV_BTN_Y + 1,
                guiLeft + PREV_BTN_X + BUTTON_SIZE - 1, guiTop + PREV_BTN_Y + BUTTON_SIZE - 1,
                hoverPrev ? 0xFF444444 : 0xFF222222);

        // 向下
        drawRect(guiLeft + NEXT_BTN_X, guiTop + NEXT_BTN_Y,
                guiLeft + NEXT_BTN_X + BUTTON_SIZE, guiTop + NEXT_BTN_Y + BUTTON_SIZE,
                hoverNext ? 0xFFAAAAAA : 0xFF666666);
        drawRect(guiLeft + NEXT_BTN_X + 1, guiTop + NEXT_BTN_Y + 1,
                guiLeft + NEXT_BTN_X + BUTTON_SIZE - 1, guiTop + NEXT_BTN_Y + BUTTON_SIZE - 1,
                hoverNext ? 0xFF444444 : 0xFF222222);

        // 按钮箭头
        fontRenderer.drawString("▲", guiLeft + PREV_BTN_X + 5, guiTop + PREV_BTN_Y + 4, hoverPrev ? 0xFFFFAA : 0xFFFFFF);
        fontRenderer.drawString("▼", guiLeft + NEXT_BTN_X + 5, guiTop + NEXT_BTN_Y + 4, hoverNext ? 0xFFFFAA : 0xFFFFFF);
    }

    private void drawSlot(int x, int y) {
        // 边框
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        // 内部
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        // 左上高光
        drawRect(x + 1, y + 1, x + 17, y + 2, 0xFFA0A0A0);
        drawRect(x + 1, y + 1, x + 2, y + 17, 0xFFA0A0A0);
        // 右下阴影
        drawRect(x + 16, y + 2, x + 17, y + 17, 0xFF555555);
        drawRect(x + 2, y + 16, x + 17, y + 17, 0xFF555555);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;

        hoverPrev = relX >= PREV_BTN_X && relX <= PREV_BTN_X + BUTTON_SIZE &&
                relY >= PREV_BTN_Y && relY <= PREV_BTN_Y + BUTTON_SIZE;
        hoverNext = relX >= NEXT_BTN_X && relX <= NEXT_BTN_X + BUTTON_SIZE &&
                relY >= NEXT_BTN_Y && relY <= NEXT_BTN_Y + BUTTON_SIZE;

        if (hoverPrev) {
            drawHoveringText("Scroll Up", relX, relY);
        } else if (hoverNext) {
            drawHoveringText("Scroll Down", relX, relY);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;

        if (mouseButton == 0) {
            if (relX >= PREV_BTN_X && relX <= PREV_BTN_X + BUTTON_SIZE &&
                    relY >= PREV_BTN_Y && relY <= PREV_BTN_Y + BUTTON_SIZE) {
                tile.scrollUp();
                updateSlotPositions();
                return;
            }
            if (relX >= NEXT_BTN_X && relX <= NEXT_BTN_X + BUTTON_SIZE &&
                    relY >= NEXT_BTN_Y && relY <= NEXT_BTN_Y + BUTTON_SIZE) {
                tile.scrollDown();
                updateSlotPositions();
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }
}