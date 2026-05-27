package com.crashguard.mixin;

import com.crashguard.modifier.ModEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.tools.ToolCore;

import java.util.Map;

@Mixin(EnchantmentHelper.class)
public class MixinEnchantmentHelper {

    private static final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getEnchantmentLevel", at = @At("HEAD"), cancellable = true)
    private static void getEnchantmentLevel(Enchantment ench, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (isProcessing.get()) return;
        if (!stack.isEmpty() && stack.getItem() instanceof ToolCore) {
            isProcessing.set(true);
            try {
                int level = ModEnchantment.getLevelDirect(stack, ench);
                if (level > 0) {
                    cir.setReturnValue(level);
                }
            } finally {
                isProcessing.set(false);
            }
        }
    }

    @Inject(method = "getEnchantments", at = @At("RETURN"), cancellable = true)
    private static void getEnchantments(ItemStack stack, CallbackInfoReturnable<Map<Enchantment, Integer>> cir) {
        if (isProcessing.get()) return;
        if (!stack.isEmpty() && stack.getItem() instanceof ToolCore) {
            isProcessing.set(true);
            try {
                Map<Enchantment, Integer> result = cir.getReturnValue();
                Map<Enchantment, Integer> directEnchants = ModEnchantment.getAllEnchantmentsDirect(stack);
                if (!directEnchants.isEmpty()) {
                    for (Map.Entry<Enchantment, Integer> entry : directEnchants.entrySet()) {
                        result.put(entry.getKey(), Math.max(result.getOrDefault(entry.getKey(), 0), entry.getValue()));
                    }
                    cir.setReturnValue(result);
                }
            } finally {
                isProcessing.set(false);
            }
        }
    }
}