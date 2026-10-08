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
package com.codename1.camera;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AndroidCameraFrameConverterTest {
    @TempDir static Path temporary;
    private static URLClassLoader loader;
    private static Method convert;

    @BeforeAll
    static void compilePortConverter() throws Exception {
        Path source = Paths.get("../../Ports/Android/src/com/codename1/impl/android/AndroidCameraFrameConverter.java");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", temporary.toString(), source.toString()));
        loader = new URLClassLoader(new URL[] {temporary.toUri().toURL()}, null);
        Class<?> type = loader.loadClass("com.codename1.impl.android.AndroidCameraFrameConverter");
        convert = type.getDeclaredMethod("toNV21", int.class, int.class,
                ByteBuffer.class, int.class, int.class, ByteBuffer.class, int.class,
                int.class, ByteBuffer.class, int.class, int.class);
        convert.setAccessible(true);
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) loader.close();
    }

    private byte[] pack(ByteBuffer y, int ys, ByteBuffer u, int us, int up,
                        ByteBuffer v, int vs, int vp) throws Exception {
        return (byte[]) convert.invoke(null, 4, 4, y, ys, 1, u, us, up, v, vs, vp);
    }

    private byte[] expected() {
        return new byte[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,
                31,21,32,22,33,23,34,24};
    }

    @Test
    void planarChromaIsInterleavedVU() throws Exception {
        assertArrayEquals(expected(), pack(ByteBuffer.wrap(new byte[] {
                1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}), 4,
                ByteBuffer.wrap(new byte[] {21,22,23,24}), 2, 1,
                ByteBuffer.wrap(new byte[] {31,32,33,34}), 2, 1));
    }

    @Test
    void paddedRowsAndFinalRowWithoutTrailingPaddingAreSupported() throws Exception {
        assertArrayEquals(expected(), pack(ByteBuffer.wrap(new byte[] {
                1,2,3,4,99,99,5,6,7,8,99,99,9,10,11,12,99,99,13,14,15,16}), 6,
                ByteBuffer.wrap(new byte[] {21,99,22,99,99,99,23,99,24}), 6, 2,
                ByteBuffer.wrap(new byte[] {31,99,32,99,99,99,33,99,34}), 6, 2));
    }

    @Test
    void overlappingReadOnlyPlanesAndBufferPositionsArePreserved() throws Exception {
        ByteBuffer y = ByteBuffer.allocateDirect(17);
        y.put((byte) 99);
        for (int i = 1; i <= 16; i++) y.put((byte) i);
        y.flip();
        y.position(1);
        ByteBuffer uv = ByteBuffer.wrap(new byte[] {99,31,21,32,22,33,23,34,24});
        ByteBuffer v = uv.asReadOnlyBuffer();
        v.position(1);
        v.limit(8);
        ByteBuffer u = uv.asReadOnlyBuffer();
        u.position(2);
        assertArrayEquals(expected(), pack(y, 4, u, 4, 2, v, 4, 2));
        assertEquals(1, y.position());
        assertEquals(2, u.position());
        assertEquals(1, v.position());
        assertEquals(8, v.limit());
    }

    @Test
    void truncatedPlaneFailsInsteadOfEncodingCorruption() {
        assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> pack(ByteBuffer.allocate(15), 4, ByteBuffer.allocate(4), 2, 1,
                        ByteBuffer.allocate(4), 2, 1));
    }

    @Test
    void oddDimensionsAndOverflowAreRejected() {
        for (int width : new int[] {3, 0, Integer.MAX_VALUE - 1}) {
            java.lang.reflect.InvocationTargetException error = assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> convert.invoke(null, width, 4, ByteBuffer.allocate(16), 4, 1,
                            ByteBuffer.allocate(4), 2, 1, ByteBuffer.allocate(4), 2, 1));
            assertTrue(error.getCause() instanceof IllegalArgumentException);
        }
    }
}
