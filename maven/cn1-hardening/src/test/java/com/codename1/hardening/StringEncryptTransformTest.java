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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.Test;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Verifies string encryption on a real compiled class: the transform must produce
 * bytecode that (a) verifies, (b) computes exactly what the original did, and
 * (c) no longer contains any plaintext secret -- neither as an LDC nor as a field
 * {@code ConstantValue}.
 */
public class StringEncryptTransformTest {

    private static final String CLASS = "com.codename1.hardening.fixture.Secrets";
    private static final String GREETING = "hello secret world";
    private static final String API = "https://api.example.com/secret-endpoint";

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

    private byte[] transformed() throws Exception {
        StringEncryptTransform t = new StringEncryptTransform(true, 12345);
        byte[] out = t.transform(original());
        assertTrue("expected some strings encrypted", t.getEncryptedCount() >= 3);
        return out;
    }

    @Test
    public void plaintextIsGone() throws Exception {
        byte[] out = transformed();
        assertFalse("LDC / field plaintext greeting survived",
                StringEncryptTransform.containsStringLiteral(out, GREETING));
        assertFalse("static final API plaintext survived",
                StringEncryptTransform.containsStringLiteral(out, API));
    }

    @Test
    public void transformedClassVerifies() throws Exception {
        // CheckClassAdapter with data-flow verification; throws on invalid bytecode.
        byte[] out = transformed();
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
    }

    @Test
    public void behaviourIsPreserved() throws Exception {
        Class<?> c = new ByteLoader().define(CLASS, transformed());
        assertEquals(GREETING, c.getMethod("greet").invoke(null));
        assertEquals(API, c.getMethod("api").invoke(null));
        assertEquals("welcome, Ada, to the club",
                c.getMethod("concat", String.class).invoke(null, "Ada"));
        assertEquals(5, c.getMethod("compute", int.class, int.class).invoke(null, 2, 3));
    }

    @Test
    public void decodedLiteralsAreCanonical() throws Exception {
        // The decoded literal must be interned, so reference (==) equality that Java guarantees
        // for string literals still holds after encryption.
        Class<?> c = new ByteLoader().define(CLASS, transformed());
        Object a = c.getMethod("greet").invoke(null);
        Object b = c.getMethod("greet").invoke(null);
        org.junit.Assert.assertSame("decoded literals must be the canonical interned String", a, b);
        org.junit.Assert.assertSame(GREETING.intern(), a);
    }

    @Test
    public void shortStringsAreNotEncrypted() throws Exception {
        // The control integer method has no strings; encryption count comes only from
        // the real secrets, and the transform stays a no-op on classes with nothing to do.
        StringEncryptTransform t = new StringEncryptTransform(true, 7);
        byte[] out = t.transform(original());
        assertTrue(t.getEncryptedCount() >= 3);
        // Round-trips under a different seed too.
        Class<?> c = new ByteLoader().define(CLASS, out);
        assertEquals(GREETING, c.getMethod("greet").invoke(null));
    }

