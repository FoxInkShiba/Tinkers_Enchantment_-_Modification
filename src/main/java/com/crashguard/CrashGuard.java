package com.crashguard;

import com.crashguard.block.AdvancedEnchantingTableBlock;
import com.crashguard.block.EnchantingTableBlock;
import com.crashguard.command.TestCommand;
import com.crashguard.config.ConfigHandler;
import com.crashguard.container.ContainerAdvancedEnchantingTable;
import com.crashguard.container.ContainerEnchantingTable;
import com.crashguard.gui.GuiAdvancedEnchantingTable;
import com.crashguard.gui.GuiEnchantingTable;
import com.crashguard.tileentity.TileEntityAdvancedEnchantingTable;
import com.crashguard.tileentity.TileEntityEnchantingTable;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = CrashGuard.MODID,
        name = "Tinkers' Enchantment and Modification",
        version = "1.1",
        dependencies = "required-after:tconstruct")
public class CrashGuard implements IGuiHandler {

    public static final String MODID = "crashguard";
    public static CrashGuard instance;

    // GUI IDs
    public static final int GUI_ENCHANTING_TABLE = 0;
    public static final int GUI_ADVANCED_ENCHANTING_TABLE = 1;

    // 方块
    public static Block enchantingTableBlock;
    public static Block advancedEnchantingTableBlock;

    public CrashGuard() {
        instance = this;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigHandler.init(event.getSuggestedConfigurationFile());

        // 注册 GUI 处理器
        NetworkRegistry.INSTANCE.registerGuiHandler(this, this);

        System.out.println("[CrashGuard] 初始化完成");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // 普通附魔台配方
        ResourceLocation recipeId1 = new ResourceLocation(MODID, "enchanting_table");
        GameRegistry.addShapedRecipe(recipeId1, recipeId1, new ItemStack(enchantingTableBlock),
                "LDL", "FBF", "FWF",
                'L', Blocks.LAPIS_BLOCK,
                'D', Blocks.DIAMOND_BLOCK,
                'F', Blocks.IRON_BLOCK,
                'B', Blocks.OBSIDIAN,
                'W', Blocks.CRAFTING_TABLE);

        // 高级附魔台配方
        ResourceLocation recipeId2 = new ResourceLocation(MODID, "advanced_enchanting_table");
        GameRegistry.addShapedRecipe(recipeId2, recipeId2, new ItemStack(advancedEnchantingTableBlock),
                "OSO", "ERE", "MTM",
                'O', Blocks.OBSIDIAN,
                'S', Items.NETHER_STAR,
                'E', Blocks.EMERALD_BLOCK,
                'R', Blocks.REDSTONE_BLOCK,
                'M', new ItemStack(Item.getByNameOrId("tconstruct:cast")),
                'T', new ItemStack(enchantingTableBlock));

        System.out.println("[CrashGuard] 匠魂附魔与修改已加载");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TestCommand());
        System.out.println("[CrashGuard] 命令已注册");
    }

    // ========== 注册方块 ==========
    @Mod.EventBusSubscriber(modid = MODID)
    public static class BlockRegistry {
        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            // 普通附魔台
            enchantingTableBlock = new EnchantingTableBlock();
            event.getRegistry().register(enchantingTableBlock);
            GameRegistry.registerTileEntity(TileEntityEnchantingTable.class,
                    new ResourceLocation(MODID, "enchanting_table"));

            // 高级附魔台
            advancedEnchantingTableBlock = new AdvancedEnchantingTableBlock();
            event.getRegistry().register(advancedEnchantingTableBlock);
            GameRegistry.registerTileEntity(TileEntityAdvancedEnchantingTable.class,
                    new ResourceLocation(MODID, "advanced_enchanting_table"));
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            // 普通附魔台物品
            ItemBlock enchantingTableItem = new ItemBlock(enchantingTableBlock);
            enchantingTableItem.setRegistryName(enchantingTableBlock.getRegistryName());
            event.getRegistry().register(enchantingTableItem);

            // 高级附魔台物品
            ItemBlock advancedEnchantingTableItem = new ItemBlock(advancedEnchantingTableBlock);
            advancedEnchantingTableItem.setRegistryName(advancedEnchantingTableBlock.getRegistryName());
            event.getRegistry().register(advancedEnchantingTableItem);
        }
    }

    // ========== 注册模型 ==========
    @Mod.EventBusSubscriber(modid = MODID)
    public static class ModelRegistry {
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            // 普通附魔台
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(enchantingTableBlock), 0,
                    new ModelResourceLocation(enchantingTableBlock.getRegistryName(), "inventory"));

            // 高级附魔台
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(advancedEnchantingTableBlock), 0,
                    new ModelResourceLocation(advancedEnchantingTableBlock.getRegistryName(), "inventory"));
        }
    }

    // ========== GUI 处理 ==========
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        if (ID == GUI_ENCHANTING_TABLE) {
            if (tile instanceof TileEntityEnchantingTable) {
                return new ContainerEnchantingTable(player.inventory, (TileEntityEnchantingTable) tile);
            }
        } else if (ID == GUI_ADVANCED_ENCHANTING_TABLE) {
            if (tile instanceof TileEntityAdvancedEnchantingTable) {
                return new ContainerAdvancedEnchantingTable(player.inventory, (TileEntityAdvancedEnchantingTable) tile);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        if (ID == GUI_ENCHANTING_TABLE) {
            if (tile instanceof TileEntityEnchantingTable) {
                return new GuiEnchantingTable(
                        new ContainerEnchantingTable(player.inventory, (TileEntityEnchantingTable) tile),
                        (TileEntityEnchantingTable) tile);
            }
        } else if (ID == GUI_ADVANCED_ENCHANTING_TABLE) {
            if (tile instanceof TileEntityAdvancedEnchantingTable) {
                return new GuiAdvancedEnchantingTable(
                        new ContainerAdvancedEnchantingTable(player.inventory, (TileEntityAdvancedEnchantingTable) tile),
                        (TileEntityAdvancedEnchantingTable) tile);
            }
        }
        return null;
    }
}