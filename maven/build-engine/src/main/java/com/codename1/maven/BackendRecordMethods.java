/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/// Materializes javac's ObjectMethods bootstrap as ordinary methods. The native
/// runtime does not execute java.lang.runtime.ObjectMethods. Explicit user
/// implementations are preserved; component order and boxed equality implement
/// record semantics, including arrays, NaN and signed zero.
final class BackendRecordMethods implements Opcodes {
    private BackendRecordMethods() { }

    static void rewrite(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) rewrite(file);
            else if (file.getName().endsWith(".class")) {
                byte[] before = Files.readAllBytes(file.toPath());
                byte[] after = rewrite(before);
                if (after != before) Files.write(file.toPath(), after);
            }
        }
    }

    static byte[] rewrite(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        if ((node.access & ACC_RECORD) == 0 || node.recordComponents == null) return bytes;
        boolean changed = false;
        for (MethodNode method : node.methods) {
            boolean generated = false;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof InvokeDynamicInsnNode &&
                        "java/lang/runtime/ObjectMethods".equals(((InvokeDynamicInsnNode) instruction).bsm.getOwner())) {
                    generated = true;
                }
            }
            if (!generated) continue;
            if (!(method.name.equals("equals") && method.desc.equals("(Ljava/lang/Object;)Z"))
                    && !(method.name.equals("hashCode") && method.desc.equals("()I"))
                    && !(method.name.equals("toString") && method.desc.equals("()Ljava/lang/String;"))) {
                throw new IllegalArgumentException("Unsupported record bootstrap in " + node.name + "." + method.name);
            }
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.visitCode();
            if (method.name.equals("equals")) equals(node, method);
            else if (method.name.equals("hashCode")) hashCode(node, method);
            else toString(node, method);
            method.visitMaxs(5, method.name.equals("equals") ? 2 : 1);
            method.visitEnd();
            changed = true;
        }
        if (!changed) return bytes;
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void component(ClassNode node, MethodVisitor mv, RecordComponentNode field, int local) {
        mv.visitVarInsn(ALOAD, local);
        if (local != 0) mv.visitTypeInsn(CHECKCAST, node.name);
        mv.visitFieldInsn(GETFIELD, node.name, field.name, field.descriptor);
        Type type = Type.getType(field.descriptor);
        String wrapper;
        switch (type.getSort()) {
            case Type.BOOLEAN: wrapper = "Boolean"; break;
            case Type.BYTE: wrapper = "Byte"; break;
            case Type.CHAR: wrapper = "Character"; break;
            case Type.SHORT: wrapper = "Short"; break;
            case Type.INT: wrapper = "Integer"; break;
            case Type.LONG: wrapper = "Long"; break;
            case Type.FLOAT: wrapper = "Float"; break;
            case Type.DOUBLE: wrapper = "Double"; break;
            default: return;
        }
        String owner = "java/lang/" + wrapper;
        mv.visitMethodInsn(INVOKESTATIC, owner, "valueOf", "(" + field.descriptor + ")L" + owner + ";", false);
    }

    private static void equals(ClassNode node, MethodVisitor mv) {
        Label no = new Label();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(INSTANCEOF, node.name);
        mv.visitJumpInsn(IFEQ, no);
        for (RecordComponentNode field : node.recordComponents) {
            component(node, mv, field, 0);
            component(node, mv, field, 1);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(IFEQ, no);
        }
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        mv.visitLabel(no);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
    }

    private static void hashCode(ClassNode node, MethodVisitor mv) {
        mv.visitInsn(ICONST_0);
        for (RecordComponentNode field : node.recordComponents) {
            mv.visitIntInsn(BIPUSH, 31);
            mv.visitInsn(IMUL);
            component(node, mv, field, 0);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hashCode", "(Ljava/lang/Object;)I", false);
            mv.visitInsn(IADD);
        }
        mv.visitInsn(IRETURN);
    }

    private static void toString(ClassNode node, MethodVisitor mv) {
        String name = node.name.substring(node.name.lastIndexOf('/') + 1);
        for (org.objectweb.asm.tree.InnerClassNode inner : node.innerClasses) {
            if (node.name.equals(inner.name) && inner.innerName != null) name = inner.innerName;
        }
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(name + "[");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        boolean first = true;
        for (RecordComponentNode field : node.recordComponents) {
            mv.visitLdcInsn((first ? "" : ", ") + field.name + "=");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            component(node, mv, field, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
            first = false;
        }
        mv.visitLdcInsn("]");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
    }
}
