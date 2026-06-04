package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class BowToolClassTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("slimeknights.tconstruct.tools.ranged.item.ShortBow") ||
                transformedName.equals("slimeknights.tconstruct.tools.ranged.item.LongBow") ||
                transformedName.equals("slimeknights.tconstruct.tools.ranged.item.CrossBow")) {
            System.out.println("[CrashGuard] 给 " + transformedName + " 添加 bow 标签");
            return addBowTag(basicClass);
        }

        return basicClass;
    }

    private byte[] addBowTag(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                // 修改 getToolClasses 方法
                if (name.equals("getToolClasses") && desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/util/Set;")) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            // 调用父类方法获取 Set
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitVarInsn(Opcodes.ALOAD, 1);
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                    "slimeknights/tconstruct/library/tools/ToolCore",
                                    "getToolClasses",
                                    "(Lnet/minecraft/item/ItemStack;)Ljava/util/Set;", false);

                            // 添加 "bow" 到 Set 中
                            mv.visitLdcInsn("bow");
                            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);

                            // 弹出返回值（add 返回的 boolean，不需要）
                            mv.visitInsn(Opcodes.POP);

                            // 返回 Set
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