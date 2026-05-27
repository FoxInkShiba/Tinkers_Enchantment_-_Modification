package com.crashguard.container;

import com.crashguard.config.ConfigHandler;
import com.crashguard.tileentity.TileEntityAdvancedEnchantingTable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;

public class ContainerAdvancedEnchantingTable extends Container {

    private TileEntityAdvancedEnchantingTable tile;

    private static final int SLOT_MATERIAL_TOP = 0;
    private static final int SLOT_TOOL = 1;
    private static final int SLOT_MATERIAL_BOTTOM = 2;
    private static final int SLOT_OUTPUT = 3;
    private static final int SLOT_BOOK_START = 4;

    private static final int BOOK_SLOT_COUNT = 324;
    private static final int SLOT_BOOK_END = SLOT_BOOK_START + BOOK_SLOT_COUNT - 1;

    private int playerInvStartIndex;
    private int hotbarStartIndex;

    public ContainerAdvancedEnchantingTable(InventoryPlayer playerInv, TileEntityAdvancedEnchantingTable tile) {
        this.tile = tile;

        // 材料槽（上）
        this.addSlotToContainer(new Slot(tile, SLOT_MATERIAL_TOP, 8, 17));

        // 工具槽
        this.addSlotToContainer(new Slot(tile, SLOT_TOOL, 8, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(SLOT_TOOL, stack);
            }
        });

        // 材料槽（下）
        this.addSlotToContainer(new Slot(tile, SLOT_MATERIAL_BOTTOM, 8, 71));

        // 输出槽
        this.addSlotToContainer(new Slot(tile, SLOT_OUTPUT, 201, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                tile.triggerClear();
                return super.onTake(player, stack);
            }
        });

        // 附魔书槽位（324格，坐标占位）
        for (int i = 0; i < BOOK_SLOT_COUNT; i++) {
            final int slotIndex = SLOT_BOOK_START + i;
            this.addSlotToContainer(new Slot(tile, slotIndex, 0, 0) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemEnchantedBook;
                }
            });
        }

        // 记录玩家背包起始索引
        playerInvStartIndex = this.inventorySlots.size();
        // 玩家背包主区域 27格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 8 + col * 18;
                int y = 108 + row * 18;
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // 记录快捷栏起始索引
        hotbarStartIndex = this.inventorySlots.size();
        // 快捷栏 9格
        for (int i = 0; i < 9; i++) {
            int x = 8 + i * 18;
            int y = 166;
            this.addSlotToContainer(new Slot(playerInv, i, x, y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            originalStack = slotStack.copy();

            // 输出槽：Shift+左键 / 数字快捷键 / 鼠标拖拽
            if (index == SLOT_OUTPUT) {
                // 先复制一份
                ItemStack outputCopy = slotStack.copy();
                // 清空输出槽
                slot.putStack(ItemStack.EMPTY);
                // 尝试移到背包
                this.mergeItemStack(outputCopy, playerInvStartIndex, hotbarStartIndex + 9, true);
                // 强制再次清空输出槽（防止残留）
                if (!slot.getStack().isEmpty()) {
                    slot.putStack(ItemStack.EMPTY);
                }
                // 触发清空材料
                tile.triggerClear();
                return ItemStack.EMPTY;
            }
            // 附魔书槽位 -> 玩家背包
            else if (index >= SLOT_BOOK_START && index <= SLOT_BOOK_END) {
                if (!this.mergeItemStack(slotStack, playerInvStartIndex, hotbarStartIndex + 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 工具/材料槽 -> 玩家背包
            else if (index == SLOT_TOOL || index == SLOT_MATERIAL_TOP || index == SLOT_MATERIAL_BOTTOM) {
                if (!this.mergeItemStack(slotStack, playerInvStartIndex, hotbarStartIndex + 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 玩家背包 -> 附魔书槽位
            else if (index >= playerInvStartIndex && index < hotbarStartIndex + 9) {
                if (slotStack.getItem() instanceof ItemEnchantedBook) {
                    if (!this.mergeItemStack(slotStack, SLOT_BOOK_START, SLOT_BOOK_END + 1, false)) {
                        this.mergeItemStack(slotStack, SLOT_MATERIAL_TOP, SLOT_MATERIAL_BOTTOM + 1, false);
                    }
                } else {
                    this.mergeItemStack(slotStack, SLOT_MATERIAL_TOP, SLOT_MATERIAL_BOTTOM + 1, false);
                }
            }

            if (slotStack.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return originalStack;
    }

    public void updateBookSlotPositions(int scrollRow) {
        for (int i = 0; i < BOOK_SLOT_COUNT; i++) {
            Slot slot = this.inventorySlots.get(SLOT_BOOK_START + i);
            int row = i / 9;
            int col = i % 9;
            if (row >= scrollRow && row < scrollRow + 3) {
                slot.xPos = 29 + col * 18;
                slot.yPos = 17 + (row - scrollRow) * 18;
            } else {
                slot.xPos = -1000;
                slot.yPos = -1000;
            }
        }
    }
}