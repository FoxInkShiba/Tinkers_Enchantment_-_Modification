package com.crashguard.tileentity;

import com.crashguard.config.ConfigHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class TileEntityAdvancedEnchantingTable extends TileEntity implements IInventory, ITickable {

    private NonNullList<ItemStack> inventory = NonNullList.withSize(328, ItemStack.EMPTY);

    private static final int SLOT_MATERIAL_TOP = 0;
    private static final int SLOT_TOOL = 1;
    private static final int SLOT_MATERIAL_BOTTOM = 2;
    private static final int SLOT_OUTPUT = 3;
    private static final int BOOK_START = 4;
    private static final int BOOK_SLOT_COUNT = 324;

    private static final int GUI_ROWS = 3;
    private static final int TOTAL_ROWS = 36;
    private static final int COLUMNS = 9;

    private int scrollRow = 0;
    private ItemStack lastTool = ItemStack.EMPTY;
    private boolean isUpdating = false;
    private boolean pendingClear = false;

    private static class EnchantBoost {
        int boost;
        int maxLevel;

        EnchantBoost(int boost, int maxLevel) {
            this.boost = boost;
            this.maxLevel = maxLevel;
        }
    }

    public void triggerClear() {
        pendingClear = true;
    }

    public void resetScrollRow() {
        scrollRow = 0;
    }

    public void syncToClient() {
        if (world != null && !world.isRemote) {
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Override
    public void update() {
        if (world.isRemote) return;
        if (isUpdating) return;

        if (pendingClear) {
            pendingClear = false;
            inventory.set(SLOT_TOOL, ItemStack.EMPTY);
            inventory.set(SLOT_MATERIAL_TOP, ItemStack.EMPTY);
            inventory.set(SLOT_MATERIAL_BOTTOM, ItemStack.EMPTY);
            for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
                inventory.set(i, ItemStack.EMPTY);
            }
            scrollRow = 0;
            lastTool = ItemStack.EMPTY;
            updateOutput();
            markDirty();
            syncToClient();
        }

        ItemStack tool = getStackInSlot(SLOT_TOOL);

        if (!tool.isEmpty()) {
            if (!ItemStack.areItemsEqual(tool, lastTool)) {
                refreshBooksFromTool(tool);
                lastTool = tool.copy();
                updateOutput();
                markDirty();
            }
        } else {
            if (!lastTool.isEmpty()) {
                clearAllBooks();
                lastTool = ItemStack.EMPTY;
                updateOutput();
                markDirty();
            }
        }
    }

    private void refreshBooksFromTool(ItemStack tool) {
        isUpdating = true;

        for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
            inventory.set(i, ItemStack.EMPTY);
        }

        if (tool.isEmpty()) {
            isUpdating = false;
            return;
        }

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            isUpdating = false;
            return;
        }

        NBTBase enchBase = rootTag.getTag("ench");
        NBTTagList enchList = new NBTTagList();

        if (enchBase instanceof NBTTagList) {
            enchList = (NBTTagList) enchBase;
        } else if (enchBase instanceof NBTTagCompound) {
            enchList.appendTag((NBTTagCompound) enchBase);
        }

        if (enchList.tagCount() == 0) {
            isUpdating = false;
            return;
        }

        int index = 0;
        for (int i = 0; i < enchList.tagCount() && index < BOOK_SLOT_COUNT; i++) {
            NBTTagCompound tag = enchList.getCompoundTagAt(i);
            Enchantment ench = null;
            int lvl = 0;

            if (tag.hasKey("id", Constants.NBT.TAG_STRING)) {
                String id = tag.getString("id");
                ench = Enchantment.getEnchantmentByLocation(id);
                lvl = tag.getInteger("lvl");
            } else if (tag.hasKey("id", Constants.NBT.TAG_SHORT)) {
                int id = tag.getShort("id");
                ench = Enchantment.getEnchantmentByID(id);
                lvl = tag.getShort("lvl");
            }

            if (ench != null) {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(book, new net.minecraft.enchantment.EnchantmentData(ench, lvl));
                inventory.set(BOOK_START + index, book);
                index++;
            }
        }

        isUpdating = false;
    }

    private void updateOutput() {
        ItemStack tool = getStackInSlot(SLOT_TOOL);
        if (tool.isEmpty()) {
            inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        // 收集加成物品（只读，不消耗）
        List<EnchantBoost> boosts = new ArrayList<>();
        int finalMaxLevel = 0;

        ItemStack materialTop = getStackInSlot(SLOT_MATERIAL_TOP);
        if (!materialTop.isEmpty()) {
            String itemId = materialTop.getItem().getRegistryName().toString();
            ConfigHandler.EnchantBoostInfo info = ConfigHandler.enchantBoostItems.get(itemId);
            if (info != null) {
                int count = materialTop.getCount();
                for (int i = 0; i < count; i++) {
                    boosts.add(new EnchantBoost(info.boost, info.maxLevel));
                }
                finalMaxLevel = Math.max(finalMaxLevel, info.maxLevel);
            }
        }

        ItemStack materialBottom = getStackInSlot(SLOT_MATERIAL_BOTTOM);
        if (!materialBottom.isEmpty()) {
            String itemId = materialBottom.getItem().getRegistryName().toString();
            ConfigHandler.EnchantBoostInfo info = ConfigHandler.enchantBoostItems.get(itemId);
            if (info != null) {
                int count = materialBottom.getCount();
                for (int i = 0; i < count; i++) {
                    boosts.add(new EnchantBoost(info.boost, info.maxLevel));
                }
                finalMaxLevel = Math.max(finalMaxLevel, info.maxLevel);
            }
        }

        // 按 maxLevel 从小到大排序（上限小的先加）
        boosts.sort(Comparator.comparingInt(b -> b.maxLevel));

        // 计算输出槽工具
        ItemStack output = tool.copy();
        if (!boosts.isEmpty()) {
            NBTTagCompound rootTag = output.getTagCompound();
            if (rootTag == null) {
                rootTag = new NBTTagCompound();
                output.setTagCompound(rootTag);
            }

            NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
            if (enchList.tagCount() > 0) {
                // 解析附魔到 Map
                Map<Enchantment, Integer> enchantmentMap = new HashMap<>();
                for (int i = 0; i < enchList.tagCount(); i++) {
                    NBTTagCompound tag = enchList.getCompoundTagAt(i);
                    int id = tag.getShort("id");
                    int lvl = tag.getShort("lvl");
                    Enchantment ench = Enchantment.getEnchantmentByID(id);
                    if (ench != null) {
                        enchantmentMap.put(ench, lvl);
                    }
                }

                // 依次应用加成
                for (EnchantBoost boost : boosts) {
                    for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                        int newLevel = entry.getValue() + boost.boost;
                        newLevel = Math.min(newLevel, boost.maxLevel);
                        enchantmentMap.put(entry.getKey(), newLevel);
                    }
                }

                // 应用最终上限（所有物品 maxLevel 的最大值）
                if (finalMaxLevel > 0) {
                    for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                        int newLevel = Math.min(entry.getValue(), finalMaxLevel);
                        enchantmentMap.put(entry.getKey(), newLevel);
                    }
                }

                // 再应用全局附魔上限
                for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                    int globalMax = ConfigHandler.getEnchantmentMaxLevel(entry.getKey().getRegistryName().toString());
                    int newLevel = Math.min(entry.getValue(), globalMax);
                    enchantmentMap.put(entry.getKey(), newLevel);
                }

                // 重建附魔列表
                NBTTagList newEnchList = new NBTTagList();
                for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
                    tag.setShort("lvl", entry.getValue().shortValue());
                    newEnchList.appendTag(tag);
                }
                rootTag.setTag("ench", newEnchList);
            }
        }

        inventory.set(SLOT_OUTPUT, output);
    }

    private void clearAllBooks() {
        for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
            inventory.set(i, ItemStack.EMPTY);
        }
        scrollRow = 0;
    }

    private int applyLevelLimit(Enchantment ench, int level) {
        String enchId = ench.getRegistryName().toString();
        int maxLevel = ConfigHandler.getEnchantmentMaxLevel(enchId);
        if (level > maxLevel) return maxLevel;
        if (level > 32767) return 32767;
        return level;
    }

    private NBTTagList buildEnchantmentListFromBooks() {
        Map<Enchantment, Integer> enchantmentMap = new HashMap<>();

        boolean allowAdditive = ConfigHandler.enchantmentAllowAdditive;
        String mode = ConfigHandler.enchantmentMode;
        boolean protectExisting = ConfigHandler.protectExistingEnchantment;

        for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
            ItemStack book = inventory.get(i);
            if (book.isEmpty() || !(book.getItem() instanceof ItemEnchantedBook)) continue;

            NBTTagList enchants = ItemEnchantedBook.getEnchantments(book);
            for (int j = 0; j < enchants.tagCount(); j++) {
                NBTTagCompound enchTag = enchants.getCompoundTagAt(j);
                Enchantment ench = null;
                int lvl = 0;

                if (enchTag.hasKey("id", Constants.NBT.TAG_STRING)) {
                    String id = enchTag.getString("id");
                    ench = Enchantment.getEnchantmentByLocation(id);
                    lvl = enchTag.getInteger("lvl");
                } else if (enchTag.hasKey("id", Constants.NBT.TAG_SHORT)) {
                    int id = enchTag.getShort("id");
                    ench = Enchantment.getEnchantmentByID(id);
                    lvl = enchTag.getShort("lvl");
                }

                if (ench == null) continue;

                if (!allowAdditive) {
                    if (protectExisting && enchantmentMap.containsKey(ench)) {
                        int existingLevel = enchantmentMap.get(ench);
                        if (existingLevel >= lvl) continue;
                    }
                    int existing = enchantmentMap.getOrDefault(ench, 0);
                    enchantmentMap.put(ench, Math.max(existing, lvl));
                } else if ("separate".equals(mode)) {
                    enchantmentMap.put(ench, lvl);
                } else {
                    int existing = enchantmentMap.getOrDefault(ench, 0);
                    enchantmentMap.put(ench, existing + lvl);
                }
            }
        }

        NBTTagList newEnchList = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
            int finalLevel = applyLevelLimit(entry.getKey(), entry.getValue());
            NBTTagCompound enchTag = new NBTTagCompound();
            enchTag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            enchTag.setShort("lvl", (short) finalLevel);
            newEnchList.appendTag(enchTag);
        }
        return newEnchList;
    }

    private void updateToolEnchantments() {
        if (world.isRemote) return;

        ItemStack tool = getStackInSlot(SLOT_TOOL);
        if (tool.isEmpty()) return;

        NBTTagList newEnchList = buildEnchantmentListFromBooks();

        ItemStack result = tool.copy();
        NBTTagCompound rootTag = result.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            result.setTagCompound(rootTag);
        }

        if (newEnchList.tagCount() > 0) {
            rootTag.setTag("ench", newEnchList);
        } else {
            rootTag.removeTag("ench");
        }

        inventory.set(SLOT_TOOL, result);
        updateOutput();
        lastTool = result.copy();
        markDirty();
    }

    public void onBookInventoryChanged() {
        if (isUpdating) return;
        isUpdating = true;
        updateToolEnchantments();
        isUpdating = false;
    }

    public void scrollUp() {
        if (scrollRow - 3 >= 0) {
            scrollRow -= 3;
            markDirty();
            syncToClient();
        }
    }

    public void scrollDown() {
        if (scrollRow + GUI_ROWS + 3 <= TOTAL_ROWS) {
            scrollRow += 3;
            markDirty();
            syncToClient();
        }
    }

    public int getScrollRow() { return scrollRow; }
    public int getMaxScrollRow() { return TOTAL_ROWS - GUI_ROWS; }

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

    @Override
    public int getSizeInventory() { return 328; }

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
        if (index == SLOT_OUTPUT) {
            ItemStack output = inventory.get(SLOT_OUTPUT);
            if (!output.isEmpty()) {
                ItemStack result = output.copy();
                inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                // 取出输出槽时，真正消耗材料槽的物品
                inventory.set(SLOT_MATERIAL_TOP, ItemStack.EMPTY);
                inventory.set(SLOT_MATERIAL_BOTTOM, ItemStack.EMPTY);
                markDirty();
                triggerClear();
                return result;
            }
            return ItemStack.EMPTY;
        }

        ItemStack result = ItemStackHelper.getAndSplit(inventory, index, count);
        if (index >= BOOK_START && !result.isEmpty()) {
            onBookInventoryChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index == SLOT_OUTPUT) {
            ItemStack output = inventory.get(SLOT_OUTPUT);
            if (!output.isEmpty()) {
                ItemStack result = output.copy();
                inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                inventory.set(SLOT_MATERIAL_TOP, ItemStack.EMPTY);
                inventory.set(SLOT_MATERIAL_BOTTOM, ItemStack.EMPTY);
                markDirty();
                triggerClear();
                return result;
            }
            return ItemStack.EMPTY;
        }

        ItemStack s = inventory.get(index);
        if (!s.isEmpty()) {
            inventory.set(index, ItemStack.EMPTY);
            if (index >= BOOK_START) {
                onBookInventoryChanged();
            }
            return s;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inventory.set(index, stack);
        if (index == SLOT_MATERIAL_TOP || index == SLOT_MATERIAL_BOTTOM) {
            // 材料槽变化时，重新计算输出槽
            updateOutput();
            markDirty();
            syncToClient();
        }
        if (index == SLOT_TOOL) {
            refreshBooksFromTool(stack);
            lastTool = stack.copy();
            updateOutput();
        }
        if (index >= BOOK_START) {
            onBookInventoryChanged();
        }
        markDirty();
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
        if (index == SLOT_TOOL) {
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
        if (index == SLOT_OUTPUT) {
            return false;
        }
        if (index >= BOOK_START) {
            return stack.getItem() instanceof ItemEnchantedBook;
        }
        return true;
    }

    @Override
    public int getField(int id) {
        return id == 0 ? scrollRow : getMaxScrollRow();
    }

    @Override
    public void setField(int id, int value) {
        if (id == 0) {
            scrollRow = Math.max(0, Math.min(value, getMaxScrollRow()));
            syncToClient();
        }
    }

    @Override
    public int getFieldCount() { return 2; }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public String getName() { return "Advanced Enchanting Table"; }

    @Override
    public boolean hasCustomName() { return false; }

    @Override
    public ITextComponent getDisplayName() { return new TextComponentString("Advanced Enchanting Table"); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, inventory);
        compound.setInteger("ScrollRow", scrollRow);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, inventory);
        scrollRow = compound.getInteger("ScrollRow");

        int maxRow = getMaxScrollRow();
        if (scrollRow > maxRow) scrollRow = maxRow;
        if (scrollRow < 0) scrollRow = 0;

        if (!getStackInSlot(SLOT_TOOL).isEmpty()) {
            lastTool = getStackInSlot(SLOT_TOOL).copy();
        }
    }
}