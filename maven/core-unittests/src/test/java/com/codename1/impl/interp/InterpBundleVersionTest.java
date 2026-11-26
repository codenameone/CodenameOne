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
package com.codename1.impl.interp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The framework version a bundle carries, which the device compares with its
 * own before it runs a pushed program. Driven through the real writer, loaded
 * reflectively like the harness does, so both halves of the format are held.
 */
class InterpBundleVersionTest {

    private static InterpBundle roundTrip(String version) throws Exception {
        Class<?> writerClass = Class.forName("com.codename1.tools.translator.InterpBundleWriter");
        Object writer = writerClass.getDeclaredConstructor().newInstance();
        writerClass.getMethod("setMainClass", String.class).invoke(writer, "com/example/Main");
        Method setVersion = writerClass.getMethod("setFrameworkVersion", String.class);
        setVersion.invoke(writer, version);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writerClass.getMethod("write", java.io.OutputStream.class).invoke(writer, bos);
        return InterpBundleReader.read(new ByteArrayInputStream(bos.toByteArray()));
    }

    @Test
    @DisplayName("the framework version survives the bundle round trip")
    void versionRoundTrips() throws Exception {
        InterpBundle b = roundTrip("7.0.284");
        assertEquals("7.0.284", b.getFrameworkVersion());
        assertEquals("com/example/Main", b.getMainClass(), "the field after it still reads");
    }

    @Test
    @DisplayName("a bundle that names no version reads as unknown")
    void missingVersionIsUnknown() throws Exception {
        assertNull(roundTrip(null).getFrameworkVersion());
        assertNull(roundTrip("").getFrameworkVersion());
    }
}