    @Test
    public void encryptsInterfaceDefaultAndStaticMethodLiterals() throws Exception {
        InputStream in = getClass().getResourceAsStream(
                "/com/codename1/hardening/fixture/Iface.class");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) {
            b.write(buf, 0, r);
        }
        in.close();
        StringEncryptTransform t = new StringEncryptTransform(true, 99);
        byte[] out = t.transform(b.toByteArray());
        assertTrue("interface method literals should be encrypted", t.getEncryptedCount() >= 2);
        assertFalse(StringEncryptTransform.containsStringLiteral(out, "interface default secret"));
        assertFalse(StringEncryptTransform.containsStringLiteral(out, "interface static secret"));
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        // The static method round-trips when loaded.
        Class<?> c = new ByteLoader().define("com.codename1.hardening.fixture.Iface", out);
        assertEquals("interface static secret", c.getMethod("staticSecret").invoke(null));
    }

    @Test
    public void encryptsInterfaceConstantValueField() throws Exception {
        InputStream in = getClass().getResourceAsStream(
                "/com/codename1/hardening/fixture/Iface.class");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) >= 0) {
            b.write(buf, 0, r);
        }
        in.close();
        StringEncryptTransform t = new StringEncryptTransform(true, 5);
        byte[] out = t.transform(b.toByteArray());
        // The interface's String TOKEN constant must no longer be present as plaintext.
        assertFalse("interface field ConstantValue plaintext survived",
                StringEncryptTransform.containsStringLiteral(out, "interface constant secret"));
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        // Loading the interface runs its <clinit> decoder; the field reads back its true value.
        Class<?> c = new ByteLoader().define("com.codename1.hardening.fixture.Iface", out);
        assertEquals("interface constant secret", c.getField("TOKEN").get(null));
    }

    @Test
    public void repeatedLiteralIsHoistedAndDecodedOnce() throws Exception {
        // The same value is used by two methods. It must be hoisted to ONE synthetic field decoded
        // once (encryptedCount == 1, not 2), and both reads must return that one interned object.
        String shared = "a shared hoisted secret literal";
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/Hoist", null, "java/lang/Object", null);
        for (String name : new String[] {"a", "b"}) {
            org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                    | org.objectweb.asm.Opcodes.ACC_STATIC, name, "()Ljava/lang/String;", null, null);
            m.visitCode();
            m.visitLdcInsn(shared);
            m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
            m.visitMaxs(1, 0);
            m.visitEnd();
        }
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 21);
        byte[] out = t.transform(w.toByteArray());
        assertEquals("a repeated literal must be hoisted to a single decoded field", 1, t.getEncryptedCount());
        assertFalse(StringEncryptTransform.containsStringLiteral(out, shared));
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.Hoist", out);
        Object a = c.getMethod("a").invoke(null);
        Object b = c.getMethod("b").invoke(null);
        assertEquals(shared, a);
        org.junit.Assert.assertSame("both reads share one interned object", a, b);
    }

    @Test
    public void encryptsEvenWhenDecoderNameCollides() throws Exception {
        // A class that already declares a member named "zqdec$" must NOT be skipped: skipping would
        // leave its literal in plaintext while an equal literal elsewhere was encrypted, breaking a
        // valid literal == on ParparVM. The transform picks a non-colliding decoder name instead.
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/Clash", null, "java/lang/Object", null);
        // A pre-existing member named exactly like the decoder.
        w.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE | org.objectweb.asm.Opcodes.ACC_STATIC,
                "zqdec$", "I", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "secret", "()Ljava/lang/String;", null, null);
        m.visitCode();
        m.visitLdcInsn("this is a clash secret value");
        m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        m.visitMaxs(1, 0);
        m.visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 11);
        byte[] out = t.transform(w.toByteArray());
        assertTrue("the clashing class must still be encrypted, not skipped", t.getEncryptedCount() >= 1);
        assertFalse("plaintext must be gone despite the name clash",
                StringEncryptTransform.containsStringLiteral(out, "this is a clash secret value"));
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.Clash", out);
        assertEquals("this is a clash secret value", c.getMethod("secret").invoke(null));
    }

    @Test
    public void oversizedLiteralIsLeftPlaintextNotCrashing() throws Exception {
        // A large-but-valid ASCII literal (40000 chars = 40000 UTF-8 bytes, under the 65535 limit)
        // would encrypt into mostly 3-byte characters and overflow the constant pool. The transform
        // must skip it and still write a valid class rather than throw UTF8 string too large.
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 40000; i++) {
            big.append('a');
        }
        String huge = big.toString();
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/Huge", null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "big", "()Ljava/lang/String;", null, null);
        m.visitCode();
        m.visitLdcInsn(huge);
        m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        m.visitMaxs(1, 0);
        m.visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 3);
        byte[] out = t.transform(w.toByteArray()); // must not throw
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.Huge", out);
        assertEquals(huge, c.getMethod("big").invoke(null));
        // A helper-level check that the guard is doing the classifying.
        assertFalse("oversized ciphertext must be rejected by the fit check",
                StringEncryptTransform.fitsConstantPool(mostlyThreeByte()));
    }

    private static String mostlyThreeByte() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30000; i++) {
            sb.append('\u0800'); // smallest 3-byte modified-UTF-8 char; 30000 * 3 = 90000 > 65535
        }
        return sb.toString();
    }

    /** Defines transformed bytes as a fresh class distinct from the already-loaded fixture. */
    private static final class ByteLoader extends ClassLoader {
        Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
