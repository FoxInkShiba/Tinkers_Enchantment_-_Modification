package com.crashguard.config;

import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigHandler {
    public static Configuration config;

    // ========== 存储方式配置 / Storage Settings ==========
    public static boolean storeEnchantmentInRootNBT = true;
    public static boolean preventEnchantmentDeletion = true;
    public static boolean enchantmentStackAdditive = true;
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

    // ========== 盔甲保护配置（通用）/ Armor Protection Settings (General) ==========
    public static int resistantMaxLevel = 12;
    public static String damageReductionFormula = "";
    public static float damageReductionCap = 1f;

    // ========== 匠魂盔甲减伤配置 / Tinkers' Armor Reduction Settings ==========
    public static String tinkersArmorMode = "custom";
    public static float tinkersCapHelmet = 0.12f;
    public static float tinkersCapChestplate = 0.24f;
    public static float tinkersCapLeggings = 0.32f;
    public static float tinkersCapBoots = 0.12f;
    public static float tinkersMinecraftCap = 0.8f;
    public static float tinkersCustomK = 5.0f;
    public static boolean tinkersCustomEnableSinglePiece100 = true;
    public static float tinkersCustomSinglePieceThreshold = 1000.0f;
    public static boolean tinkersCustomEnableDamageDecay = true;
    public static float tinkersCustomDamageDecayDivisor = 100.0f;
    public static float tinkersCustomArmorMinRatio = 0.2f;
    public static String tinkersCustomFormula = "";

    // ========== 其他盔甲减伤配置（原版/模组）/ Other Armor Reduction Settings (Vanilla/Modded) ==========
    public static String otherArmorMode = "custom";
    public static float otherCapHelmet = 0.12f;
    public static float otherCapChestplate = 0.24f;
    public static float otherCapLeggings = 0.32f;
    public static float otherCapBoots = 0.12f;
    public static float otherMinecraftCap = 0.8f;
    public static float otherCustomK = 5.0f;
    public static boolean otherCustomEnableSinglePiece100 = true;
    public static float otherCustomSinglePieceThreshold = 1000.0f;
    public static boolean otherCustomEnableDamageDecay = true;
    public static float otherCustomDamageDecayDivisor = 100.0f;
    public static float otherCustomArmorMinRatio = 0.2f;
    public static String otherCustomFormula = "";

    // ========== 非玩家生物护甲减伤配置 / Non-Player Entity Armor Reduction Settings ==========
    public static String entityArmorMode = "custom";
    public static float entityMinecraftCap = 0.8f;
    public static float entityCustomK = 5.0f;
    public static boolean entityCustomEnableSinglePiece100 = true;
    public static float entityCustomSinglePieceThreshold = 1000.0f;
    public static boolean entityCustomEnableDamageDecay = true;
    public static float entityCustomDamageDecayDivisor = 100.0f;
    public static float entityCustomArmorMinRatio = 0.2f;
    public static String entityCustomFormula = "";

    // ========== 护甲全类型减免配置（是否无视 isUnblockable） ==========
    public static boolean playerIgnoreUnblockable = false;   // 玩家护甲是否对所有伤害生效
    public static boolean entityIgnoreUnblockable = false;   // 非玩家生物护甲是否对所有伤害生效

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

    // ========== 伤害计算配置 / Damage Calculation ==========
    public static boolean applyEnchantBeforeDecay = true;

    // ========== 伤害衰减配置 / Damage Decay Settings ==========
    public static float damageDecayMultiplier = 1.1f;
    public static float damageDecayCap = 25.0f;

    public static void init(File file) {
        config = new Configuration(file);
        syncConfig();
        if (config.hasChanged()) config.save();
    }

    public static void syncConfig() {
        // ========== 伤害计算配置 / Damage Calculation ==========
        applyEnchantBeforeDecay = config.getBoolean("applyEnchantBeforeDecay", "damage", true,
                "附魔加成是否在伤害衰减之前计算\nApply enchantment bonus before damage decay");

        // ========== 存储方式 / Storage ==========
        storeEnchantmentInRootNBT = config.getBoolean("storeEnchantmentInRootNBT", "storage", true,
                "附魔存储方式: true=根NBT, false=原版位置\nEnchantment storage: true=Root NBT, false=Vanilla");
        preventEnchantmentDeletion = config.getBoolean("preventEnchantmentDeletion", "storage", true,
                "防止匠魂重建时删除附魔\nPrevent enchantment deletion on rebuild");
        enchantmentStackAdditive = config.getBoolean("enchantmentStackAdditive", "storage", true,
                "远程附魔叠加方式: true=加算, false=取最大值\nRanged enchantment stacking: true=Additive, false=Max");
        preserveUpgradeItem = config.getString("preserveUpgradeItem", "storage", "minecraft:diamond",
                "附魔保护升级物品ID\nEnchantment preserver upgrade item");

        // ========== 高级附魔台配置 / Advanced Enchanting Table ==========
        advancedEnchantingTableOnlyTinker = config.getBoolean("advancedEnchantingTableOnlyTinker", "storage", true,
                "高级附魔台只允许匠魂工具/盔甲\nAdvanced table only Tinker tools/armor");
        protectExistingEnchantment = config.getBoolean("protectExistingEnchantment", "storage", true,
                "保护已有附魔不被低等级覆盖\nProtect existing enchantments");
        enchantmentAllowAdditive = config.getBoolean("enchantmentAllowAdditive", "storage", true,
                "允许附魔等级相加\nAllow enchantment level addition");
        enchantmentMode = config.getString("enchantmentMode", "storage", "merge",
                "附魔处理模式: separate=多条目, merge=合并\nEnchantment mode: separate, merge");

        // ========== 附魔等级上限 / Enchantment Level Limits ==========
        globalEnchantmentMaxLevel = config.getInt("globalEnchantmentMaxLevel", "storage", 32767, 1, 32767,
                "全局附魔等级上限\nGlobal enchantment max level");

        String[] entries = config.getStringList("enchantmentMaxLevels", "storage", new String[0],
                "特定附魔等级上限，格式: 附魔ID=最大等级\nPer-enchantment max levels, format: id=max_level");
        enchantmentMaxLevels.clear();
        for (String entry : entries) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    enchantmentMaxLevels.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {}
            }
        }

        // ========== 附魔加成物品 / Enchant Boost Items ==========
        String[] boostEntries = config.getStringList("enchantBoostItems", "storage", new String[0],
                "附魔加成物品，格式: 物品ID@加成值@最大等级\nEnchant boost items, format: item_id@boost@max_level");
        if (boostEntries.length == 0) {
            config.getCategory("storage").get("enchantBoostItems").setValues(new String[]{
                    "minecraft:diamond_block@5@10",
                    "minecraft:nether_star@1@32767"
            });
            boostEntries = config.getStringList("enchantBoostItems", "storage", new String[0], "");
        }
        enchantBoostItems.clear();
        for (String entry : boostEntries) {
            String[] parts = entry.split("@");
            if (parts.length == 3) {
                try {
                    enchantBoostItems.put(parts[0].trim(), new EnchantBoostInfo(
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())));
                } catch (NumberFormatException e) {}
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
                "力量附魔增伤系数\nPower enchantment multiplier");
        advPowerMultiplier = config.getFloat("advPowerMultiplier", "damage", 0.375f, 0f, 10f,
                "高级力量附魔增伤系数\nAdvanced Power multiplier");
        advPowerBase = config.getFloat("advPowerBase", "damage", 0.625f, 0f, 10f,
                "高级力量附魔基数\nAdvanced Power base");
        advPowerCritChancePerLevel = config.getFloat("advPowerCritChancePerLevel", "damage", 0.25f, 0f, 1f,
                "高级力量附魔每级暴击概率\nAdvanced Power crit chance per level");
        critDamageMultiplier = config.getFloat("critDamageMultiplier", "damage", 1.5f, 1f, 10f,
                "暴击伤害倍率\nCritical damage multiplier");
        speedDamageMultiplier = config.getFloat("speedDamageMultiplier", "damage", 0.1f, 0f, 32767f,
                "速度伤害加成系数\nSpeed damage multiplier");

        // ========== 伤害衰减配置 / Damage Decay ==========
        damageDecayMultiplier = config.getFloat("damageDecayMultiplier", "decay", 1.1f, 0.0f, 32767f,
                "匠魂伤害衰减系数，原版0.9\nDamage decay multiplier, vanilla 0.9");
        damageDecayCap = config.getFloat("damageDecayCap", "decay", 25.0f, 0.0f, 3276700f,
                "匠魂伤害衰减上限\nDamage decay cap");

        // ========== 盔甲保护配置（通用）/ Armor Protection (General) ==========
        resistantMaxLevel = config.getInt("resistantMaxLevel", "armor", 12, 0, 1000,
                "保护附魔最大等级\nMaximum level of protection enchantment");
        damageReductionFormula = config.getString("damageReductionFormula", "armor", "",
                "最终减伤公式（JS表达式），变量: damage, reduction\nFinal damage reduction formula, variables: damage, reduction\n留空使用原版: Math.max(reduction, damage * 0.2)");
        damageReductionCap = config.getFloat("damageReductionCap", "armor", 1f, 0.0f, 1.0f,
                "减伤上限（0.8=80%%，1.0=100%%）\nDamage reduction cap");

        // ========== 匠魂盔甲减伤配置 / Tinkers' Armor Reduction ==========
        tinkersArmorMode = config.getString("tinkersArmorMode", "armor_tinkers", "custom",
                "匠魂盔甲减伤模式: tconstruct_vanilla, minecraft_vanilla, custom\nTinkers' armor reduction mode");

        tinkersCapHelmet = config.getFloat("tinkersCapHelmet", "armor_tinkers", 0.12f, 0f, 1f,
                "匠魂原版模式: 头盔上限\nTinkers vanilla mode: helmet cap");
        tinkersCapChestplate = config.getFloat("tinkersCapChestplate", "armor_tinkers", 0.24f, 0f, 1f,
                "匠魂原版模式: 胸甲上限\nTinkers vanilla mode: chestplate cap");
        tinkersCapLeggings = config.getFloat("tinkersCapLeggings", "armor_tinkers", 0.32f, 0f, 1f,
                "匠魂原版模式: 护腿上限\nTinkers vanilla mode: leggings cap");
        tinkersCapBoots = config.getFloat("tinkersCapBoots", "armor_tinkers", 0.12f, 0f, 1f,
                "匠魂原版模式: 靴子上限\nTinkers vanilla mode: boots cap");

        tinkersMinecraftCap = config.getFloat("tinkersMinecraftCap", "armor_tinkers", 1f, 0f, 1f,
                "匠魂MC原版模式: 减伤上限\nTinkers MC vanilla mode: damage reduction cap");

        tinkersCustomK = config.getFloat("tinkersCustomK", "armor_tinkers", 5.0f, 0.1f, 100f,
                "匠魂自定义模式: 反比例公式参数 K\nTinkers custom mode: K parameter for reduction = armor/(armor+K)");
        tinkersCustomEnableSinglePiece100 = config.getBoolean("tinkersCustomEnableSinglePiece100", "armor_tinkers", true,
                "匠魂自定义模式: 总护甲≥阈值直接100%减伤\nTinkers custom: total armor >= threshold = 100% reduction");
        tinkersCustomSinglePieceThreshold = config.getFloat("tinkersCustomSinglePieceThreshold", "armor_tinkers", 1000f, 1f, 10000f,
                "匠魂自定义模式: 总护甲免伤阈值\nTinkers custom: total armor immunity threshold");
        tinkersCustomEnableDamageDecay = config.getBoolean("tinkersCustomEnableDamageDecay", "armor_tinkers", false,
                "匠魂自定义模式: 高伤害衰减\nTinkers custom: enable damage decay");
        tinkersCustomDamageDecayDivisor = config.getFloat("tinkersCustomDamageDecayDivisor", "armor_tinkers", 100f, 1f, 10000f,
                "匠魂自定义模式: 伤害衰减系数\nTinkers custom: damage decay divisor");
        tinkersCustomArmorMinRatio = config.getFloat("tinkersCustomArmorMinRatio", "armor_tinkers", 0.2f, 0f, 1f,
                "匠魂自定义模式: 护甲值保底比例\nTinkers custom: armor min ratio");
        tinkersCustomFormula = config.getString("tinkersCustomFormula", "armor_tinkers", "",
                "匠魂自定义模式: 自定义公式（JS），变量: armor, damage\nTinkers custom: custom formula, variables: armor, damage\n留空使用 K 公式");

        // ========== 其他盔甲减伤配置 / Other Armor Reduction ==========
        otherArmorMode = config.getString("otherArmorMode", "armor_other", "custom",
                "其他盔甲（原版/模组）减伤模式: tconstruct_vanilla, minecraft_vanilla, custom\nOther armor reduction mode");

        otherCapHelmet = config.getFloat("otherCapHelmet", "armor_other", 0.12f, 0f, 1f,
                "其他盔甲原版模式: 头盔上限\nOther armor vanilla mode: helmet cap");
        otherCapChestplate = config.getFloat("otherCapChestplate", "armor_other", 0.24f, 0f, 1f,
                "其他盔甲原版模式: 胸甲上限\nOther armor vanilla mode: chestplate cap");
        otherCapLeggings = config.getFloat("otherCapLeggings", "armor_other", 0.32f, 0f, 1f,
                "其他盔甲原版模式: 护腿上限\nOther armor vanilla mode: leggings cap");
        otherCapBoots = config.getFloat("otherCapBoots", "armor_other", 0.12f, 0f, 1f,
                "其他盔甲原版模式: 靴子上限\nOther armor vanilla mode: boots cap");

        otherMinecraftCap = config.getFloat("otherMinecraftCap", "armor_other", 1f, 0f, 1f,
                "其他盔甲MC原版模式: 减伤上限\nOther armor MC vanilla mode: damage reduction cap");

        otherCustomK = config.getFloat("otherCustomK", "armor_other", 5.0f, 0.1f, 100f,
                "其他盔甲自定义模式: 反比例公式参数 K\nOther armor custom mode: K parameter");
        otherCustomEnableSinglePiece100 = config.getBoolean("otherCustomEnableSinglePiece100", "armor_other", false,
                "其他盔甲自定义模式: 总护甲≥阈值直接100%减伤\nOther armor custom: total armor >= threshold = 100% reduction");
        otherCustomSinglePieceThreshold = config.getFloat("otherCustomSinglePieceThreshold", "armor_other", 1000f, 1f, 10000f,
                "其他盔甲自定义模式: 总护甲免伤阈值\nOther armor custom: total armor immunity threshold");
        otherCustomEnableDamageDecay = config.getBoolean("otherCustomEnableDamageDecay", "armor_other", true,
                "其他盔甲自定义模式: 高伤害衰减\nOther armor custom: enable damage decay");
        otherCustomDamageDecayDivisor = config.getFloat("otherCustomDamageDecayDivisor", "armor_other", 100f, 1f, 10000f,
                "其他盔甲自定义模式: 伤害衰减系数\nOther armor custom: damage decay divisor");
        otherCustomArmorMinRatio = config.getFloat("otherCustomArmorMinRatio", "armor_other", 0.2f, 0f, 1f,
                "其他盔甲自定义模式: 护甲值保底比例\nOther armor custom: armor min ratio");
        otherCustomFormula = config.getString("otherCustomFormula", "armor_other", "",
                "其他盔甲自定义模式: 自定义公式（JS），变量: armor, damage\nOther armor custom: custom formula, variables: armor, damage\n留空使用 K 公式");

        // ========== 非玩家生物护甲减伤配置 / Non-Player Entity Armor Reduction ==========
        entityArmorMode = config.getString("entityArmorMode", "armor_entity", "custom",
                "非玩家生物减伤模式: custom, minecraft_vanilla, tconstruct_vanilla\nNon-player entity armor reduction mode");

        entityMinecraftCap = config.getFloat("entityMinecraftCap", "armor_entity", 0.8f, 0f, 1f,
                "非玩家生物MC原版模式: 减伤上限\nNon-player entity MC vanilla mode: damage reduction cap");

        entityCustomK = config.getFloat("entityCustomK", "armor_entity", 5.0f, 0.1f, 100f,
                "非玩家生物自定义模式: 反比例公式参数 K\nNon-player entity custom mode: K parameter");

        entityCustomEnableSinglePiece100 = config.getBoolean("entityCustomEnableSinglePiece100", "armor_entity", false,
                "非玩家生物自定义模式: 总护甲≥阈值直接100%减伤\nNon-player entity custom: total armor >= threshold = 100% reduction");

        entityCustomSinglePieceThreshold = config.getFloat("entityCustomSinglePieceThreshold", "armor_entity", 1000f, 1f, 10000f,
                "非玩家生物自定义模式: 总护甲免伤阈值\nNon-player entity custom: total armor immunity threshold");

        entityCustomEnableDamageDecay = config.getBoolean("entityCustomEnableDamageDecay", "armor_entity", true,
                "非玩家生物自定义模式: 高伤害衰减\nNon-player entity custom: enable damage decay");

        entityCustomDamageDecayDivisor = config.getFloat("entityCustomDamageDecayDivisor", "armor_entity", 100f, 1f, 10000f,
                "非玩家生物自定义模式: 伤害衰减系数\nNon-player entity custom: damage decay divisor");

        entityCustomArmorMinRatio = config.getFloat("entityCustomArmorMinRatio", "armor_entity", 0.2f, 0f, 1f,
                "非玩家生物自定义模式: 护甲值保底比例\nNon-player entity custom: armor min ratio");

        entityCustomFormula = config.getString("entityCustomFormula", "armor_entity", "",
                "非玩家生物自定义模式: 自定义公式（JS），变量: armor, damage\nNon-player entity custom: custom formula, variables: armor, damage\n留空使用 K 公式");

        // ========== 护甲全类型减免配置（是否无视 isUnblockable） ==========
        playerIgnoreUnblockable = config.getBoolean("playerIgnoreUnblockable", "armor", false,
                "玩家护甲是否对所有伤害生效（无视isUnblockable标记）\n" +
                        "false: 只减免物理伤害（原版行为）\n" +
                        "true: 减免所有类型伤害（包括魔法、火焰、破甲等）");

        entityIgnoreUnblockable = config.getBoolean("entityIgnoreUnblockable", "armor", false,
                "非玩家生物护甲是否对所有伤害生效（无视isUnblockable标记）\n" +
                        "false: 只减免物理伤害（原版行为）\n" +
                        "true: 减免所有类型伤害（包括魔法、火焰、破甲等）");
    }

    // ========== Getter 方法 ==========
    public static int getEnchantmentMaxLevel(String enchantmentId) {
        return enchantmentMaxLevels.getOrDefault(enchantmentId, globalEnchantmentMaxLevel);
    }

    public static float getDamageDecayMultiplier() { return damageDecayMultiplier; }
    public static float getDamageDecayCap() { return damageDecayCap; }
    public static boolean applyEnchantBeforeDecay() { return applyEnchantBeforeDecay; }
    public static int getResistantMaxLevel() { return resistantMaxLevel; }
    public static String getDamageReductionFormula() { return damageReductionFormula; }
    public static float getDamageReductionCap() { return damageReductionCap; }

    // 匠魂盔甲 Getter
    public static String getTinkersArmorMode() { return tinkersArmorMode; }
    public static float getTinkersCapHelmet() { return tinkersCapHelmet; }
    public static float getTinkersCapChestplate() { return tinkersCapChestplate; }
    public static float getTinkersCapLeggings() { return tinkersCapLeggings; }
    public static float getTinkersCapBoots() { return tinkersCapBoots; }
    public static float getTinkersMinecraftCap() { return tinkersMinecraftCap; }
    public static float getTinkersCustomK() { return tinkersCustomK; }
    public static boolean getTinkersCustomEnableSinglePiece100() { return tinkersCustomEnableSinglePiece100; }
    public static float getTinkersCustomSinglePieceThreshold() { return tinkersCustomSinglePieceThreshold; }
    public static boolean getTinkersCustomEnableDamageDecay() { return tinkersCustomEnableDamageDecay; }
    public static float getTinkersCustomDamageDecayDivisor() { return tinkersCustomDamageDecayDivisor; }
    public static float getTinkersCustomArmorMinRatio() { return tinkersCustomArmorMinRatio; }
    public static String getTinkersCustomFormula() { return tinkersCustomFormula; }

    // 其他盔甲 Getter
    public static String getOtherArmorMode() { return otherArmorMode; }
    public static float getOtherCapHelmet() { return otherCapHelmet; }
    public static float getOtherCapChestplate() { return otherCapChestplate; }
    public static float getOtherCapLeggings() { return otherCapLeggings; }
    public static float getOtherCapBoots() { return otherCapBoots; }
    public static float getOtherMinecraftCap() { return otherMinecraftCap; }
    public static float getOtherCustomK() { return otherCustomK; }
    public static boolean getOtherCustomEnableSinglePiece100() { return otherCustomEnableSinglePiece100; }
    public static float getOtherCustomSinglePieceThreshold() { return otherCustomSinglePieceThreshold; }
    public static boolean getOtherCustomEnableDamageDecay() { return otherCustomEnableDamageDecay; }
    public static float getOtherCustomDamageDecayDivisor() { return otherCustomDamageDecayDivisor; }
    public static float getOtherCustomArmorMinRatio() { return otherCustomArmorMinRatio; }
    public static String getOtherCustomFormula() { return otherCustomFormula; }

    // 非玩家生物 Getter
    public static String getEntityArmorMode() { return entityArmorMode; }
    public static float getEntityMinecraftCap() { return entityMinecraftCap; }
    public static float getEntityCustomK() { return entityCustomK; }
    public static boolean getEntityCustomEnableSinglePiece100() { return entityCustomEnableSinglePiece100; }
    public static float getEntityCustomSinglePieceThreshold() { return entityCustomSinglePieceThreshold; }
    public static boolean getEntityCustomEnableDamageDecay() { return entityCustomEnableDamageDecay; }
    public static float getEntityCustomDamageDecayDivisor() { return entityCustomDamageDecayDivisor; }
    public static float getEntityCustomArmorMinRatio() { return entityCustomArmorMinRatio; }
    public static String getEntityCustomFormula() { return entityCustomFormula; }

    // 护甲全类型减免 Getter
    public static boolean shouldPlayerIgnoreUnblockable() { return playerIgnoreUnblockable; }
    public static boolean shouldEntityIgnoreUnblockable() { return entityIgnoreUnblockable; }
}