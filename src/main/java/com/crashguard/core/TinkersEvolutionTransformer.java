package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class TinkersEvolutionTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("xyz.phanta.tconevo.handler.DamageCutoffCoreHooks")) {
            System.out.println("[CrashGuard] 清空匠魂进化 shouldIgnoreCutoff 方法");
            return transformDamageCutoffHooks(basicClass);
        }

        return basicClass;
    }

    private byte[] transformDamageCutoffHooks(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("shouldIgnoreCutoff") && desc.equals("()Z")) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            // 清空，只返回 false
                            mv.visitInsn(Opcodes.ICONST_0);
                            mv.visitInsn(Opcodes.IRETURN);
                        }

                        @Override
                        public void visitInsn(int opcode) {}
                        @Override
                        public void visitVarInsn(int opcode, int var) {}
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {}
                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String desc) {}
                        @Override
                        public void visitJumpInsn(int opcode, Label label) {}
                        @Override
                        public void visitLabel(Label label) {}
                        @Override
                        public void visitLdcInsn(Object cst) {}
                    };
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }
}