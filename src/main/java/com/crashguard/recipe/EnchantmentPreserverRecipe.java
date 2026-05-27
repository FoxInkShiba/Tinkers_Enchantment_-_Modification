package com.crashguard.recipe;

import com.crashguard.modifier.EnchantmentPreserverModifier;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;
import slimeknights.tconstruct.library.tools.ToolCore;

public class EnchantmentPreserverRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private final ItemStack upgradeItem;

    public EnchantmentPreserverRecipe(ResourceLocation id, ItemStack upgradeItem) {
        this.setRegistryName(id);
        this.upgradeItem = upgradeItem;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        ItemStack tool = ItemStack.EMPTY;
        ItemStack upgrade = ItemStack.EMPTY;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (isTinkerItem(stack)) {
                if (!tool.isEmpty()) return false;
                tool = stack;
            } else if (isUpgradeItem(stack)) {
                if (!upgrade.isEmpty()) return false;
                upgrade = stack;
            } else {
                return false;
            }
        }

        return !tool.isEmpty() && !upgrade.isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack tool = ItemStack.EMPTY;
        ItemStack upgrade = ItemStack.EMPTY;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (isTinkerItem(stack)) {
                tool = stack;
            } else if (isUpgradeItem(stack)) {
                upgrade = stack;
            }
        }

        if (tool.isEmpty() || upgrade.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = tool.copy();
        EnchantmentPreserverModifier.addPreserveMark(result);
        return result;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        return remaining;
    }

    private boolean isTinkerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && (tag.hasKey("InfiTool") || tag.hasKey("TinkerData"))) {
            return true;
        }
        // 盔甲判断
        if (tag != null && tag.hasKey("ArmorData")) {
            return true;
        }
        return stack.getItem() instanceof ToolCore;
    }

    private boolean isUpgradeItem(ItemStack stack) {
        if (upgradeItem.isEmpty()) return false;
        return ItemStack.areItemsEqual(stack, upgradeItem);
    }
}