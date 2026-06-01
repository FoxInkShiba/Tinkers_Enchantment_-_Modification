package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

public class TinkerArmorTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("c4.conarm.common.events.ArmorEvents")) {
            System.out.println("[CrashGuard] 正在修改 ArmorEvents...");
            return transformArmorEvents(basicClass);
        }

        if (transformedName.equals("c4.conarm.common.armor.utils.ArmorHelper")) {
            System.out.println("[CrashGuard] 正在修改 ArmorHelper...");
            return transformArmorHelper(basicClass);
        }

        if (transformedName.equals("c4.conarm.common.armor.modifiers.ModResistantType")) {
            System.out.println("[CrashGuard] 正在修改 ModResistantType...");
            return transformModResistantType(basicClass);
        }

        return basicClass;
    }

    private byte[] transformArmorEvents(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if ((name.equals("playerHurt") || name.equals("playerDamaged"))
                        && (desc.equals("(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V")
                        || desc.equals("(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V"))) {
                    System.out.println("[CrashGuard] 修改 " + name + " 方法");
                    return new DamageEventVisitor(mv);
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }

    private static class DamageEventVisitor extends MethodVisitor {
        public DamageEventVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            // 插入重置标记的调用
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/ArmorReductionHelper",
                    "resetProcessed",
                    "()V", false);
            super.visitCode();
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (name.equals("max") && owner.equals("java/lang/Math") && desc.equals("(FF)F")) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/ArmorFormulaHelper",
                        "calculateFinalDamage",
                        "(FF)F", false);
                return;
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private byte[] transformArmorHelper(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("getPropertiesAfterAbsorb") &&
                        desc.equals("(Lnet/minecraft/item/ItemStack;DFFLnet/minecraft/inventory/EntityEquipmentSlot;)Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;")) {
                    System.out.println("[CrashGuard] 修改 getPropertiesAfterAbsorb 方法");
                    return new GetPropertiesAdvice(access, name, desc, mv);
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }

    private static class GetPropertiesAdvice extends AdviceAdapter {

        protected GetPropertiesAdvice(int access, String name, String desc, MethodVisitor mv) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode == Opcodes.ARETURN) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.DLOAD, 1);
                mv.visitVarInsn(Opcodes.FLOAD, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 5);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/ArmorReductionHelper",
                        "modifyArmorProperties",
                        "(Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;Lnet/minecraft/item/ItemStack;DFLnet/minecraft/inventory/EntityEquipmentSlot;)Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;",
                        false);
            }
        }
    }

    private byte[] transformModResistantType(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("<init>")) {
                    System.out.println("[CrashGuard] 修改 ModResistantType 构造函数");
                    return new ConstructorVisitor(mv);
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }

    private static class ConstructorVisitor extends MethodVisitor {
        public ConstructorVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.BIPUSH && operand == 8) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/config/ConfigHandler",
                        "getResistantMaxLevel",
                        "()I", false);
                return;
            }
            super.visitIntInsn(opcode, operand);
        }
    }
}