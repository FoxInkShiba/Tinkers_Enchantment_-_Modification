package com.crashguard.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class BookInventoryWrapper implements IInventory {

    private NonNullList<ItemStack> bookList;

    public BookInventoryWrapper(NonNullList<ItemStack> list) {
        this.bookList = list;
    }

    @Override
    public int getSizeInventory() { return bookList.size(); }
    @Override
    public boolean isEmpty() { return false; }
    @Override
    public ItemStack getStackInSlot(int index) { return bookList.get(index); }
    @Override
    public ItemStack decrStackSize(int index, int count) { return ItemStack.EMPTY; }
    @Override
    public ItemStack removeStackFromSlot(int index) { return ItemStack.EMPTY; }
    @Override
    public void setInventorySlotContents(int index, ItemStack stack) { bookList.set(index, stack); }
    @Override
    public int getInventoryStackLimit() { return 64; }
    @Override
    public void markDirty() {}
    @Override
    public boolean isUsableByPlayer(EntityPlayer player) { return true; }
    @Override
    public void openInventory(EntityPlayer player) {}
    @Override
    public void closeInventory(EntityPlayer player) {}
    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) { return false; }
    @Override
    public int getField(int id) { return 0; }
    @Override
    public void setField(int id, int value) {}
    @Override
    public int getFieldCount() { return 0; }
    @Override
    public void clear() {}
    @Override
    public String getName() { return "books"; }
    @Override
    public boolean hasCustomName() { return false; }
    @Override
    public ITextComponent getDisplayName() { return new TextComponentString(""); }
}