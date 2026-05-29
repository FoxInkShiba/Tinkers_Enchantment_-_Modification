package com.crashguard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class EnchantmentMaterialConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = Loader.instance().getConfigDir() + "/crashguard/";
    private static final String FILE_NAME = "enchantment_materials.json";

    private static EnchantmentMaterialConfig INSTANCE;

    public List<EnchantmentEntry> enchantments = new ArrayList<>();
    public List<GlobalMaterialEntry> global_materials = new ArrayList<>();

    public static class EnchantmentEntry {
        public String id;
        public List<MaterialEntry> materials = new ArrayList<>();
    }

    public static class GlobalMaterialEntry {
        public String item;
        public int count = 1;
        public int max_level = 32767;
        public int per_level = 1;
    }

    public static class MaterialEntry {
        public String item;
        public int count = 1;
        public int max_level = 32767;
    }

    public static void load() {
        File dir = new File(CONFIG_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, FILE_NAME);

        if (!file.exists()) {
            createDefaultConfig(file);
        }

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<EnchantmentMaterialConfig>(){}.getType();
            INSTANCE = GSON.fromJson(reader, type);
            System.out.println("[CrashGuard] 加载附魔材料配置成功");
            System.out.println("[CrashGuard]   - 特定附魔材料: " + INSTANCE.enchantments.size() + " 种");
            System.out.println("[CrashGuard]   - 全局材料: " + INSTANCE.global_materials.size() + " 种");
        } catch (IOException e) {
            System.err.println("[CrashGuard] 加载附魔材料配置失败: " + e.getMessage());
            INSTANCE = new EnchantmentMaterialConfig();
        }
    }

    private static void createDefaultConfig(File file) {
        EnchantmentMaterialConfig defaultConfig = new EnchantmentMaterialConfig();

        // 锋利
        EnchantmentEntry sharpness = new EnchantmentEntry();
        sharpness.id = "minecraft:sharpness";
        MaterialEntry quartz = new MaterialEntry();
        quartz.item = "minecraft:quartz";
        quartz.count = 40;
        quartz.max_level = 5;
        sharpness.materials.add(quartz);
        defaultConfig.enchantments.add(sharpness);

        // 效率
        EnchantmentEntry efficiency = new EnchantmentEntry();
        efficiency.id = "minecraft:efficiency";
        MaterialEntry redstone = new MaterialEntry();
        redstone.item = "minecraft:redstone";
        redstone.count = 40;
        redstone.max_level = 5;
        efficiency.materials.add(redstone);
        defaultConfig.enchantments.add(efficiency);

        // 时运
        EnchantmentEntry fortune = new EnchantmentEntry();
        fortune.id = "minecraft:fortune";
        MaterialEntry lapis = new MaterialEntry();
        lapis.item = "minecraft:lapis_lazuli";
        lapis.count = 40;
        lapis.max_level = 5;
        fortune.materials.add(lapis);
        defaultConfig.enchantments.add(fortune);

        // 抢夺
        EnchantmentEntry looting = new EnchantmentEntry();
        looting.id = "minecraft:looting";
        looting.materials.add(lapis);
        defaultConfig.enchantments.add(looting);

        // 耐久
        EnchantmentEntry unbreaking = new EnchantmentEntry();
        unbreaking.id = "minecraft:unbreaking";
        MaterialEntry obsidian = new MaterialEntry();
        obsidian.item = "minecraft:obsidian";
        obsidian.count = 10;
        obsidian.max_level = 10;
        unbreaking.materials.add(obsidian);
        defaultConfig.enchantments.add(unbreaking);

        // 保护
        EnchantmentEntry protection = new EnchantmentEntry();
        protection.id = "minecraft:protection";
        MaterialEntry ironBlock = new MaterialEntry();
        ironBlock.item = "minecraft:iron_block";
        ironBlock.count = 5;
        ironBlock.max_level = 5;
        protection.materials.add(ironBlock);
        defaultConfig.enchantments.add(protection);

        // 力量
        EnchantmentEntry power = new EnchantmentEntry();
        power.id = "minecraft:power";
        MaterialEntry string = new MaterialEntry();
        string.item = "minecraft:string";
        string.count = 40;
        string.max_level = 5;
        power.materials.add(string);
        defaultConfig.enchantments.add(power);

        // 经验修补
        EnchantmentEntry mending = new EnchantmentEntry();
        mending.id = "minecraft:mending";
        MaterialEntry expBottle = new MaterialEntry();
        expBottle.item = "minecraft:experience_bottle";
        expBottle.count = 1;
        expBottle.max_level = 3;
        mending.materials.add(expBottle);
        defaultConfig.enchantments.add(mending);

        // 龙蛋
        GlobalMaterialEntry dragonEgg = new GlobalMaterialEntry();
        dragonEgg.item = "minecraft:dragon_egg";
        dragonEgg.count = 1;
        dragonEgg.max_level = 32767;
        dragonEgg.per_level = 1;
        defaultConfig.global_materials.add(dragonEgg);

        try (Writer writer = new FileWriter(file)) {
            writer.write(GSON.toJson(defaultConfig));
            System.out.println("[CrashGuard] 创建默认附魔材料配置文件: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[CrashGuard] 创建默认配置文件失败: " + e.getMessage());
        }
    }

    public static EnchantmentMaterialConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static MaterialEntry getMaterialEntry(String enchantmentId, String itemId) {
        for (EnchantmentEntry entry : get().enchantments) {
            if (entry.id.equals(enchantmentId)) {
                for (MaterialEntry material : entry.materials) {
                    if (material.item.equals(itemId)) {
                        return material;
                    }
                }
            }
        }
        return null;
    }

    public static Map<String, MaterialEntry> getEnchantmentsForItem(String itemId) {
        Map<String, MaterialEntry> result = new HashMap<>();
        for (EnchantmentEntry entry : get().enchantments) {
            for (MaterialEntry material : entry.materials) {
                if (material.item.equals(itemId)) {
                    result.put(entry.id, material);
                }
            }
        }
        return result;
    }

    public static GlobalMaterialEntry getGlobalMaterialEntry(String itemId) {
        for (GlobalMaterialEntry entry : get().global_materials) {
            if (entry.item.equals(itemId)) {
                return entry;
            }
        }
        return null;
    }

    public static void reload() {
        INSTANCE = null;
        load();
    }
}