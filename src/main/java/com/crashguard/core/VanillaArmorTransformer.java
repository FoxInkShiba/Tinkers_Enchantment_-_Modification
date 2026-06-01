package com.crashguard.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class VanillaArmorTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (transformedName.equals("net.minecraft.item.ItemArmor")) {
            System.out.println("[CrashGuard] 正在修改 ItemArmor 实现 ISpecialArmor...");
            return transformItemArmor(basicClass);
        }

        if (transformedName.equals("net.minecraft.entity.EntityLivingBase")) {
            System.out.println("[CrashGuard] 正在修改 EntityLivingBase.getTotalArmorValue...");
            return transformEntityLivingBase(basicClass);
        }

        return basicClass;
    }

    // ========== 1. 修改 ItemArmor，实现 ISpecialArmor ==========
    private byte[] transformItemArmor(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                String[] newInterfaces;
                if (interfaces == null) {
                    newInterfaces = new String[]{"net/minecraftforge/common/ISpecialArmor"};
                } else {
                    newInterfaces = new String[interfaces.length + 1];
                    System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
                    newInterfaces[interfaces.length] = "net/minecraftforge/common/ISpecialArmor";
                }
                super.visit(version, access, name, signature, superName, newInterfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (name.equals("getProperties")) return null;
                if (name.equals("getArmorDisplay")) return null;
                if (name.equals("damageArmor")) return null;
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getProperties",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;DI)Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;",
                        null, null);
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC,
                        "com/crashguard/core/VanillaArmorHandler",
                        "INSTANCE",
                        "Lcom/crashguard/core/VanillaArmorHandler;");
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitVarInsn(Opcodes.DLOAD, 4);
                mv.visitVarInsn(Opcodes.ILOAD, 6);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "com/crashguard/core/VanillaArmorHandler",
                        "getProperties",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;DI)Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;",
                        false);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(7, 7);
                mv.visitEnd();

                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getArmorDisplay",
                        "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;I)I",
                        null, null);
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC,
                        "com/crashguard/core/VanillaArmorHandler",
                        "INSTANCE",
                        "Lcom/crashguard/core/VanillaArmorHandler;");
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ILOAD, 3);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "com/crashguard/core/VanillaArmorHandler",
                        "getArmorDisplay",
                        "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;I)I",
                        false);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(4, 4);
                mv.visitEnd();

                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "damageArmor",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;II)V",
                        null, null);
                mv.visitCode();
                mv.visitFieldInsn(Opcodes.GETSTATIC,
                        "com/crashguard/core/VanillaArmorHandler",
                        "INSTANCE",
                        "Lcom/crashguard/core/VanillaArmorHandler;");
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitVarInsn(Opcodes.ILOAD, 4);
                mv.visitVarInsn(Opcodes.ILOAD, 5);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "com/crashguard/core/VanillaArmorHandler",
                        "damageArmor",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;II)V",
                        false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(6, 6);
                mv.visitEnd();

                super.visitEnd();
            }
        }, ClassReader.EXPAND_FRAMES);

        return cw.toByteArray();
    }

    // ========== 2. 只修改 getTotalArmorValue ==========
    private byte[] transformEntityLivingBase(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if ((name.equals("getTotalArmorValue") || name.equals("func_110148_a"))
                        && desc.equals("()I")) {
                    System.out.println("[CrashGuard] 修改 getTotalArmorValue");
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "com/crashguard/util/ArmorReductionHelper",
                                    "getRealTotalArmor",
                                    "(Lnet/minecraft/entity/EntityLivingBase;)I", false);
                            mv.visitInsn(Opcodes.IRETURN);
                        }
                    };
                }

                return mv;
            }
        }, 0);

        return cw.toByteArray();
    }
}