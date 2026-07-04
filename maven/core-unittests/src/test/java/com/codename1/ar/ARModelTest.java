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
package com.codename1.ar;

import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.junit.UITestBase;
import com.codename1.util.Base64;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link ARModel}: glTF byte retention with lazy device-free
 * parsing, and the mesh/color/texture factories.
 */
class ARModelTest extends UITestBase {

    // A single-triangle glTF built the same way as GltfLoaderTest.

    private static void putFloatLE(byte[] b, int p, float f) {
        int bits = Float.floatToIntBits(f);
        b[p] = (byte) (bits & 0xff);
        b[p + 1] = (byte) ((bits >> 8) & 0xff);
        b[p + 2] = (byte) ((bits >> 16) & 0xff);
        b[p + 3] = (byte) ((bits >> 24) & 0xff);
    }

    private static void putUShortLE(byte[] b, int p, int v) {
        b[p] = (byte) (v & 0xff);
        b[p + 1] = (byte) ((v >> 8) & 0xff);
    }

    private static byte[] triangleGltf() {
        byte[] buf = new byte[42];
        float[] pos = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        for (int i = 0; i < 9; i++) {
            putFloatLE(buf, i * 4, pos[i]);
        }
        putUShortLE(buf, 36, 0);
        putUShortLE(buf, 38, 1);
        putUShortLE(buf, 40, 2);
        String dataUri = "data:application/octet-stream;base64," + Base64.encodeNoNewline(buf);
        String json = "{"
                + "\"asset\":{\"version\":\"2.0\"},"
                + "\"buffers\":[{\"byteLength\":42,\"uri\":\"" + dataUri + "\"}],"
                + "\"bufferViews\":["
                + "{\"buffer\":0,\"byteOffset\":0,\"byteLength\":36},"
                + "{\"buffer\":0,\"byteOffset\":36,\"byteLength\":6}],"
                + "\"accessors\":["
                + "{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"},"
                + "{\"bufferView\":1,\"componentType\":5123,\"count\":3,\"type\":\"SCALAR\"}],"
                + "\"meshes\":[{\"primitives\":[{"
                + "\"attributes\":{\"POSITION\":0},\"indices\":1,\"mode\":4}]}]"
                + "}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void gltfModelRetainsBytesAndLazilyParsesGeometry() {
        byte[] data = triangleGltf();
        ARModel model = ARModel.fromGltf(data);
        assertArrayEquals(data, model.getGltfBytes());
        // Defensive copy: mutating the returned bytes must not corrupt the model.
        model.getGltfBytes()[0] = 0;

        Mesh mesh = model.getMesh();
        assertNotNull(mesh);
        assertEquals(3, mesh.getVertices().getVertexCount());
        assertSame(mesh, model.getMesh(), "parsing happens once");
        assertNull(model.getBaseColorImage());
        assertEquals(0xffffffff, model.getColor());
    }

    @Test
    void gltfModelFromStream() throws IOException {
        ARModel model = ARModel.fromGltf(new ByteArrayInputStream(triangleGltf()));
        assertEquals(3, model.getMesh().getVertices().getVertexCount());
    }

    @Test
    void meshModelExposesMeshAndColor() {
        Mesh sphere = Primitives.sphere(0.5f, 4, 6, false);
        ARModel model = ARModel.fromMesh(sphere, 0xffff0000);
        assertSame(sphere, model.getMesh());
        assertEquals(0xffff0000, model.getColor());
        assertNull(model.getGltfBytes());
        assertNull(model.getBaseColorImage());
    }

    @Test
    void factoriesRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ARModel.fromGltf((byte[]) null);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ARModel.fromGltf(new byte[0]);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ARModel.fromMesh(null, 0xffffffff);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                ARModel.fromMesh(Primitives.sphere(1f, 4, 6, false), null);
            }
        });
    }
}
