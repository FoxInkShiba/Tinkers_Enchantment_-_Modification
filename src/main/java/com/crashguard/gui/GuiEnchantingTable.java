package com.crashguard.gui;

import com.crashguard.container.ContainerEnchantingTable;
import com.crashguard.tileentity.TileEntityEnchantingTable;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;

public class GuiEnchantingTable extends GuiContainer {

    private TileEntityEnchantingTable tileEntity;

    public GuiEnchantingTable(ContainerEnchantingTable container, TileEntityEnchantingTable tile) {
        super(container);
        this.tileEntity = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 整体背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFC6C6C6);

        // ========== 槽位格子 ==========
        // 工具槽
        drawSlot(guiLeft + 44, guiTop + 35);
        // 附魔书槽
        drawSlot(guiLeft + 80, guiTop + 35);
        // 输出槽
        drawSlot(guiLeft + 116, guiTop + 35);

        // 背包槽位（27个）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiLeft + 8 + col * 18, guiTop + 84 + row * 18);
            }
        }

        // 快捷栏（9个）
        for (int col = 0; col < 9; col++) {
            drawSlot(guiLeft + 8 + col * 18, guiTop + 142);
        }

        // ========== 文字 ==========
        fontRenderer.drawString("Enchanting Table", guiLeft + 8, guiTop + 6, 0x404040);
        fontRenderer.drawString("Inventory", guiLeft + 8, guiTop + 74, 0x404040);
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }
}