package com.crashguard.core;

import com.crashguard.util.ArmorReductionHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.ISpecialArmor;

public class VanillaArmorHandler implements ISpecialArmor {

    public static final VanillaArmorHandler INSTANCE = new VanillaArmorHandler();

    @Override
    public ArmorProperties getProperties(EntityLivingBase entity, ItemStack armor, DamageSource source, double damage, int slot) {
        // 所有伤害已由 LivingHurtEvent 处理，这里直接返回 0 避免干扰
        return new ArmorProperties(0, 0, 0);
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        if (armor.isEmpty()) return 0;
        if (armor.getItem() instanceof ItemArmor) {
            return ((ItemArmor) armor.getItem()).damageReduceAmount;
        }
        return 0;
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {
        stack.damageItem(damage, entity);
    }
}