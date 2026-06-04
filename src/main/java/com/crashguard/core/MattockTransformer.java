package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class MattockTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("slimeknights.tconstruct.tools.tools.Mattock")) {
            System.out.println("[CrashGuard] 正在修改 Mattock...");
            return transformMattock(basicClass);
        }

        return basicClass;
    }

    private byte[] transformMattock(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // 修改 getToolClasses 方法
                if (name.equals("getToolClasses") && desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/util/Set;")) {
                    System.out.println("[CrashGuard] 修改 Mattock.getToolClasses");
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            // 返回包含 "axe", "shovel", "hoe" 的 Set
                            mv.visitInsn(Opcodes.ICONST_3);
                            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitInsn(Opcodes.ICONST_0);
                            mv.visitLdcInsn("axe");
                            mv.visitInsn(Opcodes.AASTORE);
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitInsn(Opcodes.ICONST_1);
                            mv.visitLdcInsn("shovel");
                            mv.visitInsn(Opcodes.AASTORE);
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitInsn(Opcodes.ICONST_2);
                            mv.visitLdcInsn("hoe");
                            mv.visitInsn(Opcodes.AASTORE);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "com/google/common/collect/ImmutableSet",
                                    "copyOf",
                                    "([Ljava/lang/Object;)Lcom/google/common/collect/ImmutableSet;",
                                    false);
                            mv.visitInsn(Opcodes.ARETURN);
                        }
                    };
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }
}