package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class EntityPlayerTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("net.minecraft.client.entity.EntityPlayerSP")) {
            System.out.println("[CrashGuard] 修改 EntityPlayerSP 阈值 3 -> 0");
            return transformEntityPlayerSP(basicClass);
        }

        return basicClass;
    }

    private byte[] transformEntityPlayerSP(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("onUpdate") && desc.equals("()V")) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            if (opcode == Opcodes.BIPUSH && operand == 3) {
                                super.visitIntInsn(Opcodes.ICONST_0, 0);
                                return;
                            }
                            super.visitIntInsn(opcode, operand);
                        }
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.ICONST_3) {
                                super.visitInsn(Opcodes.ICONST_0);
                                return;
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }
}