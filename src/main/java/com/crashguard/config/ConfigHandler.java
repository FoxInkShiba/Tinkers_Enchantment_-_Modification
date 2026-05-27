package com.crashguard.config;

import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigHandler {
    public static Configuration config;

    // ========== 存储方式配置 / Storage Settings ==========
    public static boolean storeEnchantmentInRootNBT = true;

    // ========== 附魔保护配置 / Enchantment Protection ==========
    public static boolean preventEnchantmentDeletion = true;

    // ========== 远程附魔叠加方式 / Ranged Enchantment Stacking ==========
    public static boolean enchantmentStackAdditive = true;

    // ========== 附魔保护升级物品 / Upgrade Item ==========
    public static String preserveUpgradeItem = "minecraft:diamond";

    // ========== 高级附魔台配置 / Advanced Enchanting Table Settings ==========
    public static boolean advancedEnchantingTableOnlyTinker = true;
    public static boolean protectExistingEnchantment = true;
    public static boolean enchantmentAllowAdditive = true;
    public static String enchantmentMode = "merge";

    // ========== 附魔等级上限 / Enchantment Level Limits ==========
    public static int globalEnchantmentMaxLevel = 32767;
    public static Map<String, Integer> enchantmentMaxLevels = new HashMap<>();

    // ========== 附魔加成物品 / Enchant Boost Items ==========
    public static Map<String, EnchantBoostInfo> enchantBoostItems = new HashMap<>();

    public static class EnchantBoostInfo {
        public int boost;
        public int maxLevel;

        public EnchantBoostInfo(int boost, int maxLevel) {
            this.boost = boost;
            this.maxLevel = maxLevel;
        }
    }

    // ========== 远程伤害公式配置 / Ranged Damage Formula ==========
    public static float powerMultiplier = 0.25f;
    public static float advPowerMultiplier = 0.375f;
    public static float advPowerBase = 0.625f;
    public static float advPowerCritChancePerLevel = 0.25f;
    public static float critDamageMultiplier = 1.5f;
    public static float speedDamageMultiplier = 0.1f;

    // ========== 功能开关 / Feature Toggles ==========
    public static boolean enableRangedEnchant = true;
    public static boolean enableSpeedBonus = true;
    public static boolean enableMeleeEnchant = true;

    // ========== 伤害衰减配置 / Damage Decay Settings ==========
    public static float damageDecayMultiplier = 1.1f;
    public static float damageDecayCap = 25.0f;

    public static void init(File file) {
        config = new Configuration(file);
        syncConfig();
        if (config.hasChanged()) config.save();
    }

    public static void syncConfig() {
        // ========== 存储方式 / Storage ==========
        storeEnchantmentInRootNBT = config.getBoolean("storeEnchantmentInRootNBT", "storage", true,
                "附魔存储方式: true=存到根NBT(替换部件/升级时保留附魔), false=存到匠魂原位置(无用选项，不必理会)\n" +
                        "Enchantment storage location: true=Root NBT (preserved when replacing parts), false=Vanilla location (Translate the useless options, no need to worry about them.)");

        // ========== 附魔保护 / Enchantment Protection ==========
        preventEnchantmentDeletion = config.getBoolean("preventEnchantmentDeletion", "storage", true,
                "防止匠魂在重建工具/盔甲时删除附魔: true=保留附魔, false=允许删除(附魔会丢失)\n" +
                        "Prevent enchantment deletion on tool/armor rebuild: true=Keep enchantments, false=Allow deletion");

        // ========== 远程附魔叠加 / Ranged Enchantment Stacking ==========
        enchantmentStackAdditive = config.getBoolean("enchantmentStackAdditive", "storage", true,
                "远程附魔叠加方式: true=弓和箭附魔等级加算(如力量3+力量1=力量4), false=取最大值(如力量3+力量1=力量3)\n" +
                        "Ranged enchantment stacking method: true=Additive (Power3+Power1=Power4), false=Max (Power3+Power1=Power3)");

        // ========== 附魔保护升级物品 / Upgrade Item ==========
        preserveUpgradeItem = config.getString("preserveUpgradeItem", "storage", "minecraft:diamond",
                "附魔保护升级物品（物品ID）。留空则禁用此功能。示例: minecraft:diamond, tconstruct:manyullyn_ingot\n" +
                        "Enchantment preserver upgrade item ID. Empty to disable. Examples: minecraft:diamond, tconstruct:manyullyn_ingot");

        // ========== 高级附魔台配置 / Advanced Enchanting Table ==========
        advancedEnchantingTableOnlyTinker = config.getBoolean("advancedEnchantingTableOnlyTinker", "storage", true,
                "高级附魔台是否只允许匠魂工具/盔甲: true=只允许匠魂, false=允许所有物品\n" +
                        "Advanced enchanting table item restriction: true=Only Tinker tools/armor, false=Any item");

        protectExistingEnchantment = config.getBoolean("protectExistingEnchantment", "storage", true,
                "保护已有附魔不被低等级附魔书覆盖: true=只有附魔书等级更高时才替换, false=直接覆盖\n" +
                        "Protect existing enchantments from lower level books: true=Only upgrade if book level is higher, false=Always overwrite");

        enchantmentAllowAdditive = config.getBoolean("enchantmentAllowAdditive", "storage", true,
                "是否允许附魔等级总体可以相加:\n" +
                        "  true = 允许相加（根据enchantmentMode决定separate或merge）\n" +
                        "  false = 禁止相加（相同附魔取最高）\n\n" +
                        "Allow enchantment level addition:\n" +
                        "  true = Allow addition (depends on enchantmentMode)\n" +
                        "  false = Disable addition (keep highest only)");

        enchantmentMode = config.getString("enchantmentMode", "storage", "merge",
                "附魔处理模式(仅enchantmentAllowAdditive=true时生效):\n" +
                        "  separate = 保留多个条目（工具上会显示多个相同附魔）\n" +
                        "  merge = 合并等级（锋利3+锋利2=锋利5）\n\n" +
                        "Enchantment mode (only when enchantmentAllowAdditive=true):\n" +
                        "  separate = Keep multiple entries\n" +
                        "  merge = Merge levels (Sharpness3+Sharpness2=Sharpness5)");

        // ========== 附魔等级上限 / Enchantment Level Limits ==========
        globalEnchantmentMaxLevel = config.getInt("globalEnchantmentMaxLevel", "storage", 32767, 1, 32767,
                "全局附魔等级上限。默认32767，超过此上限的附魔将被限制。\n" +
                        "Global enchantment max level. Default 32767. Enchantments above this limit will be capped.");

        String[] entries = config.getStringList("enchantmentMaxLevels", "storage", new String[0],
                "特定附魔等级上限，优先级高于全局上限。格式: 附魔ID=最大等级\n" +
                        "示例:\nminecraft:sharpness=10\nminecraft:protection=5\nsomanyenchantments:advancedpower=20\n\n" +
                        "Per-enchantment max levels, priority over global. Format: enchantment_id=max_level\n" +
                        "Examples:\nminecraft:sharpness=10\nminecraft:protection=5\nsomanyenchantments:advancedpower=20");

        enchantmentMaxLevels.clear();
        for (String entry : entries) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    int maxLevel = Integer.parseInt(parts[1].trim());
                    enchantmentMaxLevels.put(parts[0].trim(), maxLevel);
                    System.out.println("[CrashGuard] Loaded enchantment max level: " + parts[0] + " -> " + maxLevel);
                } catch (NumberFormatException e) {
                    System.out.println("[CrashGuard] Invalid enchantment max level format: " + entry);
                }
            }
        }

        // ========== 附魔加成物品 / Enchant Boost Items ==========
        String[] boostEntries = config.getStringList("enchantBoostItems", "storage", new String[0],
                "附魔加成物品配置。格式: 物品ID@加成值@最大等级\n" +
                        "放入材料槽后，会消耗该物品，对工具附魔进行加成（按maxLevel从小到大排序依次应用）\n" +
                        "示例:\nminecraft:diamond@5@10\ntconstruct:manyullyn_ingot@3@15\n" +
                        "minecraft:nether_star@1@32767\n\n" +
                        "Enchant boost items. Format: item_id@boost@max_level\n" +
                        "When placed in material slot, consumes the item and boosts tool enchantments.\n" +
                        "Examples:\nminecraft:diamond@5@10\ntconstruct:manyullyn_ingot@3@15\n" +
                        "minecraft:nether_star@1@32767");

        // 如果没有配置项，写入默认值
        if (boostEntries.length == 0) {
            config.getCategory("storage").get("enchantBoostItems").setValues(new String[]{
                    "minecraft:diamond@5@10",
                    "minecraft:nether_star@1@32767"
            });
            boostEntries = config.getStringList("enchantBoostItems", "storage", new String[0], "");
        }

        enchantBoostItems.clear();
        for (String entry : boostEntries) {
            String[] parts = entry.split("@");
            if (parts.length == 3) {
                String itemId = parts[0].trim();
                try {
                    int boost = Integer.parseInt(parts[1].trim());
                    int maxLevel = Integer.parseInt(parts[2].trim());
                    enchantBoostItems.put(itemId, new EnchantBoostInfo(boost, maxLevel));
                    System.out.println("[CrashGuard] Loaded enchant boost: " + itemId + " -> +" + boost + " (max " + maxLevel + ")");
                } catch (NumberFormatException e) {
                    System.out.println("[CrashGuard] Invalid enchant boost format: " + entry);
                }
            } else {
                System.out.println("[CrashGuard] Invalid enchant boost entry (expected 3 parts): " + entry);
            }
        }

        // ========== 通用设置 / General Settings ==========
        enableMeleeEnchant = config.getBoolean("enableMeleeEnchant", "general", true,
                "启用近战武器附魔效果\nEnable melee enchantment effects");

        enableRangedEnchant = config.getBoolean("enableRangedEnchant", "general", true,
                "启用远程武器附魔效果\nEnable ranged enchantment effects");

        enableSpeedBonus = config.getBoolean("enableSpeedBonus", "general", true,
                "启用速度对远程伤害的加成\nEnable speed bonus for ranged damage");

        // ========== 伤害公式配置 / Damage Formula ==========
        powerMultiplier = config.getFloat("powerMultiplier", "damage", 0.25f, 0f, 10f,
                "力量附魔增伤系数: 增伤 = 基础伤害 × 系数 × (等级+1)\n" +
                        "Power enchantment multiplier: Damage bonus = base damage × multiplier × (level+1)");

        advPowerMultiplier = config.getFloat("advPowerMultiplier", "damage", 0.375f, 0f, 10f,
                "高级力量附魔增伤系数: 增伤 = 基础伤害 × (系数 × 等级 + 基数)\n" +
                        "Advanced Power enchantment multiplier: Damage bonus = base damage × (multiplier × level + base)");

        advPowerBase = config.getFloat("advPowerBase", "damage", 0.625f, 0f, 10f,
                "高级力量附魔基数\nAdvanced Power enchantment base");

        advPowerCritChancePerLevel = config.getFloat("advPowerCritChancePerLevel", "damage", 0.25f, 0f, 1f,
                "高级力量附魔每级暴击概率\nAdvanced Power crit chance per level");

        critDamageMultiplier = config.getFloat("critDamageMultiplier", "damage", 1.5f, 1f, 10f,
                "暴击伤害倍率\nCritical damage multiplier");

        speedDamageMultiplier = config.getFloat("speedDamageMultiplier", "damage", 0.1f, 0f, 1f,
                "速度伤害加成系数: 最终伤害 = (基础伤害 + 附魔增伤) × (1 + 速度 × 系数)\n" +
                        "Speed damage multiplier: Final damage = (base + bonus) × (1 + speed × multiplier)");

        // ========== 伤害衰减配置 / Damage Decay ==========
        damageDecayMultiplier = config.getFloat("damageDecayMultiplier", "decay", 1.1f, 0.0f, 10.0f,
                "匠魂伤害衰减系数增量。值越大，伤害越高。原版为0.9 过高可能导致崩溃。\n" +
                        "Tinkers' damage decay multiplier increment. Higher value = more damage. Vanilla is 0.9. Too high may cause crashes.");

        damageDecayCap = config.getFloat("damageDecayCap", "decay", 25.0f, 0.0f, 100.0f,
                "匠魂伤害衰减上限，设置单次衰减最多增加原本几倍的伤害\n" +
                        "Tinkers' damage decay cap. Sets how many times the original damage can be increased per decay.");
    }

    public static int getEnchantmentMaxLevel(String enchantmentId) {
        if (enchantmentMaxLevels.containsKey(enchantmentId)) {
            return enchantmentMaxLevels.get(enchantmentId);
        }
        return globalEnchantmentMaxLevel;
    }

    public static float getDamageDecayMultiplier() { return damageDecayMultiplier; }
    public static float getDamageDecayCap() { return damageDecayCap; }
}