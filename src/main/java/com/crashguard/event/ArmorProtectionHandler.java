package com.crashguard.event;

import com.crashguard.config.ConfigHandler;
import com.crashguard.util.ArmorReductionHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ArmorProtectionHandler {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        boolean shouldReduce;
        if (entity instanceof EntityPlayer) {
            shouldReduce = ConfigHandler.shouldPlayerIgnoreUnblockable();
        } else {
            shouldReduce = ConfigHandler.shouldEntityIgnoreUnblockable();
        }

        if (!shouldReduce) {
            return;
        }

        float damage = event.getAmount();
        float reducedDamage = ArmorReductionHelper.calculateArmorReduction(entity, damage);
        event.setAmount(reducedDamage);
    }
}