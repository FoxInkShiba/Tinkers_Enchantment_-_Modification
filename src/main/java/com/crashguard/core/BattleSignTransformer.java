package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class BattleSignTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        // 模糊匹配，只要类名包含 BattleSign 就处理
        if (transformedName != null && transformedName.contains("BattleSign")) {
            System.out.println("[CrashGuard] 正在修改 BattleSign: " + transformedName);
            return transformBattleSign(basicClass);
        }

        return basicClass;
    }

    private byte[] transformBattleSign(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                // 正常复制所有现有方法
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                // 添加 isShield 方法
                MethodVisitor newMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "isShield",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Z",
                        null, null);
                newMv.visitCode();
                newMv.visitInsn(Opcodes.ICONST_1);
                newMv.visitInsn(Opcodes.IRETURN);
                newMv.visitMaxs(1, 2);
                newMv.visitEnd();

                super.visitEnd();
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }
}