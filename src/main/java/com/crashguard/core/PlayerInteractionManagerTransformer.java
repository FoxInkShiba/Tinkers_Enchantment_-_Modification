package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class PlayerInteractionManagerTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("net.minecraft.server.management.PlayerInteractionManager")) {
            System.out.println("[CrashGuard] 修改 PlayerInteractionManager");
            return transformPlayerInteractionManager(basicClass);
        }

        return basicClass;
    }

    private byte[] transformPlayerInteractionManager(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // 处理 processRightClickBlock 方法（这是关键！）
                if (name.equals("processRightClickBlock") ||
                        name.equals("func_187252_a") ||  // 混淆名
                        name.contains("processRightClick")) {
                    System.out.println("[CrashGuard] 修改 " + name);
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            if (opcode == Opcodes.BIPUSH && operand == 3) {
                                System.out.println("[CrashGuard] PlayerInteractionManager: 阈值 3 -> 0");
                                super.visitIntInsn(Opcodes.ICONST_0, 0);
                                return;
                            }
                            super.visitIntInsn(opcode, operand);
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == Opcodes.ICONST_3) {
                                System.out.println("[CrashGuard] PlayerInteractionManager: ICONST_3 -> ICONST_0");
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