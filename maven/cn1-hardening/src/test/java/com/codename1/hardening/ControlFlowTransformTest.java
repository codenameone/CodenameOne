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
package com.codename1.hardening;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

/** The opaque-predicate guard must verify and leave behaviour a strict no-op. */
public class ControlFlowTransformTest {

    private static final String CLASS = "com.codename1.hardening.fixture.Secrets";

    private byte[] original() throws Exception {
        InputStream in = getClass().getResourceAsStream(
                "/com/codename1/hardening/fixture/Secrets.class");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) {
            b.write(buf, 0, r);
        }
        in.close();
        return b.toByteArray();
    }

    @Test
    public void guardsVerifyAndPreserveBehaviour() throws Exception {
        ControlFlowTransform t = new ControlFlowTransform();
        byte[] out = t.transform(original());
        assertTrue("expected several methods guarded", t.getGuardedMethods() >= 3);

        CheckClassAdapter.verify(new ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));

        Class<?> c = new ByteLoader().define(CLASS, out);
        assertEquals("hello secret world", c.getMethod("greet").invoke(null));
        assertEquals(5, c.getMethod("compute", int.class, int.class).invoke(null, 2, 3));
        assertEquals("welcome, Bo, to the club",
                c.getMethod("concat", String.class).invoke(null, "Bo"));
    }

    @Test
    public void intenseGuardsVerifyAndPreserveBehaviour() throws Exception {
        // Paranoid intensity: two nested guards per method. Must still verify and be a no-op.
        byte[] out = new ControlFlowTransform(null, 2).transform(original());
        CheckClassAdapter.verify(new ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define(CLASS + "$P", rename(out, CLASS, CLASS + "$P"));
        // A distinct class name via a fresh loader; behaviour must be unchanged.
        assertEquals("hello secret world", c.getMethod("greet").invoke(null));
        assertEquals(5, c.getMethod("compute", int.class, int.class).invoke(null, 2, 3));
    }

    @Test
    public void guardsAClassThatAlreadyDeclaresAGuardFieldName() throws Exception {
        // A class already declares a field named exactly like the guard field. It must still be
        // guarded (with a non-colliding field), not returned unchanged on a false "already
        // transformed" assumption.
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/GuardClash", null, "java/lang/Object", null);
        w.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE | org.objectweb.asm.Opcodes.ACC_STATIC,
                ControlFlowTransform.GUARD_FIELD, "I", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "add", "(II)I", null, null);
        m.visitCode();
        m.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 0);
        m.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 1);
        m.visitInsn(org.objectweb.asm.Opcodes.IADD);
        m.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
        m.visitMaxs(2, 2);
        m.visitEnd();
        w.visitEnd();

        ControlFlowTransform t = new ControlFlowTransform();
        byte[] out = t.transform(w.toByteArray());
        assertTrue("the clashing class must still be guarded, not skipped", t.getGuardedMethods() >= 1);
        CheckClassAdapter.verify(new ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.GuardClash", out);
        assertEquals(5, c.getMethod("add", int.class, int.class).invoke(null, 2, 3));
    }

    @Test
    public void oversizedMethodIsSkippedNotOverflowed() throws Exception {
        // A method already near the 65535-byte limit cannot take a guard without overflowing. It must
        // be skipped (and reported), while a normal sibling method is still guarded -- the build must
        // not abort on a valid input class.
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/BigMethod", null, "java/lang/Object", null);
        // ~62 KB of harmless ICONST_0/POP filler: too large to accept a guard.
        org.objectweb.asm.MethodVisitor big = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "big", "()V", null, null);
        big.visitCode();
        for (int i = 0; i < 31000; i++) {
            big.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
            big.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        big.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        big.visitMaxs(1, 0);
        big.visitEnd();
        // A normal method that can and must still be guarded.
        org.objectweb.asm.MethodVisitor add = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "add", "(II)I", null, null);
        add.visitCode();
        add.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 0);
        add.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 1);
        add.visitInsn(org.objectweb.asm.Opcodes.IADD);
        add.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
        add.visitMaxs(2, 2);
        add.visitEnd();
        w.visitEnd();

        ControlFlowTransform t = new ControlFlowTransform();
        byte[] out = t.transform(w.toByteArray());
        assertEquals("the near-limit method must be skipped", 1, t.getOversizedMethods());
        assertTrue("the normal method must still be guarded", t.getGuardedMethods() >= 1);
        CheckClassAdapter.verify(new ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.BigMethod", out);
        assertEquals(5, c.getMethod("add", int.class, int.class).invoke(null, 2, 3));
    }

    // Renames the class internal name so the intense variant can load beside the plain one.
    private static byte[] rename(byte[] bytes, String from, String to) {
        org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(bytes);
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(0);
        cr.accept(new org.objectweb.asm.commons.ClassRemapper(cw,
                new org.objectweb.asm.commons.SimpleRemapper(from.replace('.', '/'),
                        to.replace('.', '/'))), 0);
        return cw.toByteArray();
    }

    private static final class ByteLoader extends ClassLoader {
        Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
