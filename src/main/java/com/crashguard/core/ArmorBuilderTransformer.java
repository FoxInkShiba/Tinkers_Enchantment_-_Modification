package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class ArmorBuilderTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("c4.conarm.lib.tinkering.ArmorBuilder")) {
            System.out.println("[CrashGuard] Transforming ArmorBuilder");
            return transformArmorBuilder(basicClass);
        }

        return basicClass;
    }

    private byte[] transformArmorBuilder(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("rebuildArmor")) {
                    System.out.println("[CrashGuard] Transforming rebuildArmor");
                    return new RebuildArmorMethodVisitor(mv, access, desc);
                }
                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    private static class RebuildArmorMethodVisitor extends LocalVariablesSorter {

        private int savedEnchVarIndex = -1;

        protected RebuildArmorMethodVisitor(MethodVisitor mv, int access, String desc) {
            super(Opcodes.ASM5, access, desc, mv);
        }

        @Override
        public void visitCode() {
            // 保存附魔（配置检查在方法内部）
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "saveEnchantmentsBeforeRebuildForArmor",
                    "(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagList;",
                    false);

            savedEnchVarIndex = newLocal(Type.getType("Lnet/minecraft/nbt/NBTTagList;"));
            mv.visitVarInsn(Opcodes.ASTORE, savedEnchVarIndex);

            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                if (savedEnchVarIndex != -1) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitVarInsn(Opcodes.ALOAD, savedEnchVarIndex);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "com/crashguard/util/CompatHandler",
                            "restoreEnchantmentsAfterRebuildForArmor",
                            "(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/nbt/NBTTagList;)V",
                            false);
                }
            }
            super.visitInsn(opcode);
        }
    }
}