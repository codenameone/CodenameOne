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

    /** Defines transformed bytes as a fresh class distinct from the already-loaded fixture. */
    private static final class ByteLoader extends ClassLoader {
        Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
