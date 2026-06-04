package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class ItemBowTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("net.minecraft.item.ItemBow")) {
            System.out.println("[CrashGuard] 修改 ItemBow 阈值");
            return transformItemBow(basicClass);
        }

        return basicClass;
    }

    private byte[] transformItemBow(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals("onPlayerStoppedUsing") &&
                        desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;I)V")) {
                    System.out.println("[CrashGuard] 修改原版弓阈值 3 -> 0");
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