    package com.crashguard.util;

    import com.crashguard.config.ConfigHandler;
    import com.google.common.collect.Multimap;
    import net.minecraft.entity.EntityLivingBase;
    import net.minecraft.entity.ai.attributes.AttributeModifier;
    import net.minecraft.entity.player.EntityPlayer;
    import net.minecraft.inventory.EntityEquipmentSlot;
    import net.minecraft.item.ItemArmor;
    import net.minecraft.item.ItemStack;
    import net.minecraft.server.MinecraftServer;
    import net.minecraft.util.DamageSource;
    import net.minecraftforge.common.ISpecialArmor;
    import net.minecraftforge.fml.common.FMLCommonHandler;
    import org.apache.commons.lang3.StringUtils;

    import javax.script.ScriptEngine;
    import javax.script.ScriptEngineManager;
    import javax.script.ScriptException;
    import java.util.Collection;

    public class ArmorReductionHelper {

        private static ScriptEngine engine;

        static {
            try {
                engine = new ScriptEngineManager().getEngineByName("JavaScript");
            } catch (Exception e) {
                engine = null;
            }
        }
        public static void resetProcessed() {
            // 保留空方法
        }


        // ==================== 盔甲类型检测 ====================

        public static boolean isTinkersArmor(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return stack.getItem() instanceof slimeknights.tconstruct.library.tools.ToolCore;
        }

        public static boolean isVanillaArmor(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return stack.getItem() instanceof ItemArmor;
        }

        // ==================== 护甲值获取 ====================

        public static float getArmorValue(ItemStack stack, EntityEquipmentSlot slot) {
            if (stack.isEmpty()) return 0;

            if (isTinkersArmor(stack)) {
                try {
                    Class<?> armorHelperClass = Class.forName("c4.conarm.common.armor.utils.ArmorHelper");
                    java.lang.reflect.Method getDefenseMethod = armorHelperClass.getMethod("getDefense", ItemStack.class);
                    Object result = getDefenseMethod.invoke(null, stack);
                    if (result instanceof Float) {
                        return (Float) result;
                    }
                } catch (Exception e) {}
            }

            Multimap<String, AttributeModifier> modifiers = stack.getAttributeModifiers(slot);
            Collection<AttributeModifier> armorModifiers = modifiers.get("generic.armor");
            double total = 0;
            for (AttributeModifier mod : armorModifiers) {
                total += mod.getAmount();
            }

            if (total == 0 && stack.getItem() instanceof ItemArmor) {
                total = ((ItemArmor) stack.getItem()).damageReduceAmount;
            }

            return (float) total;
        }

        public static float getMaxPieceArmor(EntityLivingBase entity) {
            if (entity == null) return 0;
            float max = 0;
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
                ItemStack armor = entity.getItemStackFromSlot(slot);
                float value = getArmorValue(armor, slot);
                if (value > max) max = value;
            }
            return max;
        }

        public static float getTotalArmor(EntityLivingBase entity) {
            if (entity == null) return 0;
            float total = 0;
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
                total += getArmorValue(entity.getItemStackFromSlot(slot), slot);
            }
            return total;
        }

        // 获取真实总护甲值（不 cap，供生物使用）
        public static int getRealTotalArmor(EntityLivingBase entity) {
            if (entity == null) return 0;

            double attributeArmor = entity.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ARMOR).getAttributeValue();

            float armorSum = 0;
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
                armorSum += getArmorValue(entity.getItemStackFromSlot(slot), slot);
            }

            return (int) (attributeArmor + armorSum);
        }

        // ==================== 玩家护甲减伤 ====================


        // ==================== 非玩家生物护甲减伤 ====================

        public static float calculateEntityReduction(EntityLivingBase entity, float damage) {
            if (entity == null || damage <= 0) return 0;
            float totalArmor = getRealTotalArmor(entity);
            float reduction = calculateEntityReduction(totalArmor, damage);
            return reduction;
        }

        // ==================== 减伤计算核心 ====================

        private static float evalFormula(String formula, float armor, float damage, float maxPiece) {
            if (StringUtils.isBlank(formula)) return -1f;
            try {
                ScriptEngine eng = new ScriptEngineManager().getEngineByName("JavaScript");
                if (eng == null) return -1f;
                eng.put("armor", armor);
                eng.put("damage", damage);
                eng.put("maxPiece", maxPiece);
                Object result = eng.eval(formula);
                if (result instanceof Number) {
                    return Math.max(0, Math.min(1, ((Number) result).floatValue()));
                }
            } catch (ScriptException e) {}
            return -1f;
        }

        public static float calculateReduction(float totalArmor, float damage,
                                               String mode, float minecraftCap, float slotCap,
                                               float customK, boolean enableTotalThreshold, float totalThreshold,
                                               boolean enableDamageDecay, float decayDivisor, float minRatio,
                                               String customFormula) {
            float custom = evalFormula(customFormula, totalArmor, damage, totalArmor);
            if (custom >= 0) return custom;

            switch (mode) {
                case "minecraft_vanilla":
                    float red = totalArmor / 25f;
                    return Math.min(red, minecraftCap);
                case "tconstruct_vanilla":
                    red = totalArmor / 25f;
                    return Math.min(red, slotCap);
                case "custom":
                default:
                    if (enableTotalThreshold && totalArmor >= totalThreshold) {
                        return 1.0f;
                    }
                    float effective = totalArmor;
                    if (enableDamageDecay && damage > 0) {
                        effective = totalArmor / (1 + damage / decayDivisor);
                        effective = Math.max(effective, totalArmor * minRatio);
                    }
                    return effective / (effective + customK);
            }
        }

        // ==================== 原版盔甲专用入口 ====================
        public static float calculateVanillaReduction(float totalArmor, float damage) {
            return calculateReduction(
                    totalArmor, damage,
                    ConfigHandler.getOtherArmorMode(),
                    ConfigHandler.getOtherMinecraftCap(), 0f,
                    ConfigHandler.getOtherCustomK(),
                    ConfigHandler.getOtherCustomEnableSinglePiece100(),
                    ConfigHandler.getOtherCustomSinglePieceThreshold(),
                    ConfigHandler.getOtherCustomEnableDamageDecay(),
                    ConfigHandler.getOtherCustomDamageDecayDivisor(),
                    ConfigHandler.getOtherCustomArmorMinRatio(),
                    ConfigHandler.getOtherCustomFormula()
            );
        }

        // ==================== 匠魂盔甲专用入口 ====================
        public static float calculateTinkersReduction(float totalArmor, float damage, EntityEquipmentSlot slot) {
            float slotCap = 0f;
            if (ConfigHandler.getTinkersArmorMode().equals("tconstruct_vanilla")) {
                if (slot != null) {
                    switch (slot) {
                        case HEAD: slotCap = ConfigHandler.getTinkersCapHelmet(); break;
                        case CHEST: slotCap = ConfigHandler.getTinkersCapChestplate(); break;
                        case LEGS: slotCap = ConfigHandler.getTinkersCapLeggings(); break;
                        case FEET: slotCap = ConfigHandler.getTinkersCapBoots(); break;
                        default: slotCap = 0.12f;
                    }
                } else {
                    // 没有具体槽位时，取四个槽位的平均值或最小值？这里用头盔值
                    slotCap = ConfigHandler.getTinkersCapHelmet();
                }
            }
            return calculateReduction(
                    totalArmor, damage,
                    ConfigHandler.getTinkersArmorMode(),
                    ConfigHandler.getTinkersMinecraftCap(), slotCap,
                    ConfigHandler.getTinkersCustomK(),
                    ConfigHandler.getTinkersCustomEnableSinglePiece100(),
                    ConfigHandler.getTinkersCustomSinglePieceThreshold(),
                    ConfigHandler.getTinkersCustomEnableDamageDecay(),
                    ConfigHandler.getTinkersCustomDamageDecayDivisor(),
                    ConfigHandler.getTinkersCustomArmorMinRatio(),
                    ConfigHandler.getTinkersCustomFormula()
            );
        }

        // 非玩家生物专用入口（重载版本）
        public static float calculateEntityReduction(float totalArmor, float damage) {
            return calculateReduction(
                    totalArmor, damage,
                    ConfigHandler.getEntityArmorMode(),
                    ConfigHandler.getEntityMinecraftCap(), 0f,
                    ConfigHandler.getEntityCustomK(),
                    ConfigHandler.getEntityCustomEnableSinglePiece100(),
                    ConfigHandler.getEntityCustomSinglePieceThreshold(),
                    ConfigHandler.getEntityCustomEnableDamageDecay(),
                    ConfigHandler.getEntityCustomDamageDecayDivisor(),
                    ConfigHandler.getEntityCustomArmorMinRatio(),
                    ConfigHandler.getEntityCustomFormula()
            );
        }

        // 非玩家生物专用减伤计算（直接传实体）
        public static float calculateEntityReduction(EntityLivingBase entity, DamageSource source, float damage) {
            float totalArmor = getRealTotalArmor(entity);
            return calculateEntityReduction(totalArmor, damage);
        }

        // ==================== 减伤计算（根据盔甲类型选择配置）====================
        public static float calculateReduction(ItemStack armorStack, float totalArmor, float damage, float maxPieceArmor) {
            boolean isTinkers = isTinkersArmor(armorStack);

            if (isTinkers) {
                return calculateReductionByConfig(
                        ConfigHandler.getTinkersArmorMode(),
                        totalArmor, damage, maxPieceArmor,
                        ConfigHandler.getTinkersCapHelmet(),
                        ConfigHandler.getTinkersCapChestplate(),
                        ConfigHandler.getTinkersCapLeggings(),
                        ConfigHandler.getTinkersCapBoots(),
                        ConfigHandler.getTinkersMinecraftCap(),
                        ConfigHandler.getTinkersCustomK(),
                        ConfigHandler.getTinkersCustomEnableSinglePiece100(),
                        ConfigHandler.getTinkersCustomSinglePieceThreshold(),
                        ConfigHandler.getTinkersCustomEnableDamageDecay(),
                        ConfigHandler.getTinkersCustomDamageDecayDivisor(),
                        ConfigHandler.getTinkersCustomArmorMinRatio(),
                        ConfigHandler.getTinkersCustomFormula()
                );
            } else {
                return calculateReductionByConfig(
                        ConfigHandler.getOtherArmorMode(),
                        totalArmor, damage, maxPieceArmor,
                        ConfigHandler.getOtherCapHelmet(),
                        ConfigHandler.getOtherCapChestplate(),
                        ConfigHandler.getOtherCapLeggings(),
                        ConfigHandler.getOtherCapBoots(),
                        ConfigHandler.getOtherMinecraftCap(),
                        ConfigHandler.getOtherCustomK(),
                        ConfigHandler.getOtherCustomEnableSinglePiece100(),
                        ConfigHandler.getOtherCustomSinglePieceThreshold(),
                        ConfigHandler.getOtherCustomEnableDamageDecay(),
                        ConfigHandler.getOtherCustomDamageDecayDivisor(),
                        ConfigHandler.getOtherCustomArmorMinRatio(),
                        ConfigHandler.getOtherCustomFormula()
                );
            }
        }

        private static float calculateReductionByConfig(
                String mode, float totalArmor, float damage, float maxPieceArmor,
                float capHelmet, float capChestplate, float capLeggings, float capBoots,
                float minecraftCap, float customK, boolean enableSinglePiece100,
                float singlePieceThreshold, boolean enableDamageDecay,
                float damageDecayDivisor, float armorMinRatio, String customFormula) {

            switch (mode) {
                case "tconstruct_vanilla":
                    return calculateTConstructVanilla(totalArmor, Math.min(capHelmet, Math.min(capChestplate, Math.min(capLeggings, capBoots))));
                case "minecraft_vanilla":
                    return calculateMinecraftVanilla(totalArmor, minecraftCap);
                case "custom":
                default:
                    return calculateCustom(totalArmor, damage, maxPieceArmor,
                            customK, enableSinglePiece100, singlePieceThreshold,
                            enableDamageDecay, damageDecayDivisor, armorMinRatio, customFormula);
            }
        }

        public static float calculateReductionWithSlot(ItemStack armorStack, float totalArmor, float damage, float maxPieceArmor, EntityEquipmentSlot slot) {
            boolean isTinkers = isTinkersArmor(armorStack);

            if (isTinkers) {
                float slotCap = getSlotCap(slot,
                        ConfigHandler.getTinkersCapHelmet(),
                        ConfigHandler.getTinkersCapChestplate(),
                        ConfigHandler.getTinkersCapLeggings(),
                        ConfigHandler.getTinkersCapBoots());
                return calculateReductionByConfigWithCap(
                        ConfigHandler.getTinkersArmorMode(),
                        totalArmor, damage, maxPieceArmor, slotCap,
                        ConfigHandler.getTinkersMinecraftCap(),
                        ConfigHandler.getTinkersCustomK(),
                        ConfigHandler.getTinkersCustomEnableSinglePiece100(),
                        ConfigHandler.getTinkersCustomSinglePieceThreshold(),
                        ConfigHandler.getTinkersCustomEnableDamageDecay(),
                        ConfigHandler.getTinkersCustomDamageDecayDivisor(),
                        ConfigHandler.getTinkersCustomArmorMinRatio(),
                        ConfigHandler.getTinkersCustomFormula()
                );
            } else {
                float slotCap = getSlotCap(slot,
                        ConfigHandler.getOtherCapHelmet(),
                        ConfigHandler.getOtherCapChestplate(),
                        ConfigHandler.getOtherCapLeggings(),
                        ConfigHandler.getOtherCapBoots());
                return calculateReductionByConfigWithCap(
                        ConfigHandler.getOtherArmorMode(),
                        totalArmor, damage, maxPieceArmor, slotCap,
                        ConfigHandler.getOtherMinecraftCap(),
                        ConfigHandler.getOtherCustomK(),
                        ConfigHandler.getOtherCustomEnableSinglePiece100(),
                        ConfigHandler.getOtherCustomSinglePieceThreshold(),
                        ConfigHandler.getOtherCustomEnableDamageDecay(),
                        ConfigHandler.getOtherCustomDamageDecayDivisor(),
                        ConfigHandler.getOtherCustomArmorMinRatio(),
                        ConfigHandler.getOtherCustomFormula()
                );
            }
        }

        private static float getSlotCap(EntityEquipmentSlot slot, float capHelmet, float capChestplate, float capLeggings, float capBoots) {
            if (slot == EntityEquipmentSlot.HEAD) return capHelmet;
            if (slot == EntityEquipmentSlot.CHEST) return capChestplate;
            if (slot == EntityEquipmentSlot.LEGS) return capLeggings;
            if (slot == EntityEquipmentSlot.FEET) return capBoots;
            return 0.12f;
        }

        private static float calculateReductionByConfigWithCap(
                String mode, float totalArmor, float damage, float maxPieceArmor, float slotCap,
                float minecraftCap, float customK, boolean enableSinglePiece100,
                float singlePieceThreshold, boolean enableDamageDecay,
                float damageDecayDivisor, float armorMinRatio, String customFormula) {

            switch (mode) {
                case "tconstruct_vanilla":
                    return calculateTConstructVanilla(totalArmor, slotCap);
                case "minecraft_vanilla":
                    return calculateMinecraftVanilla(totalArmor, minecraftCap);
                case "custom":
                default:
                    return calculateCustom(totalArmor, damage, maxPieceArmor,
                            customK, enableSinglePiece100, singlePieceThreshold,
                            enableDamageDecay, damageDecayDivisor, armorMinRatio, customFormula);
            }
        }

        // ==================== 核心公式 ====================

        private static float calculateTConstructVanilla(float totalArmor, float slotCap) {
            float reduction = totalArmor / 25f;
            return Math.min(reduction, slotCap);
        }

        private static float calculateMinecraftVanilla(float totalArmor, float cap) {
            float reduction = totalArmor / 25f;
            return Math.min(reduction, cap);
        }

        private static float calculateCustom(float totalArmor, float damage, float maxPieceArmor,
                                             float k, boolean enableSinglePiece100, float singlePieceThreshold,
                                             boolean enableDamageDecay, float damageDecayDivisor,
                                             float armorMinRatio, String customFormula) {

            if (enableSinglePiece100 && maxPieceArmor >= singlePieceThreshold) {
                return 1.0f;
            }

            float effectiveArmor = totalArmor;

            if (enableDamageDecay && damage > 0) {
                effectiveArmor = totalArmor / (1 + damage / damageDecayDivisor);
                effectiveArmor = Math.max(effectiveArmor, totalArmor * armorMinRatio);
            }

            if (StringUtils.isNotBlank(customFormula) && engine != null) {
                try {
                    String expr = customFormula
                            .replace("armor", Float.toString(effectiveArmor))
                            .replace("damage", Float.toString(damage))
                            .replace("maxPiece", Float.toString(maxPieceArmor));
                    Object result = engine.eval(expr);
                    if (result instanceof Number) {
                        float reduction = ((Number) result).floatValue();
                        return Math.max(0, Math.min(1, reduction));
                    }
                } catch (ScriptException e) {}
            }

            float reduction = effectiveArmor / (effectiveArmor + k);
            return Math.max(0, Math.min(1, reduction));
        }

        // ==================== 辅助方法 ====================

        public static EntityLivingBase getEntityFromArmorStack(ItemStack armorStack) {
            if (armorStack == null || armorStack.isEmpty()) return null;

            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                    for (ItemStack armor : player.getArmorInventoryList()) {
                        if (armor == armorStack) return player;
                    }
                }
                for (net.minecraft.world.WorldServer world : server.worlds) {
                    for (EntityLivingBase entity : world.getEntities(EntityLivingBase.class, e -> true)) {
                        for (ItemStack armor : entity.getArmorInventoryList()) {
                            if (armor == armorStack) return entity;
                        }
                    }
                }
            }

            try {
                net.minecraft.world.World clientWorld = net.minecraft.client.Minecraft.getMinecraft().world;
                if (clientWorld != null) {
                    for (EntityPlayer player : clientWorld.playerEntities) {
                        for (ItemStack armor : player.getArmorInventoryList()) {
                            if (armor == armorStack) return player;
                        }
                    }
                }
            } catch (Exception e) {}

            return null;
        }

        public static EntityLivingBase getPlayerFromArmorStack(ItemStack armorStack) {
            return getEntityFromArmorStack(armorStack);
        }

        // ==================== ASM 调用接口 ====================

        public static ISpecialArmor.ArmorProperties modifyArmorProperties(
                ISpecialArmor.ArmorProperties original,
                ItemStack armorStack,
                double damage,
                float totalArmorFromConArm,
                EntityEquipmentSlot slot) {

            if (original == null) return original;

            EntityLivingBase entity = getEntityFromArmorStack(armorStack);

            if (entity == null || !(entity instanceof EntityPlayer)) {
                float reduction = calculateEntityReduction(totalArmorFromConArm, (float) damage);
                return new ISpecialArmor.ArmorProperties(0, reduction, (int) damage);
            }

            EntityPlayer player = (EntityPlayer) entity;
            float totalArmor = getTotalArmor(player);
            float maxPieceArmor = getMaxPieceArmor(player);
            float reduction = calculateReductionWithSlot(armorStack, totalArmor, (float) damage, maxPieceArmor, slot);
            return new ISpecialArmor.ArmorProperties(0, reduction, (int) damage);
        }

        public static ISpecialArmor.ArmorProperties createArmorProperties(
                ItemStack armorStack,
                double damage,
                float totalArmor,
                EntityEquipmentSlot slot) {

            EntityLivingBase player = getPlayerFromArmorStack(armorStack);
            float maxPieceArmor = 0;
            if (player != null) {
                maxPieceArmor = getMaxPieceArmor(player);
            }
            float reduction = calculateReductionWithSlot(armorStack, totalArmor, (float) damage, maxPieceArmor, slot);
            return new ISpecialArmor.ArmorProperties(0, reduction, (int) damage);
        }
        // 统一的护甲减伤计算
        // 统一的护甲减伤计算
        public static float calculateArmorReduction(EntityLivingBase entity, float damage) {
            if (entity == null || damage <= 0) return damage;

            float totalArmor = getRealTotalArmor(entity);

            // 检查是否有匠魂盔甲
            boolean hasTinkersArmor = false;
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
                ItemStack armor = entity.getItemStackFromSlot(slot);
                if (isTinkersArmor(armor)) {
                    hasTinkersArmor = true;
                    break;
                }
            }

            float reduction;
            if (hasTinkersArmor) {
                // 匠魂盔甲
                reduction = calculateTinkersReduction(totalArmor, damage, null);
            } else if (entity instanceof EntityPlayer) {
                // 玩家穿非匠魂盔甲
                reduction = calculateVanillaReduction(totalArmor, damage);
            } else {
                // 非玩家生物
                reduction = calculateEntityReduction(totalArmor, damage);
            }

            return damage * (1 - reduction);
        }
    }