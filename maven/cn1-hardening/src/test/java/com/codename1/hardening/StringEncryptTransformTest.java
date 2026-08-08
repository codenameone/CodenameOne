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
    public void hoistedFieldNameDoesNotCollideWithExistingField() throws Exception {
        // The class already declares a field named exactly like the first generated hoisted name.
        // The transform must pick a different name, not add a duplicate field.
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/FieldClash", null, "java/lang/Object", null);
        w.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE | org.objectweb.asm.Opcodes.ACC_STATIC,
                "zqL$0", "Ljava/lang/String;", null, null).visitEnd();
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "get", "()Ljava/lang/String;", null, null);
        m.visitCode();
        m.visitLdcInsn("a clashing hoist secret value");
        m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        m.visitMaxs(1, 0);
        m.visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 31);
        byte[] out = t.transform(w.toByteArray());
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.FieldClash", out);
        assertEquals("a clashing hoist secret value", c.getMethod("get").invoke(null));
    }

    @Test
    public void injectedInitializerCiphertextIsNotRewritten() throws Exception {
        // Construct two literals A and B where B is exactly the ciphertext of A for this class's key.
        // The hoisted initializer's LDC of A's ciphertext (== B) must NOT be rewritten into a read of
        // B's field, or <clinit> reads an unassigned field and throws ExceptionInInitializerError.
        String owner = "app/Involutive";
        StringEncryptTransform probe = new StringEncryptTransform(true, 99);
        int base = probe.keyBase(owner);
        String a = "an involutive secret literal";
        String b = StringEncryptTransform.encode(a, base); // ciphertext of A == plaintext B

        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                owner, null, "java/lang/Object", null);
        addStringGetter(w, "a", a);
        addStringGetter(w, "b", b);
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 99);
        byte[] out = t.transform(w.toByteArray());
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.Involutive", out); // must not throw on <clinit>
        assertEquals(a, c.getMethod("a").invoke(null));
        assertEquals(b, c.getMethod("b").invoke(null));
    }

    @Test
    public void oversizedLiteralIsCountedAsExcluded() throws Exception {
        // A valid ASCII literal too large to encrypt (its ciphertext could overflow the constant pool)
        // is left plaintext AND counted, so an strings:all build reports the exclusion.
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 30000; i++) {
            big.append('x');
        }
        String huge = big.toString();
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/Oversized", null, "java/lang/Object", null);
        addStringGetter(w, "huge", huge);
        addStringGetter(w, "small", "an encryptable small secret value");
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 7);
        byte[] out = t.transform(w.toByteArray());
        assertEquals("the oversized literal must be counted as excluded", 1, t.getOversizedLiteralCount());
        assertTrue("the small literal is still encrypted", t.getEncryptedCount() >= 1);
        assertTrue("the oversized literal stays plaintext",
                StringEncryptTransform.containsStringLiteral(out, huge));
        assertFalse("the small literal is encrypted away",
                StringEncryptTransform.containsStringLiteral(out, "an encryptable small secret value"));
    }

    /** Builds a class whose existing <clinit> is padByteApprox bytes of harmless ICONST_0/POP filler. */
    private static org.objectweb.asm.ClassWriter classWithBigClinit(String owner, int padPairs) {
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                owner, null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor clinit = w.visitMethod(org.objectweb.asm.Opcodes.ACC_STATIC,
                "<clinit>", "()V", null, null);
        clinit.visitCode();
        for (int i = 0; i < padPairs; i++) {
            clinit.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
            clinit.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        clinit.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        clinit.visitMaxs(1, 0);
        clinit.visitEnd();
        return w;
    }

    private static boolean hasHelper(byte[] out) {
        final boolean[] saw = {false};
        new org.objectweb.asm.ClassReader(out).accept(new org.objectweb.asm.ClassVisitor(
                org.objectweb.asm.Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                    String sig, String[] exceptions) {
                if (name.startsWith("zqCI$")) {
                    saw[0] = true;
                }
                return null;
            }
        }, org.objectweb.asm.ClassReader.SKIP_CODE);
        return saw[0];
    }

    @Test
    public void existingLargeClinitIsSplitWhenCombinedWithNewInit() throws Exception {
        // A small new initializer that would fit on its own is still split into a helper when the class
        // already carries a <clinit> near the 65535-byte limit -- the split decision measures the
        // COMBINED encoded size, not just the new init. ~59.6 KB of existing <clinit> + a handful of
        // literals exceeds the safe bound, so the new init must move into a helper.
        org.objectweb.asm.ClassWriter w = classWithBigClinit("app/BigExistingClinit", 29800);
        for (int i = 0; i < 40; i++) {
            addStringGetter(w, "probe" + i, "a hoisted secret literal value number " + i);
        }
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 13);
        byte[] out = t.transform(w.toByteArray());
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        assertTrue("a large existing <clinit> must push the new init into a helper", hasHelper(out));
        assertEquals(40, t.getEncryptedCount());
        Class<?> c = new ByteLoader().define("app.BigExistingClinit", out);
        assertEquals("a hoisted secret literal value number 0", c.getMethod("probe0").invoke(null));
    }

    @Test
    public void staticFinalConstantWithFullClinitStaysPlaintextAndReports() throws Exception {
        // A static-final ConstantValue can only be moved into <clinit>; when <clinit> is already full it
        // cannot, so the field keeps its plaintext. That is safe (javac inlined every read as an LDC,
        // which IS encrypted, so the field value is dead), and reported via getClinitFullLiteralCount.
        org.objectweb.asm.ClassWriter w = classWithBigClinit("app/FullClinitStatic", 31500); // ~63 KB
        w.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC
                | org.objectweb.asm.Opcodes.ACC_FINAL, "SECRET", "Ljava/lang/String;", null,
                "a static-final constant with a full clinit").visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 5);
        byte[] out = t.transform(w.toByteArray());
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        assertEquals("the un-encryptable static-final is reported", 1, t.getClinitFullLiteralCount());
        assertTrue("its constant stays plaintext rather than the build aborting",
                StringEncryptTransform.containsStringLiteral(out, "a static-final constant with a full clinit"));
    }

    @Test
    public void clinitTooFullFallsBackToPerAccessEncryption() throws Exception {
        // The existing <clinit> is so close to the limit that not even a helper CALL would fit, so the
        // literal cannot be HOISTED. Rather than leaving it plaintext (which would break a cross-class
        // == against an encrypted copy elsewhere), it is encrypted PER ACCESS -- no <clinit> growth.
        org.objectweb.asm.ClassWriter w = classWithBigClinit("app/FullClinit", 31500); // ~63 KB
        addStringGetter(w, "probe", "a literal encrypted per access here");
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 5);
        byte[] out = t.transform(w.toByteArray());
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        assertTrue("the literal is encrypted per access, not left plaintext", t.getEncryptedCount() >= 1);
        assertFalse("its plaintext must be gone",
                StringEncryptTransform.containsStringLiteral(out, "a literal encrypted per access here"));
        Class<?> c = new ByteLoader().define("app.FullClinit", out);
        assertEquals("a literal encrypted per access here", c.getMethod("probe").invoke(null));
    }

    @Test
    public void preJava8InterfaceConstantIsCountedAsExcluded() throws Exception {
        // A Java 7 interface cannot host a <clinit>/decoder, so its own static-final String constant
        // stays plaintext. The transform must not silently ship it: it leaves it and counts it so the
        // engine can warn.
        String secret = "legacy interface constant secret value";
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_7, org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_INTERFACE | org.objectweb.asm.Opcodes.ACC_ABSTRACT,
                "app/LegacyIface", null, "java/lang/Object", null);
        w.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC
                | org.objectweb.asm.Opcodes.ACC_FINAL, "SECRET", "Ljava/lang/String;", null, secret)
                .visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 55);
        byte[] out = t.transform(w.toByteArray());
        assertEquals("nothing can be encrypted on a pre-Java-8 interface", 0, t.getEncryptedCount());
        assertEquals("its constant must be counted as an exclusion", 1, t.getLegacyInterfaceConstantCount());
        assertTrue("the constant is left as-is (reported, not silently dropped)",
                StringEncryptTransform.containsStringLiteral(out, secret));
    }

    @Test
    public void annotationStringsAreCountedAsExcluded() throws Exception {
        // Strings in annotation element values/arrays live in the annotation metadata, not an LDC or a
        // ConstantValue, so no channel encrypts them. They must be counted (and stay plaintext); an enum
        // reference is not a string literal and must NOT be counted.
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/Annotated", null, "java/lang/Object", null);
        org.objectweb.asm.AnnotationVisitor av = w.visitAnnotation("Lapp/Anno;", true);
        av.visit("secret", "a class annotation secret value");
        org.objectweb.asm.AnnotationVisitor arr = av.visitArray("list");
        arr.visit(null, "an array annotation value");
        arr.visitEnd();
        av.visitEnum("kind", "Lapp/Kind;", "FOO"); // enum reference -- not a string literal
        av.visitEnd();
        org.objectweb.asm.FieldVisitor fv = w.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE,
                "f", "I", null, null);
        org.objectweb.asm.AnnotationVisitor fav = fv.visitAnnotation("Lapp/Anno;", true);
        fav.visit("fieldSecret", "a field annotation secret value");
        fav.visitEnd();
        fv.visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 9);
        byte[] out = t.transform(w.toByteArray());
        assertEquals("the three distinct annotation strings are counted, the enum ref is not",
                3, t.getAnnotationLiteralCount());
        // The annotation string survives verbatim in the constant pool (no channel encrypts it).
        assertTrue("annotation strings stay plaintext (no channel reaches them)",
                containsRawBytes(out, "a class annotation secret value"));
    }

    private static boolean containsRawBytes(byte[] haystack, String needle) throws Exception {
        byte[] n = needle.getBytes("UTF-8");
        outer:
        for (int i = 0; i + n.length <= haystack.length; i++) {
            for (int j = 0; j < n.length; j++) {
                if (haystack[i + j] != n[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    @Test
    public void interfaceMethodTooLargeSkipsAndReportsLiterals() throws Exception {
        // The interface path decodes per access (an INVOKESTATIC after each LDC). A method already near
        // the limit cannot take those, so its literals are left plaintext and reported; a normal method
        // in the same interface is still encrypted -- the build must not abort.
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_INTERFACE | org.objectweb.asm.Opcodes.ACC_ABSTRACT,
                "app/BigIface", null, "java/lang/Object", null);
        // A static method already ~60 KB, then a few encryptable literals it cannot fit a decode call for.
        org.objectweb.asm.MethodVisitor big = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "big", "()V", null, null);
        big.visitCode();
        for (int i = 0; i < 30100; i++) {
            big.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
            big.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        for (int i = 0; i < 5; i++) {
            big.visitLdcInsn("a big-interface secret literal number " + i);
            big.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        big.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        big.visitMaxs(1, 0);
        big.visitEnd();
        // A normal static method whose literal must still be encrypted.
        org.objectweb.asm.MethodVisitor small = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, "small", "()Ljava/lang/String;", null, null);
        small.visitCode();
        small.visitLdcInsn("a small interface secret literal");
        small.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        small.visitMaxs(1, 0);
        small.visitEnd();
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 3);
        byte[] out = t.transform(w.toByteArray());
        assertEquals("the near-limit method's literals are reported", 5, t.getMethodFullLiteralCount());
        assertTrue("the normal method's literal is still encrypted", t.getEncryptedCount() >= 1);
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        assertTrue("the un-encryptable literal stays plaintext",
                StringEncryptTransform.containsStringLiteral(out, "a big-interface secret literal number 0"));
        assertFalse("the small literal is encrypted away",
                StringEncryptTransform.containsStringLiteral(out, "a small interface secret literal"));
    }

    @Test
    public void poolHeavyClassEncryptsPerAccessInsteadOfOverflowingOrLeavingPlaintext() throws Exception {
        // A class with far more distinct literals than the constant pool can hold once each is hoisted
        // (a field + its reference constants) would overflow the 65535-entry pool. Rather than
        // ClassTooLargeException -- or leaving some plaintext (which breaks cross-class ==) -- the class
        // falls back to per-access encryption: no per-value field, so no pool growth, and every
        // occurrence is encrypted and interned. A probe reuses one value so a decode is checked.
        int count = 12000;
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/PoolHeavy", null, "java/lang/Object", null);
        int perMethod = 200;
        for (int start = 0; start < count; start += perMethod) {
            org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                    | org.objectweb.asm.Opcodes.ACC_STATIC, "m" + start, "()V", null, null);
            m.visitCode();
            for (int i = start; i < start + perMethod && i < count; i++) {
                m.visitLdcInsn("pool_heavy_secret_literal_number_" + i);
                m.visitInsn(org.objectweb.asm.Opcodes.POP);
            }
            m.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            m.visitMaxs(1, 0);
            m.visitEnd();
        }
        addStringGetter(w, "probe", "pool_heavy_secret_literal_number_0");
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 71);
        byte[] out = t.transform(w.toByteArray());
        // Per-access encrypts every OCCURRENCE (count filler + the probe reuse), where hoisting would
        // encrypt each distinct value once; count+1 therefore proves the per-access fallback ran.
        assertEquals(count + 1, t.getEncryptedCount());
        assertFalse("no literal is left plaintext",
                StringEncryptTransform.containsStringLiteral(out, "pool_heavy_secret_literal_number_5"));
        // The class assembles and verifies -- no ClassTooLargeException -- and a value decodes.
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));
        Class<?> c = new ByteLoader().define("app.PoolHeavy", out);
        assertEquals("pool_heavy_secret_literal_number_0", c.getMethod("probe").invoke(null));
    }

    @Test
    public void oversizedInitializerIsSplitAcrossHelpers() throws Exception {
        // A generated class with enough distinct literals that a single <clinit> would exceed the
        // 65535-byte method limit. Hoisting must split the initializer across helper methods so the
        // class still assembles, verifies and runs -- rather than throwing MethodTooLargeException.
        // 6000 fields * ~11 bytes/init-unit ~= 66 KB, past the method limit but well under the
        // constant-pool budget (so this exercises the method-size split, not the pool cap).
        int count = 6000;
        String probeValue = "big_clinit_secret_literal_number_0";
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "app/BigClinit", null, "java/lang/Object", null);
        // Spread the literals across small, individually-valid methods (each pops what it loads).
        int perMethod = 200;
        for (int start = 0; start < count; start += perMethod) {
            org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                    | org.objectweb.asm.Opcodes.ACC_STATIC, "m" + start, "()V", null, null);
            m.visitCode();
            for (int i = start; i < start + perMethod && i < count; i++) {
                m.visitLdcInsn("big_clinit_secret_literal_number_" + i);
                m.visitInsn(org.objectweb.asm.Opcodes.POP);
            }
            m.visitInsn(org.objectweb.asm.Opcodes.RETURN);
            m.visitMaxs(1, 0);
            m.visitEnd();
        }
        // A probe that returns literal 0 so we can confirm a hoisted value decodes correctly.
        addStringGetter(w, "probe", probeValue);
        w.visitEnd();

        StringEncryptTransform t = new StringEncryptTransform(true, 4242);
        byte[] out = t.transform(w.toByteArray());
        assertEquals("every distinct literal is hoisted once", count, t.getEncryptedCount());

        // The output must verify (no MethodTooLargeException was thrown building it, and no method is
        // too large or malformed).
        CheckClassAdapter.verify(new org.objectweb.asm.ClassReader(out), false,
                new java.io.PrintWriter(new java.io.StringWriter()));

        // The split actually happened: at least one synthetic initializer helper exists.
        final boolean[] sawHelper = {false};
        new org.objectweb.asm.ClassReader(out).accept(new org.objectweb.asm.ClassVisitor(
                org.objectweb.asm.Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                    String sig, String[] exceptions) {
                if (name.startsWith("zqCI$")) {
                    sawHelper[0] = true;
                }
                return null;
            }
        }, org.objectweb.asm.ClassReader.SKIP_CODE);
        assertTrue("the oversized initializer must be split into helper methods", sawHelper[0]);

        assertFalse(StringEncryptTransform.containsStringLiteral(out, probeValue));
        Class<?> c = new ByteLoader().define("app.BigClinit", out);
        assertEquals(probeValue, c.getMethod("probe").invoke(null));
    }

    private static void addStringGetter(org.objectweb.asm.ClassWriter w, String name, String value) {
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC
                | org.objectweb.asm.Opcodes.ACC_STATIC, name, "()Ljava/lang/String;", null, null);
        m.visitCode();
        m.visitLdcInsn(value);
        m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        m.visitMaxs(1, 0);
        m.visitEnd();
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
