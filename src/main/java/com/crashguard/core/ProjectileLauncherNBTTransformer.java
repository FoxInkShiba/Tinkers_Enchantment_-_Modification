package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class ProjectileLauncherNBTTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("slimeknights.tconstruct.library.tools.ProjectileLauncherNBT")) {
            System.out.println("[CrashGuard] 修改 ProjectileLauncherNBT");
            return transformNBT(basicClass);
        }

        return basicClass;
    }

    private byte[] transformNBT(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("from") && desc.equals("(Lnet/minecraft/item/ItemStack;)Lslimeknights/tconstruct/library/tools/ProjectileLauncherNBT;")) {
                    System.out.println("[CrashGuard] 修改 from 方法");
                    return new FromMethodVisitor(mv);
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }

    private static class FromMethodVisitor extends MethodVisitor {

        public FromMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.ARETURN) {
                System.out.println("[CrashGuard] 注入 drawSpeed 修改");

                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "com/crashguard/util/CompatHandler",
                        "modifyDrawSpeed",
                        "(Lslimeknights/tconstruct/library/tools/ProjectileLauncherNBT;Lnet/minecraft/item/ItemStack;)V",
                        false);
            }
            super.visitInsn(opcode);
        }
    }
}