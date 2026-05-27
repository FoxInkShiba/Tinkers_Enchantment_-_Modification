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

    public List<MaterialEntry> materials = new ArrayList<>();
    public Map<String, Integer> enchantmentLimits = new HashMap<>();

    public static class MaterialEntry {
        public String item;
        public List<EnchantmentEntry> enchantments = new ArrayList<>();
    }

    public static class EnchantmentEntry {
        public String id;
        public int level = 1;
        public int require_modifier_slots = 0;
        public int cost_modifier_slots_per_level = 0;
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
        } catch (IOException e) {
            System.err.println("[CrashGuard] 加载附魔材料配置失败: " + e.getMessage());
            INSTANCE = new EnchantmentMaterialConfig();
        }
    }

    private static void createDefaultConfig(File file) {
        EnchantmentMaterialConfig defaultConfig = new EnchantmentMaterialConfig();

        // 默认材料：附魔书
        MaterialEntry bookEntry = new MaterialEntry();
        bookEntry.item = "minecraft:enchanted_book";

        EnchantmentEntry sharpness = new EnchantmentEntry();
        sharpness.id = "minecraft:sharpness";
        sharpness.level = 5;
        sharpness.require_modifier_slots = 0;
        sharpness.cost_modifier_slots_per_level = 1;
        bookEntry.enchantments.add(sharpness);

        EnchantmentEntry protection = new EnchantmentEntry();
        protection.id = "minecraft:protection";
        protection.level = 4;
        protection.require_modifier_slots = 0;
        protection.cost_modifier_slots_per_level = 1;
        bookEntry.enchantments.add(protection);

        defaultConfig.materials.add(bookEntry);

        // 默认等级上限
        defaultConfig.enchantmentLimits.put("minecraft:sharpness", 10);
        defaultConfig.enchantmentLimits.put("minecraft:protection", 8);

        try (Writer writer = new FileWriter(file)) {
            writer.write(GSON.toJson(defaultConfig));
            System.out.println("[CrashGuard] 创建默认附魔材料配置文件");
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

    public static MaterialEntry getMaterialEntry(String itemId) {
        for (MaterialEntry entry : get().materials) {
            if (entry.item.equals(itemId)) {
                return entry;
            }
        }
        return null;
    }

    public static EnchantmentEntry getEnchantmentEntry(String itemId, String enchantmentId) {
        MaterialEntry material = getMaterialEntry(itemId);
        if (material != null) {
            for (EnchantmentEntry entry : material.enchantments) {
                if (entry.id.equals(enchantmentId)) {
                    return entry;
                }
            }
        }
        return null;
    }

    public static int getEnchantmentLimit(String enchantmentId) {
        return get().enchantmentLimits.getOrDefault(enchantmentId, Integer.MAX_VALUE);
    }
}