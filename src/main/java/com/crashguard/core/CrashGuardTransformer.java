package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class CrashGuardTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("net.minecraft.enchantment.Enchantment")) {
            return transformEnchantment(basicClass);
        }

        if (transformedName.equals("net.minecraft.enchantment.EnchantmentHelper")) {
            return transformEnchantmentHelper(basicClass);
        }

        if (transformedName.equals("slimeknights.tconstruct.library.tools.ToolCore")) {
            return transformToolCore(basicClass);
        }

        if (transformedName.equals("slimeknights.tconstruct.library.utils.ToolHelper")) {
            return transformToolHelper(basicClass);
        }

        return basicClass;
    }

    // ========== 1. 修改 Enchantment.canApply ==========
    private byte[] transformEnchantment(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("canApply") && desc.equals("(Lnet/minecraft/item/ItemStack;)Z")) {
                    return new CanApplyMethodVisitor(mv);
                }
                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    private static class CanApplyMethodVisitor extends AdviceAdapter {
        protected CanApplyMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv, Opcodes.ACC_PUBLIC, "canApply", "(Lnet/minecraft/item/ItemStack;)Z");
        }

        @Override
        protected void onMethodEnter() {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "isTinkerTool",
                    "(Lnet/minecraft/item/ItemStack;)Z",
                    false);
            Label notTinker = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, notTinker);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(notTinker);
        }
    }

    // ========== 2. 修改 EnchantmentHelper ==========
    private byte[] transformEnchantmentHelper(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("getEnchantmentLevel") && desc.equals("(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I")) {
                    return new GetEnchantmentLevelVisitor(mv);
                }

                if (name.equals("getEnchantments") && desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/util/Map;")) {
                    return new GetEnchantmentsVisitor(mv);
                }

                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    private static class GetEnchantmentLevelVisitor extends AdviceAdapter {
        protected GetEnchantmentLevelVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "getEnchantmentLevel", "(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I");
        }

        @Override
        protected void onMethodEnter() {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "isTinkerTool",
                    "(Lnet/minecraft/item/ItemStack;)Z",
                    false);
            Label notTinker = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, notTinker);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "getEnchantmentLevelFromTinkerTool",
                    "(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I",
                    false);
            mv.visitInsn(Opcodes.IRETURN);

            mv.visitLabel(notTinker);
        }
    }

    private static class GetEnchantmentsVisitor extends AdviceAdapter {
        protected GetEnchantmentsVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    "getEnchantments", "(Lnet/minecraft/item/ItemStack;)Ljava/util/Map;");
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ARETURN) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/CompatHandler",
                        "mergeTinkerEnchantments",
                        "(Ljava/util/Map;Lnet/minecraft/item/ItemStack;)V",
                        false);
            }
        }
    }

    // ========== 3. 修改 ToolCore ==========
    private byte[] transformToolCore(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("isEnchantable") && desc.equals("(Lnet/minecraft/item/ItemStack;)Z")) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            mv.visitInsn(Opcodes.ICONST_1);
                            mv.visitInsn(Opcodes.IRETURN);
                        }
                    };
                }

                if (name.equals("getItemEnchantability")) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            mv.visitIntInsn(Opcodes.SIPUSH, 15);
                            mv.visitInsn(Opcodes.IRETURN);
                        }
                    };
                }

                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    // ========== 4. 修改 ToolHelper ==========
    private byte[] transformToolHelper(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("attackEntity") && desc.contains("Lnet/minecraft/entity/EntityLivingBase;")) {
                    System.out.println("[CrashGuard] Transforming ToolHelper.attackEntity");
                    return new AttackEntityInjector(mv, access, desc);
                }

                if (name.equals("calcCutoffDamage") && desc.equals("(FF)F")) {
                    System.out.println("[CrashGuard] Transforming ToolHelper.calcCutoffDamage");
                    return new CalcCutoffDamageVisitor(mv);
                }

                if (name.equals("getActualDamage") && desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)F")) {
                    System.out.println("[CrashGuard] Transforming ToolHelper.getActualDamage");
                    return new GetActualDamageInjector(mv, access, desc);
                }

                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    private static class AttackEntityInjector extends LocalVariablesSorter {

        private int tempBaseVar = -1;
        private int tempCutoffVar = -1;

        protected AttackEntityInjector(MethodVisitor mv, int access, String desc) {
            super(Opcodes.ASM5, access, desc, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC
                    && name.equals("calcCutoffDamage")
                    && owner.equals("slimeknights/tconstruct/library/utils/ToolHelper")
                    && desc.equals("(FF)F")) {

                if (tempBaseVar == -1) {
                    tempBaseVar = newLocal(Type.FLOAT_TYPE);
                    tempCutoffVar = newLocal(Type.FLOAT_TYPE);
                }

                // 栈: [damage, cutoff]
                mv.visitInsn(Opcodes.DUP2);
                mv.visitVarInsn(Opcodes.FSTORE, tempCutoffVar);
                mv.visitVarInsn(Opcodes.FSTORE, tempBaseVar);
                mv.visitInsn(Opcodes.POP2);

                mv.visitVarInsn(Opcodes.ALOAD, 0);              // stack
                mv.visitVarInsn(Opcodes.ALOAD, 3);              // targetEntity
                mv.visitVarInsn(Opcodes.ALOAD, 4);              // projectileEntity
                mv.visitVarInsn(Opcodes.FLOAD, tempBaseVar);    // damage
                mv.visitVarInsn(Opcodes.FLOAD, tempCutoffVar);  // cutoff

                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/CompatHandler",
                        "getTotalDamageForDecay",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;FF)F",
                        false);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private static class GetActualDamageInjector extends LocalVariablesSorter {

        private int tempDamageVar = -1;
        private int tempCutoffVar = -1;

        protected GetActualDamageInjector(MethodVisitor mv, int access, String desc) {
            super(Opcodes.ASM5, access, desc, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC
                    && name.equals("calcCutoffDamage")
                    && owner.equals("slimeknights/tconstruct/library/utils/ToolHelper")
                    && desc.equals("(FF)F")) {

                if (tempDamageVar == -1) {
                    tempDamageVar = newLocal(Type.FLOAT_TYPE);
                    tempCutoffVar = newLocal(Type.FLOAT_TYPE);
                }

                // 栈: [damage, cutoff]
                mv.visitInsn(Opcodes.DUP2);
                mv.visitVarInsn(Opcodes.FSTORE, tempCutoffVar);
                mv.visitVarInsn(Opcodes.FSTORE, tempDamageVar);
                mv.visitInsn(Opcodes.POP2);

                mv.visitVarInsn(Opcodes.ALOAD, 0);              // stack
                mv.visitInsn(Opcodes.ACONST_NULL);              // target = null
                mv.visitInsn(Opcodes.ACONST_NULL);              // projectile = null
                mv.visitVarInsn(Opcodes.FLOAD, tempDamageVar);  // damage
                mv.visitVarInsn(Opcodes.FLOAD, tempCutoffVar);  // cutoff

                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/CompatHandler",
                        "getDisplayDamageWithDecay",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;FF)F",
                        false);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    // ========== 修改 CalcCutoffDamageVisitor - 使用调整后的衰减值 ==========
    private static class CalcCutoffDamageVisitor extends MethodVisitor {
        public CalcCutoffDamageVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            // 获取调整后的衰减乘数（如果匠魂进化开了禁用，则取 max(配置值, 1.0f)）
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "getAdjustedDecayMultiplier",
                    "()F", false);
            mv.visitVarInsn(Opcodes.FSTORE, 2);

            // 获取调整后的衰减上限（如果匠魂进化开了禁用，返回一个很大的值）
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "getAdjustedDecayCap",
                    "()F", false);
            mv.visitVarInsn(Opcodes.FSTORE, 3);

            // float p = 1.0F
            mv.visitInsn(Opcodes.FCONST_1);
            mv.visitVarInsn(Opcodes.FSTORE, 4);

            // float d = damage (var0)
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FSTORE, 5);

            // float total = 0.0F (复用 var0)
            mv.visitInsn(Opcodes.FCONST_0);
            mv.visitVarInsn(Opcodes.FSTORE, 0);

            Label loopStart = new Label();
            Label loopEnd = new Label();
            Label capReached = new Label();

            // while (d > cutoff)
            mv.visitLabel(loopStart);
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FCMPG);
            mv.visitJumpInsn(Opcodes.IFLE, loopEnd);

            // 检查 p 是否达到上限
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitInsn(Opcodes.FCMPG);
            mv.visitJumpInsn(Opcodes.IFGE, capReached);

            // 正常衰减
            // total += p * cutoff
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitVarInsn(Opcodes.FSTORE, 0);

            // p *= decayMultiplier
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 2);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitVarInsn(Opcodes.FSTORE, 4);

            // d -= cutoff
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FSUB);
            mv.visitVarInsn(Opcodes.FSTORE, 5);

            mv.visitJumpInsn(Opcodes.GOTO, loopStart);

            // p >= cap: 直接计算剩余部分
            mv.visitLabel(capReached);
            // 计算剩余步数
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FDIV);
            mv.visitInsn(Opcodes.F2I);
            mv.visitVarInsn(Opcodes.ISTORE, 6);

            // total += cap * cutoff * remainingSteps
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitVarInsn(Opcodes.ILOAD, 6);
            mv.visitInsn(Opcodes.I2F);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitVarInsn(Opcodes.FSTORE, 0);

            // 计算剩余不足一步的部分
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.ILOAD, 6);
            mv.visitInsn(Opcodes.I2F);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FSUB);
            mv.visitVarInsn(Opcodes.FSTORE, 7);

            // total += cap * remainder
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitVarInsn(Opcodes.FLOAD, 7);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitInsn(Opcodes.FRETURN);

            // 循环结束
            mv.visitLabel(loopEnd);
            // total += p * d
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitInsn(Opcodes.FRETURN);
        }
    }
}