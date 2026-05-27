package com.crashguard.command;

import com.crashguard.util.CompatHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import slimeknights.tconstruct.library.tools.ToolCore;

public class TestCommand extends CommandBase {

    @Override
    public String getName() { return "enchtest"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/enchtest <enchant_id> <level>"; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("Player only"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack held = player.getHeldItemMainhand();

        if (!(held.getItem() instanceof ToolCore)) {
            sender.sendMessage(new TextComponentString("Hold a Tinker tool"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(new TextComponentString("Usage: /enchtest <enchant_id> <level>"));
            return;
        }

        Enchantment ench = Enchantment.REGISTRY.getObject(new ResourceLocation(args[0]));
        if (ench == null) {
            sender.sendMessage(new TextComponentString("Enchantment not found: " + args[0]));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString("Level must be a number"));
            return;
        }

        if (CompatHandler.addEnchantment(held, ench, level)) {
            sender.sendMessage(new TextComponentString("Added " + ench.getTranslatedName(level) + " to your tool"));
        } else {
            sender.sendMessage(new TextComponentString("Failed to add enchantment"));
        }
    }
}