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

    private static final class ByteLoader extends ClassLoader {
        Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }
}
