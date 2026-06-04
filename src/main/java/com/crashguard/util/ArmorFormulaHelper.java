package com.crashguard.util;

import com.crashguard.config.ConfigHandler;
import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ArmorFormulaHelper {

    private static ScriptEngine engine;

    static {
        try {
            engine = new ScriptEngineManager().getEngineByName("JavaScript");
        } catch (Exception e) {
            engine = null;
        }
    }

    public static float calculateFinalDamage(float damage, float minDamage) {
        String formula = ConfigHandler.getDamageReductionFormula();

        if (StringUtils.isNotBlank(formula) && engine != null) {
            try {
                String expr = formula
                        .replace("damage", Float.toString(damage))
                        .replace("min", Float.toString(minDamage));
                Object result = engine.eval(expr);
                if (result instanceof Number) {
                    return Math.max(0, ((Number) result).floatValue());
                }
            } catch (ScriptException e) {}
        }

        // 模式2：cap=1→无保底, cap=0.8→原版20%, cap=0→保底100%
        float cap = ConfigHandler.getDamageReductionCap();
        float ratio = 1 - cap;  // cap=1→0, cap=0.8→0.2, cap=0→1

        return Math.max(damage, minDamage * ratio);
    }
}