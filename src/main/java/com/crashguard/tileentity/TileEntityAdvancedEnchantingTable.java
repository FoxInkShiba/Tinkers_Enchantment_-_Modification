package com.crashguard.tileentity;

import com.crashguard.config.ConfigHandler;
import com.crashguard.config.EnchantmentMaterialConfig;
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

    private int scrollRow = 0;
    private ItemStack lastTool = ItemStack.EMPTY;
    private boolean isUpdating = false;
    private boolean isRefreshingBooks = false;
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

        try {
            if (pendingClear) {
                pendingClear = false;
                inventory.set(SLOT_TOOL, ItemStack.EMPTY);
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
        } catch (Exception e) {
            System.err.println("[CrashGuard] update error: " + e.getMessage());
        }
    }

    private void refreshBooksFromTool(ItemStack tool) {
        if (isUpdating) return;
        isUpdating = true;
        isRefreshingBooks = true;
        try {
            for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
                inventory.set(i, ItemStack.EMPTY);
            }

            if (tool.isEmpty()) return;

            NBTTagCompound rootTag = tool.getTagCompound();
            if (rootTag == null) return;

            NBTBase enchBase = rootTag.getTag("ench");
            NBTTagList enchList = new NBTTagList();

            if (enchBase instanceof NBTTagList) {
                enchList = (NBTTagList) enchBase;
            } else if (enchBase instanceof NBTTagCompound) {
                enchList.appendTag((NBTTagCompound) enchBase);
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
        } catch (Exception e) {
            System.err.println("[CrashGuard] refreshBooksFromTool error: " + e.getMessage());
        } finally {
            isRefreshingBooks = false;
            isUpdating = false;
        }
        updateToolEnchantments();
    }

    private void updateOutput() {
        try {
            ItemStack tool = getStackInSlot(SLOT_TOOL);
            if (tool.isEmpty()) {
                inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
                return;
            }

            ItemStack preview = tool.copy();
            ItemStack top = getStackInSlot(SLOT_MATERIAL_TOP);
            ItemStack bottom = getStackInSlot(SLOT_MATERIAL_BOTTOM);

            if (!top.isEmpty() && !bottom.isEmpty() &&
                    top.getItem().getRegistryName().toString().equals(bottom.getItem().getRegistryName().toString())) {
                ItemStack merged = top.copy();
                merged.setCount(top.getCount() + bottom.getCount());
                applySpecificMaterialEnchantmentsPreview(preview, merged);
            } else {
                if (!top.isEmpty()) applySpecificMaterialEnchantmentsPreview(preview, top);
                if (!bottom.isEmpty()) applySpecificMaterialEnchantmentsPreview(preview, bottom);
            }

            if (!top.isEmpty()) applyGlobalMaterialEnchantmentsPreview(preview, top);
            if (!bottom.isEmpty()) applyGlobalMaterialEnchantmentsPreview(preview, bottom);
            applyEnchantBoostsPreview(preview, top, bottom);

            inventory.set(SLOT_OUTPUT, preview);
        } catch (Exception e) {
            System.err.println("[CrashGuard] updateOutput error: " + e.getMessage());
            inventory.set(SLOT_OUTPUT, ItemStack.EMPTY);
        }
    }

    private void applySpecificMaterialEnchantmentsPreview(ItemStack tool, ItemStack materialStack) {
        if (tool.isEmpty() || materialStack.isEmpty()) return;

        String itemId = materialStack.getItem().getRegistryName().toString();
        Map<String, EnchantmentMaterialConfig.MaterialEntry> enchantmentsForItem =
                EnchantmentMaterialConfig.getEnchantmentsForItem(itemId);

        if (enchantmentsForItem.isEmpty()) return;

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
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

        for (Map.Entry<String, EnchantmentMaterialConfig.MaterialEntry> entry : enchantmentsForItem.entrySet()) {
            String enchId = entry.getKey();
            EnchantmentMaterialConfig.MaterialEntry materialConfig = entry.getValue();

            Enchantment ench = Enchantment.getEnchantmentByLocation(enchId);
            if (ench == null) continue;

            int requiredCount = materialConfig.count;
            int maxLevel = materialConfig.max_level;
            int stackCount = materialStack.getCount();
            int timesCanDo = stackCount / requiredCount;

            int currentLevel = enchantmentMap.getOrDefault(ench, 0);
            if (currentLevel >= maxLevel) continue;

            int newLevel = Math.min(currentLevel + timesCanDo, maxLevel);
            if (newLevel > currentLevel) {
                enchantmentMap.put(ench, newLevel);
            }
        }

        NBTTagList newEnchList = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> e : enchantmentMap.entrySet()) {
            if (e.getValue() <= 0) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) Enchantment.getEnchantmentID(e.getKey()));
            tag.setShort("lvl", e.getValue().shortValue());
            newEnchList.appendTag(tag);
        }
        rootTag.setTag("ench", newEnchList);
    }

    private void applyGlobalMaterialEnchantmentsPreview(ItemStack tool, ItemStack materialStack) {
        if (tool.isEmpty() || materialStack.isEmpty()) return;

        String itemId = materialStack.getItem().getRegistryName().toString();
        EnchantmentMaterialConfig.GlobalMaterialEntry globalEntry = EnchantmentMaterialConfig.getGlobalMaterialEntry(itemId);
        if (globalEntry == null) return;

        int requiredCount = globalEntry.count;
        int maxLevel = globalEntry.max_level;
        int perLevel = globalEntry.per_level;
        if (perLevel <= 0) perLevel = 1;

        int stackCount = materialStack.getCount();
        int timesCanDo = stackCount / requiredCount;
        if (timesCanDo <= 0) return;

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
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

        for (Enchantment ench : Enchantment.REGISTRY) {
            if (ench == null) continue;
            int currentLevel = enchantmentMap.getOrDefault(ench, 0);
            if (currentLevel >= maxLevel) continue;
            int newLevel = Math.min(currentLevel + (perLevel * timesCanDo), maxLevel);
            if (newLevel > currentLevel) {
                enchantmentMap.put(ench, newLevel);
            }
        }

        NBTTagList newEnchList = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
            if (entry.getValue() <= 0) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            tag.setShort("lvl", entry.getValue().shortValue());
            newEnchList.appendTag(tag);
        }
        rootTag.setTag("ench", newEnchList);
    }

    private void applyEnchantBoostsPreview(ItemStack tool, ItemStack top, ItemStack bottom) {
        List<EnchantBoost> boosts = new ArrayList<>();
        int finalMaxLevel = 0;

        if (!top.isEmpty()) {
            String itemId = top.getItem().getRegistryName().toString();
            ConfigHandler.EnchantBoostInfo info = ConfigHandler.enchantBoostItems.get(itemId);
            if (info != null) {
                int count = top.getCount();
                for (int i = 0; i < count; i++) {
                    boosts.add(new EnchantBoost(info.boost, info.maxLevel));
                }
                finalMaxLevel = Math.max(finalMaxLevel, info.maxLevel);
            }
        }
        if (!bottom.isEmpty()) {
            String itemId = bottom.getItem().getRegistryName().toString();
            ConfigHandler.EnchantBoostInfo info = ConfigHandler.enchantBoostItems.get(itemId);
            if (info != null) {
                int count = bottom.getCount();
                for (int i = 0; i < count; i++) {
                    boosts.add(new EnchantBoost(info.boost, info.maxLevel));
                }
                finalMaxLevel = Math.max(finalMaxLevel, info.maxLevel);
            }
        }

        if (boosts.isEmpty()) return;

        boosts.sort(Comparator.comparingInt(b -> b.maxLevel));

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
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

        for (EnchantBoost boost : boosts) {
            for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                int currentLevel = entry.getValue();
                if (currentLevel >= boost.maxLevel) continue;
                int newLevel = Math.min(currentLevel + boost.boost, boost.maxLevel);
                if (newLevel > currentLevel) {
                    enchantmentMap.put(entry.getKey(), newLevel);
                }
            }
        }

        if (finalMaxLevel > 0) {
            for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                int currentLevel = entry.getValue();
                if (currentLevel >= finalMaxLevel) continue;
                int newLevel = Math.min(currentLevel, finalMaxLevel);
                if (newLevel < currentLevel) {
                    enchantmentMap.put(entry.getKey(), newLevel);
                }
            }
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
            int globalMax = ConfigHandler.getEnchantmentMaxLevel(entry.getKey().getRegistryName().toString());
            int currentLevel = entry.getValue();
            if (currentLevel > globalMax) {
                enchantmentMap.put(entry.getKey(), globalMax);
            }
        }

        NBTTagList newEnchList = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            tag.setShort("lvl", entry.getValue().shortValue());
            newEnchList.appendTag(tag);
        }
        rootTag.setTag("ench", newEnchList);
    }

    private void applySpecificMaterialEnchantmentsConsume(ItemStack tool, ItemStack materialStack) {
        if (tool.isEmpty() || materialStack.isEmpty()) return;

        String itemId = materialStack.getItem().getRegistryName().toString();
        Map<String, EnchantmentMaterialConfig.MaterialEntry> enchantmentsForItem =
                EnchantmentMaterialConfig.getEnchantmentsForItem(itemId);
        if (enchantmentsForItem.isEmpty()) return;

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
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

        boolean changed = false;
        int totalConsume = 0;

        for (Map.Entry<String, EnchantmentMaterialConfig.MaterialEntry> entry : enchantmentsForItem.entrySet()) {
            String enchId = entry.getKey();
            EnchantmentMaterialConfig.MaterialEntry config = entry.getValue();

            Enchantment ench = Enchantment.getEnchantmentByLocation(enchId);
            if (ench == null) continue;

            int requiredCount = config.count;
            int maxLevel = config.max_level;
            int stackCount = materialStack.getCount();
            int timesCanDo = stackCount / requiredCount;

            int currentLevel = enchantmentMap.getOrDefault(ench, 0);
            if (currentLevel >= maxLevel) continue;

            int levelNeeded = maxLevel - currentLevel;
            int actualTimes = Math.min(levelNeeded, timesCanDo);

            if (actualTimes > 0) {
                int newLevel = currentLevel + actualTimes;
                enchantmentMap.put(ench, newLevel);
                changed = true;
                totalConsume = requiredCount * actualTimes;
            }
        }

        if (changed && totalConsume > 0) {
            materialStack.shrink(totalConsume);

            NBTTagList newEnchList = new NBTTagList();
            for (Map.Entry<Enchantment, Integer> e : enchantmentMap.entrySet()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setShort("id", (short) Enchantment.getEnchantmentID(e.getKey()));
                tag.setShort("lvl", e.getValue().shortValue());
                newEnchList.appendTag(tag);
            }
            rootTag.setTag("ench", newEnchList);
        }
    }

    private void applyGlobalMaterialEnchantmentsConsume(ItemStack tool, ItemStack materialStack) {
        if (tool.isEmpty() || materialStack.isEmpty()) return;

        String itemId = materialStack.getItem().getRegistryName().toString();
        EnchantmentMaterialConfig.GlobalMaterialEntry globalEntry = EnchantmentMaterialConfig.getGlobalMaterialEntry(itemId);
        if (globalEntry == null) return;

        int requiredCount = globalEntry.count;
        int maxLevel = globalEntry.max_level;
        int perLevel = globalEntry.per_level;
        if (perLevel <= 0) perLevel = 1;

        int stackCount = materialStack.getCount();
        int timesCanDo = stackCount / requiredCount;
        if (timesCanDo <= 0) return;

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
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

        boolean changed = false;
        int actualTimes = 1;

        for (Enchantment ench : Enchantment.REGISTRY) {
            if (ench == null) continue;
            int currentLevel = enchantmentMap.getOrDefault(ench, 0);
            if (currentLevel >= maxLevel) continue;
            int newLevel = Math.min(currentLevel + (perLevel * actualTimes), maxLevel);
            if (newLevel > currentLevel) {
                enchantmentMap.put(ench, newLevel);
                changed = true;
            }
        }

        if (changed) {
            int totalConsume = requiredCount * actualTimes;
            if (materialStack.getCount() >= totalConsume) {
                materialStack.shrink(totalConsume);
            }

            NBTTagList newEnchList = new NBTTagList();
            for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                if (entry.getValue() <= 0) continue;
                NBTTagCompound tag = new NBTTagCompound();
                tag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
                tag.setShort("lvl", entry.getValue().shortValue());
                newEnchList.appendTag(tag);
            }
            rootTag.setTag("ench", newEnchList);
        }
    }

    private void applyEnchantBoostsConsume(ItemStack tool, ItemStack materialStack) {
        if (tool.isEmpty() || materialStack.isEmpty()) return;

        String itemId = materialStack.getItem().getRegistryName().toString();
        ConfigHandler.EnchantBoostInfo info = ConfigHandler.enchantBoostItems.get(itemId);
        if (info == null) return;

        int boost = info.boost;
        int maxLevel = info.maxLevel;

        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
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

        boolean changed = false;
        int availableCount = materialStack.getCount();
        int usedCount = 0;

        for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
            int currentLevel = entry.getValue();
            if (currentLevel >= maxLevel) continue;
            int needed = maxLevel - currentLevel;
            int timesNeeded = (int) Math.ceil((double) needed / boost);
            if (timesNeeded > usedCount) {
                usedCount = timesNeeded;
            }
        }

        usedCount = Math.min(usedCount, availableCount);

        if (usedCount > 0) {
            for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
                int currentLevel = entry.getValue();
                if (currentLevel >= maxLevel) continue;
                int newLevel = Math.min(currentLevel + (boost * usedCount), maxLevel);
                if (newLevel > currentLevel) {
                    enchantmentMap.put(entry.getKey(), newLevel);
                    changed = true;
                }
            }

            if (changed) {
                materialStack.shrink(usedCount);

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
    }

    private int getEnchantmentLevel(ItemStack tool, Enchantment ench) {
        if (tool.isEmpty() || ench == null) return 0;
        NBTTagCompound tag = tool.getTagCompound();
        if (tag == null) return 0;
        NBTTagList enchList = tag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
        int enchId = Enchantment.getEnchantmentID(ench);
        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound entry = enchList.getCompoundTagAt(i);
            if (entry.getShort("id") == enchId) {
                return entry.getShort("lvl");
            }
        }
        return 0;
    }

    private void setEnchantmentLevel(ItemStack tool, Enchantment ench, int level) {
        if (tool.isEmpty() || ench == null) return;
        NBTTagCompound rootTag = tool.getTagCompound();
        if (rootTag == null) {
            rootTag = new NBTTagCompound();
            tool.setTagCompound(rootTag);
        }

        NBTTagList enchList = rootTag.getTagList("ench", Constants.NBT.TAG_COMPOUND);
        Map<Enchantment, Integer> map = new HashMap<>();
        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = enchList.getCompoundTagAt(i);
            int id = tag.getShort("id");
            int lvl = tag.getShort("lvl");
            Enchantment e = Enchantment.getEnchantmentByID(id);
            if (e != null) {
                map.put(e, lvl);
            }
        }
        map.put(ench, level);

        NBTTagList newList = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : map.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            tag.setShort("lvl", entry.getValue().shortValue());
            newList.appendTag(tag);
        }
        rootTag.setTag("ench", newList);
    }

    private void performActualUpgrade() {
        if (world.isRemote) return;

        try {
            ItemStack tool = getStackInSlot(SLOT_TOOL);
            if (tool.isEmpty()) return;

            ItemStack top = getStackInSlot(SLOT_MATERIAL_TOP);
            ItemStack bottom = getStackInSlot(SLOT_MATERIAL_BOTTOM);

            if (top.isEmpty() && bottom.isEmpty()) return;

            ItemStack result = tool.copy();
            if (tool.getTagCompound() != null) {
                result.setTagCompound(tool.getTagCompound().copy());
            }

            // 1. 先处理附魔加成
            if (!top.isEmpty()) applyEnchantBoostsConsume(result, top);
            if (!bottom.isEmpty()) applyEnchantBoostsConsume(result, bottom);

            // 2. 再处理特定材料
            if (!top.isEmpty() || !bottom.isEmpty()) {
                ItemStack merged = ItemStack.EMPTY;
                if (!top.isEmpty() && !bottom.isEmpty()) {
                    String topId = top.getItem().getRegistryName().toString();
                    String bottomId = bottom.getItem().getRegistryName().toString();
                    if (topId.equals(bottomId)) {
                        merged = top.copy();
                        merged.setCount(top.getCount() + bottom.getCount());
                    }
                }

                if (!merged.isEmpty()) {
                    int beforeCount = merged.getCount();
                    applySpecificMaterialEnchantmentsConsume(result, merged);
                    int consumed = beforeCount - merged.getCount();

                    if (consumed > 0) {
                        int remaining = consumed;
                        if (!top.isEmpty()) {
                            int take = Math.min(top.getCount(), remaining);
                            top.shrink(take);
                            remaining -= take;
                        }
                        if (remaining > 0 && !bottom.isEmpty()) {
                            bottom.shrink(remaining);
                        }
                    }
                } else {
                    if (!top.isEmpty()) applySpecificMaterialEnchantmentsConsume(result, top);
                    if (!bottom.isEmpty()) applySpecificMaterialEnchantmentsConsume(result, bottom);
                }
            }

            // 3. 最后处理全局材料
            if (!top.isEmpty()) applyGlobalMaterialEnchantmentsConsume(result, top);
            if (!bottom.isEmpty()) applyGlobalMaterialEnchantmentsConsume(result, bottom);

            // 更新工具槽
            inventory.set(SLOT_TOOL, result);
            lastTool = result.copy();

            // 清空附魔书槽
            for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
                inventory.set(i, ItemStack.EMPTY);
            }

            if (top.getCount() <= 0) inventory.set(SLOT_MATERIAL_TOP, ItemStack.EMPTY);
            if (bottom.getCount() <= 0) inventory.set(SLOT_MATERIAL_BOTTOM, ItemStack.EMPTY);

            markDirty();
            syncToClient();
        } catch (Exception e) {
            System.err.println("[CrashGuard] performActualUpgrade error: " + e.getMessage());
        }
    }

    public void onBookInventoryChanged() {
        if (isRefreshingBooks || isUpdating) return;

        isUpdating = true;
        try {
            updateToolEnchantments();
        } finally {
            isUpdating = false;
        }
    }

    private void updateToolEnchantments() {
        if (world.isRemote) return;

        try {
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
        } catch (Exception e) {
            System.err.println("[CrashGuard] updateToolEnchantments error: " + e.getMessage());
        }
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
            int finalLevel = Math.min(entry.getValue(), 32767);
            NBTTagCompound enchTag = new NBTTagCompound();
            enchTag.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            enchTag.setShort("lvl", (short) finalLevel);
            newEnchList.appendTag(enchTag);
        }
        return newEnchList;
    }

    private void clearAllBooks() {
        for (int i = BOOK_START; i < BOOK_START + BOOK_SLOT_COUNT; i++) {
            inventory.set(i, ItemStack.EMPTY);
        }
        scrollRow = 0;
    }

    public void scrollUp() {
        if (scrollRow - 3 >= 0) {
            scrollRow -= 3;
            markDirty();
            syncToClient();
        }
    }

    public void scrollDown() {
        if (scrollRow + 3 <= TOTAL_ROWS) {
            scrollRow += 3;
            markDirty();
            syncToClient();
        }
    }

    public int getScrollRow() { return scrollRow; }
    public int getMaxScrollRow() { return TOTAL_ROWS - 3; }

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

                performActualUpgrade();

                markDirty();
                syncToClient();
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

                performActualUpgrade();

                markDirty();
                syncToClient();
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
        if (index == SLOT_MATERIAL_TOP || index == SLOT_MATERIAL_BOTTOM || index == SLOT_TOOL) {
            updateOutput();
            markDirty();
            syncToClient();
        }
        if (index == SLOT_TOOL) {
            refreshBooksFromTool(stack);
            lastTool = stack.copy();
        }
        if (index >= BOOK_START && !isRefreshingBooks) {
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