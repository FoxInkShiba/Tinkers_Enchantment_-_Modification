package com.crashguard.modifier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import slimeknights.tconstruct.library.modifiers.Modifier;

public class EnchantmentPreserverModifier extends Modifier {

    public static final String IDENTIFIER = "crashguard_enchantment_preserver";
    private static final String TAG_PRESERVE_ENCHANT = "crashguard_preserve_enchant";

    public EnchantmentPreserverModifier() {
        super(IDENTIFIER);
    }

    @Override
    public void applyEffect(NBTTagCompound rootCompound, NBTTagCompound modifierTag) {
        rootCompound.setBoolean(TAG_PRESERVE_ENCHANT, true);
    }

    public static void addPreserveMark(ItemStack stack) {
        if (stack.isEmpty()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setBoolean(TAG_PRESERVE_ENCHANT, true);
    }

    public static boolean shouldPreserveEnchantment(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) return false;
        return tag.getBoolean(TAG_PRESERVE_ENCHANT);
    }

    public static boolean shouldPreserveEnchantment(NBTTagCompound rootTag) {
        if (rootTag == null) return false;
        return rootTag.getBoolean(TAG_PRESERVE_ENCHANT);
    }
}