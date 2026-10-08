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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// What the compatibility layer tests share: compiling a fixture the way an
/// application is compiled -- by javac, against the JDK -- and reading back
/// which members a class file refers to.
final class CompatFixtures {

    private CompatFixtures() {
    }

    /// Compiles Java sources into `classesDir`. `nameThenSource` alternates
    /// a path (`p/Foo.java`) and the file's text.
    static void compile(File sourceDir, File classesDir, String... nameThenSource) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull("These tests need a JDK, not a JRE", javac);
        List<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(classesDir.getAbsolutePath());
        args.add("-g");
        args.add("-nowarn");
        args.add("-encoding");
        args.add("UTF-8");
        for (int i = 0; i < nameThenSource.length; i += 2) {
            File f = new File(sourceDir, nameThenSource[i]);
            assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
            Files.write(f.toPath(), nameThenSource[i + 1].getBytes("UTF-8"));
            args.add(f.getAbsolutePath());
        }
        assertTrue(classesDir.isDirectory() || classesDir.mkdirs());
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int result = javac.run(null, null, errors, args.toArray(new String[args.size()]));
        assertEquals(new String(errors.toByteArray(), "UTF-8"), 0, result);
    }

    /// Every method and field a class's code refers to, as `owner.name`, a
    /// method reference's target included.
    static Set<String> members(byte[] cls) {
        final Set<String> out = new TreeSet<String>();
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String n, String d, boolean itf) {
                        out.add(owner + "." + n);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String n, String d) {
                        out.add(owner + "." + n);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String n, String d, Handle bootstrap, Object... arguments) {
                        for (Object argument : arguments) {
                            if (argument instanceof Handle) {
                                out.add(((Handle) argument).getOwner() + "." + ((Handle) argument).getName());
                            }
                        }
                    }
                };
            }
        }, 0);
        return out;
    }

    /// A loader that defines the given classes itself, ahead of its parent:
    /// a relocated fixture, or a generated class that has the name of one
    /// the parent already holds.
    static final class Defining extends ClassLoader {
        Defining(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(byte[] cls) {
            return defineClass(null, cls, 0, cls.length);
        }
    }
}
