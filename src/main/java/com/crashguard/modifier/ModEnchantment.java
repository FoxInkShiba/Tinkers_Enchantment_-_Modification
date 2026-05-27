package com.crashguard.modifier;

import com.crashguard.config.ConfigHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import slimeknights.tconstruct.library.tools.ToolCore;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModEnchantment {

    private static final String KEY_ENCHANTS = "ench";
    private static final String KEY_ENCH_ID = "id";
    private static final String KEY_ENCH_LVL = "lvl";

    private static NBTTagCompound getRootTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    // ========== 外部调用接口 ==========
    public static boolean addEnchantment(ItemStack stack, Enchantment ench, int level) {
        if (!(stack.getItem() instanceof ToolCore)) return false;
        if (ench == null) return false;

        if (ConfigHandler.storeEnchantmentInRootNBT) {
            return addEnchantmentToRootNBT(stack, ench, level);
        } else {
            stack.addEnchantment(ench, level);
            return true;
        }
    }

    private static boolean addEnchantmentToRootNBT(ItemStack stack, Enchantment ench, int level) {
        NBTTagCompound rootTag = getRootTag(stack);
        NBTTagList enchList;

        if (rootTag.hasKey(KEY_ENCHANTS, Constants.NBT.TAG_LIST)) {
            enchList = rootTag.getTagList(KEY_ENCHANTS, Constants.NBT.TAG_COMPOUND);
        } else {
            enchList = new NBTTagList();
        }

        // 统一使用字符串 ID
        String enchId = ench.getRegistryName().toString();
        boolean found = false;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound entry = enchList.getCompoundTagAt(i);
            String existingId = null;

            // 读取现有附魔 ID（兼容新旧格式）
            if (entry.hasKey(KEY_ENCH_ID, Constants.NBT.TAG_STRING)) {
                existingId = entry.getString(KEY_ENCH_ID);
            } else if (entry.hasKey("id", Constants.NBT.TAG_SHORT)) {
                int oldId = entry.getShort("id");
                Enchantment oldEnch = Enchantment.getEnchantmentByID(oldId);
                if (oldEnch != null) {
                    existingId = oldEnch.getRegistryName().toString();
                }
            }

            if (existingId != null && existingId.equals(enchId)) {
                entry.setString(KEY_ENCH_ID, enchId);
                entry.setInteger(KEY_ENCH_LVL, level);
                found = true;
                break;
            }
        }

        if (!found) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString(KEY_ENCH_ID, enchId);
            entry.setInteger(KEY_ENCH_LVL, level);
            enchList.appendTag(entry);
        }

        rootTag.setTag(KEY_ENCHANTS, enchList);
        System.out.println("[CrashGuard] Added enchantment: " + enchId + " level " + level);
        return true;
    }

    public static int getLevel(ItemStack stack, Enchantment ench) {
        if (!(stack.getItem() instanceof ToolCore)) return 0;
        return EnchantmentHelper.getEnchantmentLevel(ench, stack);
    }

    public static Map<Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolCore)) return new LinkedHashMap<>();
        return EnchantmentHelper.getEnchantments(stack);
    }

    // ========== 直接读取 NBT（供 Mixin 和高级附魔台使用）==========
    public static int getLevelDirect(ItemStack stack, Enchantment ench) {
        if (!(stack.getItem() instanceof ToolCore)) return 0;
        if (ench == null) return 0;

        NBTTagCompound rootTag = stack.getTagCompound();
        if (rootTag == null) return 0;

        NBTTagList enchList = rootTag.getTagList(KEY_ENCHANTS, Constants.NBT.TAG_LIST);
        String target = ench.getRegistryName().toString();

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound entry = enchList.getCompoundTagAt(i);

            // 兼容字符串 ID
            if (entry.hasKey(KEY_ENCH_ID, Constants.NBT.TAG_STRING)) {
                if (entry.getString(KEY_ENCH_ID).equals(target)) {
                    return entry.getInteger(KEY_ENCH_LVL);
                }
            }
            // 兼容数字 ID
            else if (entry.hasKey("id", Constants.NBT.TAG_SHORT)) {
                int id = entry.getShort("id");
                Enchantment e = Enchantment.getEnchantmentByID(id);
                if (e != null && e.getRegistryName().toString().equals(target)) {
                    return entry.getShort("lvl");
                }
            }
        }
        return 0;
    }

    public static Map<Enchantment, Integer> getAllEnchantmentsDirect(ItemStack stack) {
        Map<Enchantment, Integer> map = new LinkedHashMap<>();
        if (!(stack.getItem() instanceof ToolCore)) return map;

        NBTTagCompound rootTag = stack.getTagCompound();
        if (rootTag == null) return map;

        NBTTagList enchList = rootTag.getTagList(KEY_ENCHANTS, Constants.NBT.TAG_LIST);

        System.out.println("[CrashGuard] getAllEnchantmentsDirect: found " + enchList.tagCount() + " enchantments in NBT");

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound entry = enchList.getCompoundTagAt(i);
            Enchantment ench = null;
            int lvl = 0;

            // 字符串 ID 格式
            if (entry.hasKey(KEY_ENCH_ID, Constants.NBT.TAG_STRING)) {
                String id = entry.getString(KEY_ENCH_ID);
                lvl = entry.getInteger(KEY_ENCH_LVL);
                ench = Enchantment.REGISTRY.getObject(new ResourceLocation(id));
                if (ench != null) {
                    map.put(ench, lvl);
                    System.out.println("[CrashGuard]   - " + id + " level " + lvl + " (string)");
                }
            }
            // 数字 ID 格式
            else if (entry.hasKey("id", Constants.NBT.TAG_SHORT)) {
                int id = entry.getShort("id");
                lvl = entry.getShort("lvl");
                ench = Enchantment.getEnchantmentByID(id);
                if (ench != null) {
                    map.put(ench, lvl);
                    System.out.println("[CrashGuard]   - " + ench.getRegistryName() + " level " + lvl + " (numeric)");
                }
            }
        }

        return map;
    }

    public static NBTTagList getAllEnchantmentsAsNBT(ItemStack stack) {
        NBTTagList result = new NBTTagList();

        if (!(stack.getItem() instanceof ToolCore)) return result;

        if (ConfigHandler.storeEnchantmentInRootNBT) {
            NBTTagCompound rootTag = stack.getTagCompound();
            if (rootTag == null) return result;
            NBTTagList enchList = rootTag.getTagList(KEY_ENCHANTS, Constants.NBT.TAG_LIST);
            for (int i = 0; i < enchList.tagCount(); i++) {
                result.appendTag(enchList.getCompoundTagAt(i).copy());
            }
        } else {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString(KEY_ENCH_ID, entry.getKey().getRegistryName().toString());
                tag.setInteger(KEY_ENCH_LVL, entry.getValue());
                result.appendTag(tag);
            }
        }
        return result;
    }

    public static boolean removeEnchantment(ItemStack stack, Enchantment ench) {
        if (!(stack.getItem() instanceof ToolCore)) return false;

        if (ConfigHandler.storeEnchantmentInRootNBT) {
            return removeFromRootNBT(stack, ench);
        } else {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            if (enchants.remove(ench) != null) {
                EnchantmentHelper.setEnchantments(enchants, stack);
                return true;
            }
            return false;
        }
    }

    private static boolean removeFromRootNBT(ItemStack stack, Enchantment ench) {
        NBTTagCompound rootTag = stack.getTagCompound();
        if (rootTag == null) return false;

        NBTTagList enchList = rootTag.getTagList(KEY_ENCHANTS, Constants.NBT.TAG_LIST);
        String target = ench.getRegistryName().toString();

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound entry = enchList.getCompoundTagAt(i);
            String existingId = null;

            if (entry.hasKey(KEY_ENCH_ID, Constants.NBT.TAG_STRING)) {
                existingId = entry.getString(KEY_ENCH_ID);
            } else if (entry.hasKey("id", Constants.NBT.TAG_SHORT)) {
                int oldId = entry.getShort("id");
                Enchantment oldEnch = Enchantment.getEnchantmentByID(oldId);
                if (oldEnch != null) {
                    existingId = oldEnch.getRegistryName().toString();
                }
            }

            if (existingId != null && existingId.equals(target)) {
                enchList.removeTag(i);
                if (enchList.tagCount() == 0) {
                    rootTag.removeTag(KEY_ENCHANTS);
                } else {
                    rootTag.setTag(KEY_ENCHANTS, enchList);
                }
                System.out.println("[CrashGuard] Removed enchantment: " + target);
                return true;
            }
        }
        return false;
    }

    public static void clearEnchantments(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolCore)) return;

        if (ConfigHandler.storeEnchantmentInRootNBT) {
            NBTTagCompound rootTag = stack.getTagCompound();
            if (rootTag != null) {
                rootTag.removeTag(KEY_ENCHANTS);
            }
        } else {
            EnchantmentHelper.setEnchantments(new LinkedHashMap<>(), stack);
        }
    }

    public static boolean hasEnchantments(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolCore)) return false;

        if (ConfigHandler.storeEnchantmentInRootNBT) {
            NBTTagCompound rootTag = stack.getTagCompound();
            if (rootTag == null) return false;
            return rootTag.hasKey(KEY_ENCHANTS, Constants.NBT.TAG_LIST) &&
                    rootTag.getTagList(KEY_ENCHANTS, Constants.NBT.TAG_LIST).tagCount() > 0;
        } else {
            return EnchantmentHelper.getEnchantments(stack).size() > 0;
        }
    }
}