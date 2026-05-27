package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

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
                    return new UnifiedAttackEntityInjector(mv);
                }

                if (name.equals("calcCutoffDamage") && desc.equals("(FF)F")) {
                    System.out.println("[CrashGuard] Transforming ToolHelper.calcCutoffDamage");
                    return new CalcCutoffDamageVisitor(mv);
                }

                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    private static class UnifiedAttackEntityInjector extends MethodVisitor {
        public UnifiedAttackEntityInjector(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);

            if (name.equals("calcCutoffDamage") && owner.equals("slimeknights/tconstruct/library/utils/ToolHelper")) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 4);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/CompatHandler",
                        "getUnifiedEnchantmentDamage",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)F",
                        false);
                mv.visitInsn(Opcodes.FADD);
            }
        }
    }

    private static class CalcCutoffDamageVisitor extends MethodVisitor {
        public CalcCutoffDamageVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/config/ConfigHandler",
                    "getDamageDecayMultiplier",
                    "()F", false);
            mv.visitVarInsn(Opcodes.FSTORE, 2);

            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/config/ConfigHandler",
                    "getDamageDecayCap",
                    "()F", false);
            mv.visitVarInsn(Opcodes.FSTORE, 3);

            mv.visitInsn(Opcodes.FCONST_1);
            mv.visitVarInsn(Opcodes.FSTORE, 4);

            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FSTORE, 5);

            mv.visitInsn(Opcodes.FCONST_0);
            mv.visitVarInsn(Opcodes.FSTORE, 0);

            Label loopStart = new Label();
            Label loopEnd = new Label();

            mv.visitLabel(loopStart);
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FCMPG);
            mv.visitJumpInsn(Opcodes.IFLE, loopEnd);

            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitVarInsn(Opcodes.FSTORE, 0);

            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitLdcInsn(0.001f);
            mv.visitInsn(Opcodes.FCMPG);
            Label pSmall = new Label();
            mv.visitJumpInsn(Opcodes.IFLE, pSmall);

            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 2);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitVarInsn(Opcodes.FSTORE, 4);

            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitInsn(Opcodes.FCMPG);
            Label pNotCap = new Label();
            mv.visitJumpInsn(Opcodes.IFLE, pNotCap);
            mv.visitVarInsn(Opcodes.FLOAD, 3);
            mv.visitVarInsn(Opcodes.FSTORE, 4);
            mv.visitLabel(pNotCap);

            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FSUB);
            mv.visitVarInsn(Opcodes.FSTORE, 5);

            mv.visitJumpInsn(Opcodes.GOTO, loopStart);

            mv.visitLabel(pSmall);
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitInsn(Opcodes.FDIV);
            mv.visitInsn(Opcodes.FCONST_1);
            mv.visitInsn(Opcodes.FSUB);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitInsn(Opcodes.FRETURN);

            mv.visitLabel(loopEnd);
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 4);
            mv.visitVarInsn(Opcodes.FLOAD, 5);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FADD);
            mv.visitInsn(Opcodes.FRETURN);
        }
    }
}