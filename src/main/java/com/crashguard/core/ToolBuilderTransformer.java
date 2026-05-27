package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class ToolBuilderTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("slimeknights.tconstruct.library.utils.ToolBuilder")) {
            System.out.println("[CrashGuard] Transforming ToolBuilder");
            return transformToolBuilder(basicClass);
        }

        return basicClass;
    }

    private byte[] transformToolBuilder(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("rebuildTool")) {
                    System.out.println("[CrashGuard] Transforming rebuildTool");
                    return new RebuildToolMethodVisitor(mv, access, desc);
                }
                return mv;
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    /**
     * 使用 LocalVariablesSorter 来安全地添加新的局部变量
     */
    private static class RebuildToolMethodVisitor extends LocalVariablesSorter {

        private int savedEnchVarIndex = -1;

        protected RebuildToolMethodVisitor(MethodVisitor mv, int access, String desc) {
            super(Opcodes.ASM5, access, desc, mv);
        }

        @Override
        public void visitCode() {
            // 在方法开头，调用保存方法并将结果存储到新的局部变量
            mv.visitVarInsn(Opcodes.ALOAD, 0);  // rootNBT
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/crashguard/util/CompatHandler",
                    "saveEnchantmentsBeforeRebuild",
                    "(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagList;",
                    false);

            // 申请一个新的局部变量位置来存储返回的 NBTTagList
            savedEnchVarIndex = newLocal(Type.getType("Lnet/minecraft/nbt/NBTTagList;"));
            mv.visitVarInsn(Opcodes.ASTORE, savedEnchVarIndex);

            super.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                // 在返回前恢复附魔
                if (savedEnchVarIndex != -1) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);  // rootNBT
                    mv.visitVarInsn(Opcodes.ALOAD, savedEnchVarIndex);  // 保存的附魔
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "com/crashguard/util/CompatHandler",
                            "restoreEnchantmentsAfterRebuild",
                            "(Lnet/minecraft/nbt/NBTTagCompound;Lnet/minecraft/nbt/NBTTagList;)V",
                            false);
                }
            }
            super.visitInsn(opcode);
        }
    }
}