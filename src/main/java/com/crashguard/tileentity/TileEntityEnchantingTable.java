package com.crashguard.tileentity;

import com.crashguard.config.ConfigHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.Constants;

public class TileEntityEnchantingTable extends TileEntity implements IInventory, ITickable {

    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    private static final int TOOL_SLOT = 0;
    private static final int BOOK_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    private ItemStack lastTool = ItemStack.EMPTY;
    private ItemStack lastBook = ItemStack.EMPTY;
    private boolean isUpdating = false;
    private boolean pendingClear = false;

    public void triggerClear() {
        pendingClear = true;
    }

    @Override
    public void update() {
        if (world.isRemote) return;
        if (isUpdating) return;

        if (pendingClear) {
            pendingClear = false;
            inventory.set(TOOL_SLOT, ItemStack.EMPTY);
            inventory.set(BOOK_SLOT, ItemStack.EMPTY);
            inventory.set(OUTPUT_SLOT, ItemStack.EMPTY);
            lastTool = ItemStack.EMPTY;
            lastBook = ItemStack.EMPTY;
            markDirty();
            syncToClient();
        }

        ItemStack tool = getStackInSlot(TOOL_SLOT);
        ItemStack book = getStackInSlot(BOOK_SLOT);

        boolean toolChanged = !ItemStack.areItemsEqual(tool, lastTool);
        boolean bookChanged = !ItemStack.areItemsEqual(book, lastBook);

        if (toolChanged || bookChanged) {
            updatePreview();
            lastTool = tool.copy();
            lastBook = book.copy();
            markDirty();
        }
    }

    private void updatePreview() {
        ItemStack tool = getStackInSlot(TOOL_SLOT);
        ItemStack book = getStackInSlot(BOOK_SLOT);
        ItemStack output = ItemStack.EMPTY;

        if (!tool.isEmpty() && !book.isEmpty() && book.getItem() instanceof ItemEnchantedBook) {
            output = applyEnchantment(tool, book);
        }

        setInventorySlotContents(OUTPUT_SLOT, output);
    }

    private ItemStack applyEnchantment(ItemStack tool, ItemStack book) {
        ItemStack result = tool.copy();
        NBTTagCompound rootTag = result.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            result.setTagCompound(rootTag);
        }

        NBTTagList enchList;
        if (rootTag.hasKey("ench", Constants.NBT.TAG_LIST)) {
            enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
        } else {
            enchList = new NBTTagList();
        }

        NBTTagList bookEnchants = ((ItemEnchantedBook) book.getItem()).getEnchantments(book);

        for (int i = 0; i < bookEnchants.tagCount(); i++) {
            NBTTagCompound tag = bookEnchants.getCompoundTagAt(i);
            int id = tag.getShort("id");
            int lvl = tag.getShort("lvl");

            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench == null) continue;

            boolean found = false;
            for (int j = 0; j < enchList.tagCount(); j++) {
                NBTTagCompound entry = enchList.getCompoundTagAt(j);
                if (entry.getShort("id") == id) {
                    int newLevel = Math.max(entry.getShort("lvl"), lvl);
                    int maxLevel = ConfigHandler.getEnchantmentMaxLevel(ench.getRegistryName().toString());
                    newLevel = Math.min(newLevel, maxLevel);
                    entry.setShort("lvl", (short) newLevel);
                    found = true;
                    break;
                }
            }

            if (!found) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setShort("id", (short) id);
                int finalLevel = Math.min(lvl, ConfigHandler.getEnchantmentMaxLevel(ench.getRegistryName().toString()));
                entry.setShort("lvl", (short) finalLevel);
                enchList.appendTag(entry);
            }
        }

        if (enchList.tagCount() > 0) {
            rootTag.setTag("ench", enchList);
        } else {
            rootTag.removeTag("ench");
        }

        return result;
    }

    public void syncToClient() {
        if (world != null && !world.isRemote) {
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return new SPacketUpdateTileEntity(pos, 0, nbt);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    // ========== IInventory ==========
    @Override
    public int getSizeInventory() { return 3; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) { return inventory.get(index); }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index == OUTPUT_SLOT) {
            ItemStack output = getStackInSlot(OUTPUT_SLOT);
            if (!output.isEmpty()) {
                ItemStack result = output.copy();
                setInventorySlotContents(OUTPUT_SLOT, ItemStack.EMPTY);
                triggerClear();
                return result;
            }
            return ItemStack.EMPTY;
        }
        return ItemStackHelper.getAndSplit(inventory, index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index == OUTPUT_SLOT) {
            ItemStack output = getStackInSlot(OUTPUT_SLOT);
            if (!output.isEmpty()) {
                ItemStack result = output.copy();
                setInventorySlotContents(OUTPUT_SLOT, ItemStack.EMPTY);
                triggerClear();
                return result;
            }
            return ItemStack.EMPTY;
        }
        ItemStack s = inventory.get(index);
        if (!s.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
            if (index == TOOL_SLOT || index == BOOK_SLOT) {
                updatePreview();
            }
        }
        return s;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventory.set(index, stack);

        if (index == TOOL_SLOT || index == BOOK_SLOT) {
            updatePreview();
        }

        markDirty();
        syncToClient();
    }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(pos) <= 64;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == TOOL_SLOT) {
            if (ConfigHandler.advancedEnchantingTableOnlyTinker) {
                if (stack.getItem() instanceof slimeknights.tconstruct.library.tools.ToolCore) return true;
                try {
                    Class<?> armorCoreClass = Class.forName("c4.conarm.lib.armor.ArmorCore");
                    if (armorCoreClass.isInstance(stack.getItem())) return true;
                } catch (ClassNotFoundException ignored) {}
                return false;
            } else {
                return true;
            }
        }
        if (index == BOOK_SLOT) {
            return stack.getItem() instanceof ItemEnchantedBook;
        }
        if (index == OUTPUT_SLOT) {
            return false;
        }
        return true;
    }

    @Override
    public int getField(int id) { return 0; }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() { return 0; }

    @Override
    public void clear() { inventory.clear(); }

    @Override
    public String getName() { return "container.enchanting_table"; }

    @Override
    public boolean hasCustomName() { return false; }

    @Override
    public ITextComponent getDisplayName() { return new TextComponentString("Enchanting Table"); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, inventory);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory = NonNullList.withSize(3, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, inventory);
        updatePreview();
        if (!getStackInSlot(TOOL_SLOT).isEmpty()) {
            lastTool = getStackInSlot(TOOL_SLOT).copy();
        }
        if (!getStackInSlot(BOOK_SLOT).isEmpty()) {
            lastBook = getStackInSlot(BOOK_SLOT).copy();
        }
    }
}