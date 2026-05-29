package com.crashguard.container;

import com.crashguard.tileentity.TileEntityEnchantingTable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;

public class ContainerEnchantingTable extends Container {

    private TileEntityEnchantingTable tile;

    public ContainerEnchantingTable(InventoryPlayer playerInventory, TileEntityEnchantingTable tile) {
        this.tile = tile;

        // 工具槽
        this.addSlotToContainer(new Slot(tile, 0, 44, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(0, stack);
            }
        });

        // 附魔书槽
        this.addSlotToContainer(new Slot(tile, 1, 80, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemEnchantedBook;
            }
        });

        // 输出槽
        this.addSlotToContainer(new Slot(tile, 2, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });

        // 玩家背包 3x9
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 快捷栏 9
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        // 先让原版处理所有点击
        ItemStack result = super.slotClick(slotId, dragType, clickType, player);

        // 检查输出槽是否变空了（说明物品被取走了）
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            Slot slot = this.inventorySlots.get(slotId);
            if (slot != null && slot.inventory == tile && slot.getSlotIndex() == 2 && !slot.getHasStack()) {
                // 输出槽空了，触发清空工具槽和附魔书槽
                tile.triggerClear();
                detectAndSendChanges();
            }
        }

        return result;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            originalStack = slotStack.copy();

            // 输出槽（Shift+左键）
            if (index == 2) {
                if (!this.mergeItemStack(slotStack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.putStack(ItemStack.EMPTY);
                tile.triggerClear();
                slot.onSlotChange(slotStack, originalStack);
                detectAndSendChanges();
                return originalStack;
            }
            // 工具槽或附魔书槽 -> 玩家背包
            else if (index == 0 || index == 1) {
                if (!this.mergeItemStack(slotStack, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 玩家背包 -> 附魔书槽 或 工具槽
            else {
                if (slotStack.getItem() instanceof ItemEnchantedBook) {
                    if (!this.mergeItemStack(slotStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.mergeItemStack(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
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
            detectAndSendChanges();
        }

        return originalStack;
    }
}