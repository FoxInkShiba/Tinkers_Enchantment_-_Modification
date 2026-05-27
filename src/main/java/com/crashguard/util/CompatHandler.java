package com.crashguard.util;

import com.crashguard.config.ConfigHandler;
import com.crashguard.modifier.EnchantmentPreserverModifier;
import com.crashguard.modifier.ModEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import slimeknights.tconstruct.library.entity.EntityProjectileBase;
import slimeknights.tconstruct.library.tools.ToolCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CompatHandler {

    private static final String BOW_ENCHANTS_KEY = "CrashGuardBowEnch";
    private static final ResourceLocation ADV_POWER_ID = new ResourceLocation("somanyenchantments", "advancedpower");
    private static final String TAG_PRESERVE_ENCHANT = "crashguard_preserve_enchant";

    private static Field tinkerProjectileField = null;
    private static Method getItemStackMethod = null;

    static {
        try {
            tinkerProjectileField = EntityProjectileBase.class.getDeclaredField("tinkerProjectile");
            tinkerProjectileField.setAccessible(true);
            Class<?> handlerClass = tinkerProjectileField.getType();
            getItemStackMethod = handlerClass.getMethod("getItemStack");
            System.out.println("[CrashGuard] Successfully initialized reflection for arrow stack");
        } catch (Exception e) {
            System.out.println("[CrashGuard] Failed to initialize reflection: " + e.getMessage());
        }
    }

    // ========== 工具判断 ==========
    public static boolean isTinkerTool(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ToolCore;
    }

    // ========== 附魔读写（通过 ModEnchantment）==========
    public static int getEnchantmentLevelFromTinkerTool(Enchantment ench, ItemStack stack) {
        if (!isTinkerTool(stack)) return 0;
        return ModEnchantment.getLevel(stack, ench);
    }

    public static void mergeTinkerEnchantments(Map<Enchantment, Integer> map, ItemStack stack) {
        if (!isTinkerTool(stack)) return;
        Map<Enchantment, Integer> enchants = ModEnchantment.getAllEnchantments(stack);
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            map.put(e.getKey(), Math.max(map.getOrDefault(e.getKey(), 0), e.getValue()));
        }
    }

    public static boolean addEnchantment(ItemStack stack, Enchantment ench, int level) {
        if (!isTinkerTool(stack)) return false;
        System.out.println("[CrashGuard] Added enchant " + ench.getRegistryName() + " level " + level);
        return ModEnchantment.addEnchantment(stack, ench, level);
    }

    // ========== 远程伤害辅助方法 ==========
    private static float getProjectileSpeed(EntityProjectileBase projectile) {
        return (float) Math.sqrt(projectile.motionX * projectile.motionX +
                projectile.motionY * projectile.motionY +
                projectile.motionZ * projectile.motionZ);
    }

    private static ItemStack getArrowStack(EntityProjectileBase projectile) {
        if (tinkerProjectileField == null || getItemStackMethod == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object handler = tinkerProjectileField.get(projectile);
            if (handler != null) {
                return (ItemStack) getItemStackMethod.invoke(handler);
            }
        } catch (Exception e) {
            System.out.println("[CrashGuard] Failed to get arrow stack: " + e.getMessage());
        }
        return ItemStack.EMPTY;
    }

    // ========== 近战附魔伤害 ==========
    public static float getMeleeEnchantmentDamage(ItemStack stack, Entity target) {
        if (!ConfigHandler.enableMeleeEnchant) return 0;
        if (!(target instanceof EntityLivingBase)) return 0;

        EntityLivingBase livingTarget = (EntityLivingBase) target;
        float extra = EnchantmentHelper.getModifierForCreature(stack, livingTarget.getCreatureAttribute());
        if (extra > 0) {
            System.out.println("[CrashGuard] Melee extra damage: " + extra);
        }
        return extra;
    }

    // ========== 远程附魔伤害 ==========
    public static float getRangedEnchantmentDamage(ItemStack stack, Entity target, Entity projectile) {
        if (!ConfigHandler.enableRangedEnchant) return 0;
        if (!(target instanceof EntityLivingBase)) return 0;
        if (!(projectile instanceof EntityProjectileBase)) return 0;

        EntityProjectileBase proj = (EntityProjectileBase) projectile;
        EntityLivingBase livingTarget = (EntityLivingBase) target;

        Map<Enchantment, Integer> merged = new HashMap<>();

        // 从发射者手上获取弓的附魔
        if (proj.shootingEntity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) proj.shootingEntity;
            ItemStack bowStack = player.getHeldItemMainhand();
            if (!(bowStack.getItem() instanceof ToolCore)) {
                bowStack = player.getHeldItemOffhand();
            }
            if (bowStack.getItem() instanceof ToolCore) {
                Map<Enchantment, Integer> bowEnchants = ModEnchantment.getAllEnchantments(bowStack);
                for (Map.Entry<Enchantment, Integer> entry : bowEnchants.entrySet()) {
                    if (ConfigHandler.enchantmentStackAdditive) {
                        merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    } else {
                        merged.merge(entry.getKey(), entry.getValue(), Math::max);
                    }
                    System.out.println("[CrashGuard]   bow from player: " + entry.getKey().getRegistryName() + " lvl=" + entry.getValue());
                }
            }
        }

        // 从箭获取附魔
        ItemStack arrow = getArrowStack(proj);
        if (!arrow.isEmpty()) {
            Map<Enchantment, Integer> arrowEnchants = EnchantmentHelper.getEnchantments(arrow);
            for (Map.Entry<Enchantment, Integer> e : arrowEnchants.entrySet()) {
                if (ConfigHandler.enchantmentStackAdditive) {
                    merged.merge(e.getKey(), e.getValue(), Integer::sum);
                } else {
                    merged.merge(e.getKey(), e.getValue(), Math::max);
                }
                System.out.println("[CrashGuard]   arrow: " + e.getKey().getRegistryName() + " lvl=" + e.getValue());
            }
        }

        // 获取基础伤害
        float baseDamage = 1.0f;
        if (proj.shootingEntity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) proj.shootingEntity;
            baseDamage = (float) player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        }

        float extra = 0;
        int powerLevel = 0;
        int advPowerLevel = 0;

        for (Map.Entry<Enchantment, Integer> e : merged.entrySet()) {
            Enchantment ench = e.getKey();
            int lvl = e.getValue();

            if (ench == Enchantments.POWER) {
                powerLevel = lvl;
                System.out.println("[CrashGuard]   Power lvl=" + lvl);
            } else if (ench.getRegistryName().equals(ADV_POWER_ID)) {
                advPowerLevel = lvl;
                System.out.println("[CrashGuard]   Advanced Power lvl=" + lvl);
            } else {
                extra += ench.calcDamageByCreature(lvl, livingTarget.getCreatureAttribute());
                if (extra > 0) {
                    System.out.println("[CrashGuard]   " + ench.getRegistryName() + " added " + extra);
                }
            }
        }

        if (powerLevel > 0) {
            float powerBonus = baseDamage * ConfigHandler.powerMultiplier * (powerLevel + 1);
            extra += powerBonus;
            System.out.println("[CrashGuard]   Power bonus: " + powerBonus);
        }

        if (advPowerLevel > 0) {
            float advPowerBonus = baseDamage * (ConfigHandler.advPowerMultiplier * advPowerLevel + ConfigHandler.advPowerBase);
            extra += advPowerBonus;
            System.out.println("[CrashGuard]   Advanced Power bonus: " + advPowerBonus);

            if (proj.world.rand.nextFloat() < advPowerLevel * ConfigHandler.advPowerCritChancePerLevel) {
                extra *= ConfigHandler.critDamageMultiplier;
                proj.setIsCritical(true);
                System.out.println("[CrashGuard]   CRITICAL HIT! extra=" + extra);
            }
        }

        if (ConfigHandler.enableSpeedBonus) {
            float speed = getProjectileSpeed(proj);
            extra *= (1.0f + speed * ConfigHandler.speedDamageMultiplier);
            System.out.println("[CrashGuard]   speed=" + speed + ", multiplier=" + (1.0f + speed * ConfigHandler.speedDamageMultiplier));
        }

        System.out.println("[CrashGuard]   getRangedEnchantmentDamage returning extra=" + extra);
        return extra;
    }

    // ========== 统一附魔伤害入口 ==========
    public static float getUnifiedEnchantmentDamage(ItemStack stack, Entity target, Entity projectile) {
        if (projectile != null) {
            return getRangedEnchantmentDamage(stack, target, projectile);
        } else {
            return getMeleeEnchantmentDamage(stack, target);
        }
    }

    // ========== 弓附魔保存到投射物 ==========
    public static void saveBowEnchantments(EntityProjectileBase projectile, ItemStack bowStack) {
        if (bowStack == null || bowStack.isEmpty()) return;
        if (!ConfigHandler.enableRangedEnchant) return;

        Map<Enchantment, Integer> bowEnchants = ModEnchantment.getAllEnchantments(bowStack);
        if (!bowEnchants.isEmpty()) {
            NBTTagCompound tag = new NBTTagCompound();
            projectile.writeToNBT(tag);

            NBTTagList list = new NBTTagList();
            for (Map.Entry<Enchantment, Integer> entry : bowEnchants.entrySet()) {
                NBTTagCompound enchTag = new NBTTagCompound();
                enchTag.setString("id", entry.getKey().getRegistryName().toString());
                enchTag.setInteger("lvl", entry.getValue());
                list.appendTag(enchTag);
            }
            tag.setTag(BOW_ENCHANTS_KEY, list);
            projectile.readFromNBT(tag);
            System.out.println("[CrashGuard] Saved " + list.tagCount() + " enchantments to projectile");
        }
    }

    public static void writeBowEnchantmentsToNBT(EntityProjectileBase projectile, NBTTagCompound tag) {
        NBTTagCompound tempTag = new NBTTagCompound();
        projectile.writeToNBT(tempTag);
        if (tempTag.hasKey(BOW_ENCHANTS_KEY, Constants.NBT.TAG_LIST)) {
            tag.setTag(BOW_ENCHANTS_KEY, tempTag.getTagList(BOW_ENCHANTS_KEY, Constants.NBT.TAG_COMPOUND));
        }
    }

    public static void readBowEnchantmentsFromNBT(EntityProjectileBase projectile, NBTTagCompound tag) {
        if (tag.hasKey(BOW_ENCHANTS_KEY, Constants.NBT.TAG_LIST)) {
            NBTTagCompound tempTag = new NBTTagCompound();
            projectile.writeToNBT(tempTag);
            tempTag.setTag(BOW_ENCHANTS_KEY, tag.getTagList(BOW_ENCHANTS_KEY, Constants.NBT.TAG_COMPOUND));
            projectile.readFromNBT(tempTag);
            System.out.println("[CrashGuard] Read bow enchantments from NBT");
        }
    }

    // ========== 强制保留标记检查 ==========
    private static boolean hasForcePreserveMark(NBTTagCompound rootTag) {
        if (rootTag == null) return false;
        return rootTag.getBoolean(TAG_PRESERVE_ENCHANT);
    }

    // ========== 工具附魔保存恢复（供 ToolBuilderTransformer 调用）==========
    public static NBTTagList saveEnchantmentsBeforeRebuild(NBTTagCompound rootNBT) {
        if (rootNBT == null) return new NBTTagList();

        boolean forcePreserve = hasForcePreserveMark(rootNBT);
        if (!ConfigHandler.preventEnchantmentDeletion && !forcePreserve) return new NBTTagList();

        if (rootNBT.hasKey("ench", Constants.NBT.TAG_LIST)) {
            NBTTagList saved = rootNBT.getTagList("ench", Constants.NBT.TAG_COMPOUND).copy();
            if (saved.tagCount() > 0) {
                System.out.println("[CrashGuard] Saved " + saved.tagCount() + " enchantments (force=" + forcePreserve + ")");
            }
            return saved;
        }
        return new NBTTagList();
    }

    public static void restoreEnchantmentsAfterRebuild(NBTTagCompound rootNBT, NBTTagList savedEnch) {
        if (rootNBT == null) return;
        if (savedEnch == null || savedEnch.tagCount() == 0) return;

        boolean forcePreserve = hasForcePreserveMark(rootNBT);
        if (!ConfigHandler.preventEnchantmentDeletion && !forcePreserve) return;

        rootNBT.setTag("ench", savedEnch);
        System.out.println("[CrashGuard] Restored " + savedEnch.tagCount() + " enchantments (force=" + forcePreserve + ")");
    }

    // ========== 盔甲附魔保存恢复（供 ArmorBuilderTransformer 调用）==========
    public static NBTTagList saveEnchantmentsBeforeRebuildForArmor(NBTTagCompound rootNBT) {
        if (rootNBT == null) return new NBTTagList();

        boolean forcePreserve = hasForcePreserveMark(rootNBT);
        if (!ConfigHandler.preventEnchantmentDeletion && !forcePreserve) return new NBTTagList();

        if (rootNBT.hasKey("ench", Constants.NBT.TAG_LIST)) {
            NBTTagList saved = rootNBT.getTagList("ench", Constants.NBT.TAG_COMPOUND).copy();
            if (saved.tagCount() > 0) {
                System.out.println("[CrashGuard] Armor: Saved " + saved.tagCount() + " enchantments (force=" + forcePreserve + ")");
            }
            return saved;
        }
        return new NBTTagList();
    }

    public static void restoreEnchantmentsAfterRebuildForArmor(NBTTagCompound rootNBT, NBTTagList savedEnch) {
        if (rootNBT == null) return;
        if (savedEnch == null || savedEnch.tagCount() == 0) return;

        boolean forcePreserve = hasForcePreserveMark(rootNBT);
        if (!ConfigHandler.preventEnchantmentDeletion && !forcePreserve) return;

        rootNBT.setTag("ench", savedEnch);
        System.out.println("[CrashGuard] Armor: Restored " + savedEnch.tagCount() + " enchantments (force=" + forcePreserve + ")");
    }
}