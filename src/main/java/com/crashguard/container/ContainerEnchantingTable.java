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

        this.addSlotToContainer(new Slot(tile, 0, 44, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.isItemValidForSlot(0, stack);
            }
        });

        this.addSlotToContainer(new Slot(tile, 1, 80, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemEnchantedBook;
            }
        });

        this.addSlotToContainer(new Slot(tile, 2, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

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
        Slot slot = slotId >= 0 ? this.inventorySlots.get(slotId) : null;
        // 数字键 + 输出槽：兼容处理
        if (clickType == ClickType.SWAP && slot != null && slot.inventory == tile && slot.getSlotIndex() == 2) {
            // 先清空材料
            tile.onTakeOutput();
            // 执行原 SWAP 逻辑
            ItemStack output = slot.getStack().copy();
            slot.putStack(ItemStack.EMPTY);
            ItemStack hotbarStack = player.inventory.getStackInSlot(dragType);
            player.inventory.setInventorySlotContents(dragType, output);
            slot.putStack(hotbarStack);
            return output;
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            originalStack = slotStack.copy();

            if (index == 2) {
                ItemStack outputCopy = slotStack.copy();
                slot.putStack(ItemStack.EMPTY);
                if (this.mergeItemStack(outputCopy, 3, 39, true)) {
                    tile.onTakeOutput();
                    slot.onSlotChange(slotStack, originalStack);
                    return outputCopy;
                }
                slot.putStack(outputCopy);
                return ItemStack.EMPTY;
            } else if (index == 0 || index == 1) {
                if (!this.mergeItemStack(slotStack, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
        }

        return originalStack;
    }
}