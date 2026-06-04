package com.crashguard.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import slimeknights.tconstruct.library.tools.ranged.BowCore;

import java.lang.reflect.Method;

public class TestShootCommand extends CommandBase {

    @Override
    public String getName() {
        return "testshoot";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/testshoot";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("只有玩家可以使用此命令"));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack bow = player.getHeldItemMainhand();

        if (bow.isEmpty()) {
            sender.sendMessage(new TextComponentString("请手持弓"));
            return;
        }

        if (!(bow.getItem() instanceof BowCore)) {
            sender.sendMessage(new TextComponentString("这不是匠魂弓，请手持匠魂弓"));
            return;
        }

        sender.sendMessage(new TextComponentString("========== 开始调试 =========="));

        try {
            Class<?> clazz = bow.getItem().getClass();
            sender.sendMessage(new TextComponentString("当前类: " + clazz.getName()));

            // 打印当前类的 onItemRightClick 方法签名
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals("onItemRightClick")) {
                    sender.sendMessage(new TextComponentString("找到 onItemRightClick: " + m.toGenericString()));
                }
            }

            // 打印父类的 onItemRightClick 方法签名
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                sender.sendMessage(new TextComponentString("父类: " + superClass.getName()));
                Method[] superMethods = superClass.getDeclaredMethods();
                for (Method m : superMethods) {
                    if (m.getName().equals("onItemRightClick")) {
                        sender.sendMessage(new TextComponentString("父类找到 onItemRightClick: " + m.toGenericString()));
                    }
                }
            }

        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("错误: " + e.getMessage()));
        }

        sender.sendMessage(new TextComponentString("========== 调试结束 =========="));
    }
}