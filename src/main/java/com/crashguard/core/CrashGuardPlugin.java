package com.crashguard.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("CrashGuardCore")
@IFMLLoadingPlugin.SortingIndex(1001)
public class CrashGuardPlugin implements IFMLLoadingPlugin {

    public CrashGuardPlugin() {
        System.out.println("[CrashGuard] CoreMod loaded");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
                "com.crashguard.core.TinkersEvolutionTransformer",
                "com.crashguard.core.CrashGuardTransformer",
                "com.crashguard.core.ToolBuilderTransformer",
                "com.crashguard.core.ArmorBuilderTransformer",
                "com.crashguard.core.BattleSignTransformer",
                "com.crashguard.core.TinkerArmorTransformer",
                "com.crashguard.core.VanillaArmorTransformer" ,
                "com.crashguard.core.MattockTransformer",
                "com.crashguard.core.BowCoreTransformer",
                "com.crashguard.core.ItemBowTransformer",
                "com.crashguard.core.EntityPlayerTransformer",
                "com.crashguard.core.BowToolClassTransformer",
              //  "com.crashguard.core.CrossBowTransformer",
                "com.crashguard.core.ProjectileLauncherNBTTransformer",
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    @Nullable
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        Mixins.addConfiguration("mixins.crashguard.json");
        System.out.println("[CrashGuard] Mixin configuration registered");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}