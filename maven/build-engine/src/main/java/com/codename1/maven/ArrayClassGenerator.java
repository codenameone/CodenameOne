/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Makes the array class of every type an application names with a class
/// literal exist on a device.
///
/// #### Why
///
/// ParparVM has an array class only for a type some bytecode creates an
/// array of: an `anewarray`, or a class literal of the array type. A
/// method that returns `FooListener[]` or casts to it does not count. So
/// `listenerList.getListeners(FooListener.class)`, or any other
/// `Array.newInstance(FooListener.class, n)`, threw "the component class
/// has no registered array class" for a listener type the application
/// defines and never makes an array of itself, while the desktop, the
/// simulator and Android, which make array classes on demand, ran it.
///
/// A class handed to `Array.newInstance` at run time got into the program
/// as a class literal, or as the component type of an array that already
/// exists. The second kind has its array class. For the first kind this
/// generates one class holding the literal of the array type of every
/// class literal in the application's classes. The translator registers
/// array types as it reads a class, so the generated class does its work
/// by being among the classes; nothing calls it.
///
/// A class literal that is never turned into an array costs one array
/// class that is not used.
public final class ArrayClassGenerator {

    /// The internal name of the generated class.
    public static final String HOLDER = "com/codename1/generated/compat/ArrayClasses";

    private final File classesDir;

    /// Creates a generator over a directory of compiled classes.
    public ArrayClassGenerator(File classesDir) {
        this.classesDir = classesDir;
    }

    /// Reads the application's classes, given by internal name, and
    /// writes the holder, or removes one a previous run left when there
    /// is nothing to hold. Answers the number of array types held.
    public int run(Collection<String> appClasses) throws IOException {
        final Set<String> named = new TreeSet<String>();
        for (String name : appClasses) {
            File file = new File(classesDir, name + ".class");
            if (HOLDER.equals(name) || !file.isFile()) {
                continue;
            }
            collect(Files.readAllBytes(file.toPath()), named);
        }
        File dest = new File(classesDir, HOLDER + ".class");
        if (named.isEmpty()) {
            if (dest.isFile() && !dest.delete()) {
                throw new IOException("Could not delete " + dest);
            }
            return 0;
        }
        ClassRelocator.writeIfDifferent(dest, holder(named));
        return named.size();
    }

    /// Adds the internal name of every class a class literal in `bytes`
    /// names to `named`. A literal of an array type has its array class
    /// already.
    static void collect(byte[] bytes, final Set<String> named) {
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type && ((Type) value).getSort() == Type.OBJECT) {
                            named.add(((Type) value).getInternalName());
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    /// The holder: one static method that loads the class of the array
    /// type of every name and drops it.
    static byte[] holder(Collection<String> named) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, HOLDER, null,
                "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "types", "()V", null, null);
        mv.visitCode();
        for (String name : named) {
            mv.visitLdcInsn(Type.getType("[L" + name + ";"));
            mv.visitInsn(Opcodes.POP);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
