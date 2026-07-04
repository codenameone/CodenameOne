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
package com.codename1.gpu;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.util.Base64;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GltfLoaderTest extends UITestBase {

    /**
     * A graphics device that only allocates plain CPU-side buffers (the concrete
     * createVertexBuffer / createIndexBuffer in the base class). The abstract GPU
     * methods are never reached by GltfLoader.load, so they throw if invoked.
     */
    static final class StubDevice extends GraphicsDevice {
        public GpuCapabilities getCapabilities() {
            throw new UnsupportedOperationException();
        }

        public Texture createTexture(Image image) {
            throw new UnsupportedOperationException();
        }

        public Texture createTexture(int width, int height, int[] argb) {
            throw new UnsupportedOperationException();
        }

        public void clear(int argbColor, boolean color, boolean depth) {
            throw new UnsupportedOperationException();
        }

        public void setViewport(int x, int y, int width, int height) {
            throw new UnsupportedOperationException();
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) {
            throw new UnsupportedOperationException();
        }

        public void dispose(VertexBuffer buffer) {
            throw new UnsupportedOperationException();
        }

        public void dispose(IndexBuffer buffer) {
            throw new UnsupportedOperationException();
        }

        public void dispose(Texture texture) {
            throw new UnsupportedOperationException();
        }
    }

    // ---- little-endian binary helpers for building the glTF buffer ----

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

    private static void putUIntLE(byte[] b, int p, int v) {
        b[p] = (byte) (v & 0xff);
        b[p + 1] = (byte) ((v >> 8) & 0xff);
        b[p + 2] = (byte) ((v >> 16) & 0xff);
        b[p + 3] = (byte) ((v >> 24) & 0xff);
    }

    /**
     * Single triangle: positions (0,0,0)(1,0,0)(0,1,0) as 9 FLOATs (36 bytes)
     * followed by indices 0,1,2 as 3 UNSIGNED_SHORTs (6 bytes). Total 42 bytes.
     */
    private static byte[] triangleBuffer() {
        byte[] buf = new byte[42];
        float[] pos = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        for (int i = 0; i < 9; i++) {
            putFloatLE(buf, i * 4, pos[i]);
        }
        putUShortLE(buf, 36, 0);
        putUShortLE(buf, 38, 1);
        putUShortLE(buf, 40, 2);
        return buf;
    }

    /** glTF JSON describing the triangle buffer above, embedded as a data URI. */
    private static String triangleGltfJson(byte[] buffer) {
        String dataUri = "data:application/octet-stream;base64," + Base64.encodeNoNewline(buffer);
        return "{"
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
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** Wraps glTF JSON into a binary .glb container with a single JSON chunk. */
    private static byte[] toGlb(String json) {
        byte[] jsonBytes = utf8(json);
        // glTF spec pads the JSON chunk to a 4-byte boundary with spaces.
        int pad = (4 - (jsonBytes.length % 4)) % 4;
        int chunkLen = jsonBytes.length + pad;
        int total = 12 + 8 + chunkLen;
        byte[] glb = new byte[total];
        putUIntLE(glb, 0, 0x46546C67); // magic "glTF"
        putUIntLE(glb, 4, 2);          // version
        putUIntLE(glb, 8, total);      // total length
        putUIntLE(glb, 12, chunkLen);  // chunk length
        putUIntLE(glb, 16, 0x4E4F534A); // chunk type "JSON"
        System.arraycopy(jsonBytes, 0, glb, 20, jsonBytes.length);
        for (int i = 0; i < pad; i++) {
            glb[20 + jsonBytes.length + i] = ' ';
        }
        return glb;
    }

    @Test
    void loadsTriangleFromGltfJson() {
        byte[] data = utf8(triangleGltfJson(triangleBuffer()));
        Mesh mesh = GltfLoader.load(new StubDevice(), data);
        assertNotNull(mesh);
        assertEquals(PrimitiveType.TRIANGLES, mesh.getPrimitiveType());
        assertTrue(mesh.isIndexed());

        VertexBuffer vb = mesh.getVertices();
        assertEquals(VertexFormat.POSITION_NORMAL_TEXCOORD, vb.getFormat());
        assertEquals(3, vb.getVertexCount());

        // pos(3)+normal(3)+uv(2) = 8 floats per vertex.
        float[] verts = vb.getData();
        // Vertex 1 position is (1,0,0).
        assertEquals(1f, verts[8], 0f);
        assertEquals(0f, verts[9], 0f);
        assertEquals(0f, verts[10], 0f);
        // Vertex 2 position is (0,1,0).
        assertEquals(0f, verts[16], 0f);
        assertEquals(1f, verts[17], 0f);

        // Indices preserved.
        assertEquals(3, mesh.getIndices().getIndexCount());
        short[] idx = mesh.getIndices().getData();
        assertEquals(0, idx[0]);
        assertEquals(1, idx[1]);
        assertEquals(2, idx[2]);
    }

    @Test
    void computesFlatNormalsWhenAbsent() {
        // The triangle lies in the z=0 plane; CCW winding gives a +Z normal.
        Mesh mesh = GltfLoader.load(new StubDevice(), utf8(triangleGltfJson(triangleBuffer())));
        float[] verts = mesh.getVertices().getData();
        // Normal of vertex 0 occupies floats 3,4,5.
        assertEquals(0f, verts[3], 1e-6f);
        assertEquals(0f, verts[4], 1e-6f);
        assertEquals(1f, verts[5], 1e-6f);
    }

    @Test
    void loadsSameTriangleFromGlbBinary() {
        byte[] glb = toGlb(triangleGltfJson(triangleBuffer()));
        Mesh mesh = GltfLoader.load(new StubDevice(), glb);
        assertNotNull(mesh);
        assertEquals(3, mesh.getVertices().getVertexCount());
        assertEquals(3, mesh.getIndices().getIndexCount());
    }

    @Test
    void loadsFromInputStreamAndClosesIt() throws IOException {
        byte[] data = utf8(triangleGltfJson(triangleBuffer()));
        final boolean[] closed = {false};
        InputStream in = new ByteArrayInputStream(data) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        Mesh mesh = GltfLoader.load(new StubDevice(), in);
        assertNotNull(mesh);
        assertTrue(closed[0], "loader must close the supplied stream");
    }

    @Test
    void synthesizesSequentialIndicesWhenPrimitiveHasNone() {
        // Drop the indices accessor reference; loader should generate 0,1,2.
        byte[] buffer = triangleBuffer();
        String dataUri = "data:application/octet-stream;base64," + Base64.encodeNoNewline(buffer);
        String json = "{"
                + "\"buffers\":[{\"byteLength\":42,\"uri\":\"" + dataUri + "\"}],"
                + "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":36}],"
                + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"}],"
                + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0}}]}]"
                + "}";
        Mesh mesh = GltfLoader.load(new StubDevice(), utf8(json));
        assertEquals(3, mesh.getIndices().getIndexCount());
        short[] idx = mesh.getIndices().getData();
        assertEquals(0, idx[0]);
        assertEquals(1, idx[1]);
        assertEquals(2, idx[2]);
    }

    @Test
    void deviceFreeLoadMatchesDeviceLoad() {
        byte[] data = utf8(triangleGltfJson(triangleBuffer()));
        Mesh direct = GltfLoader.load(data);
        Mesh viaDevice = GltfLoader.load(new StubDevice(), data);
        assertEquals(viaDevice.getVertices().getVertexCount(), direct.getVertices().getVertexCount());
        assertEquals(viaDevice.getIndices().getIndexCount(), direct.getIndices().getIndexCount());
        assertArrayEquals(viaDevice.getVertices().getData(), direct.getVertices().getData());
        assertArrayEquals(viaDevice.getIndices().getData(), direct.getIndices().getData());
    }

    @Test
    void deviceFreeStreamLoadClosesTheStream() throws IOException {
        byte[] data = utf8(triangleGltfJson(triangleBuffer()));
        final boolean[] closed = {false};
        InputStream in = new ByteArrayInputStream(data) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        Mesh mesh = GltfLoader.load(in);
        assertNotNull(mesh);
        assertTrue(closed[0], "loader must close the supplied stream");
    }

    @Test
    void imageModelWithoutMaterialHasMeshAndNullImage() {
        byte[] data = utf8(triangleGltfJson(triangleBuffer()));
        GltfLoader.GltfImageModel model = GltfLoader.loadImageModel(data);
        assertNotNull(model.getMesh());
        assertEquals(3, model.getMesh().getVertices().getVertexCount());
        assertNull(model.getBaseColorImage());
    }

    @Test
    void rejectsEmptyOrTooShortData() {
        StubDevice d = new StubDevice();
        assertThrows(IllegalArgumentException.class, () -> GltfLoader.load(d, new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> GltfLoader.load(d, (byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> GltfLoader.load(d, new byte[]{1, 2, 3}));
    }

    @Test
    void rejectsModelWithNoMeshes() {
        String json = "{\"asset\":{\"version\":\"2.0\"},\"meshes\":[]}";
        assertThrows(IllegalArgumentException.class,
                () -> GltfLoader.load(new StubDevice(), utf8(json)));
    }

    @Test
    void rejectsPrimitiveWithoutPosition() {
        String json = "{\"meshes\":[{\"primitives\":[{\"attributes\":{}}]}]}";
        assertThrows(IllegalArgumentException.class,
                () -> GltfLoader.load(new StubDevice(), utf8(json)));
    }

    @Test
    void rejectsUnsupportedPrimitiveMode() {
        // Mode 0 (POINTS) is not TRIANGLES.
        String json = "{\"meshes\":[{\"primitives\":[{"
                + "\"attributes\":{\"POSITION\":0},\"mode\":0}]}]}";
        assertThrows(IllegalArgumentException.class,
                () -> GltfLoader.load(new StubDevice(), utf8(json)));
    }

    @Test
    void rejectsExternalBufferUri() {
        // A non-data: URI must be refused; loader does not fetch side files.
        String json = "{"
                + "\"buffers\":[{\"byteLength\":36,\"uri\":\"buffer.bin\"}],"
                + "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":36}],"
                + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"}],"
                + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0}}]}]"
                + "}";
        assertThrows(IllegalArgumentException.class,
                () -> GltfLoader.load(new StubDevice(), utf8(json)));
    }

    @Test
    void glbWithoutJsonChunkIsRejected() {
        // Valid magic + header but no JSON chunk payload.
        byte[] glb = new byte[12];
        putUIntLE(glb, 0, 0x46546C67);
        putUIntLE(glb, 4, 2);
        putUIntLE(glb, 8, 12);
        assertThrows(IllegalArgumentException.class,
                () -> GltfLoader.load(new StubDevice(), glb));
    }
}
