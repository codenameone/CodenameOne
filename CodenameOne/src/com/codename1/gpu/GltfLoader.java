/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

import com.codename1.io.JSONParser;
import com.codename1.io.Util;
import com.codename1.ui.Image;
import com.codename1.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/// Loads a `Mesh` from a glTF 2.0 model so applications can render real authored
/// geometry rather than only the built in `Primitives`. Both the binary
/// container (`.glb`) and the JSON form (`.gltf`) are supported; for the JSON
/// form, buffers must be embedded as `data:` URIs (external `.bin` side files are
/// not fetched). The first triangle primitive of the first mesh is read.
///
/// The loader produces the engine's standard
/// `VertexFormat.POSITION_NORMAL_TEXCOORD` layout so the result drops straight
/// into any built in `Material`:
///
/// - `POSITION` (required) is read as the vertex position.
/// - `NORMAL` is read when present; otherwise flat per-triangle normals are
///   computed so lit materials still shade correctly.
/// - `TEXCOORD_0` is read when present; otherwise zero texture coordinates are
///   written.
///
/// Materials, textures, skinning and animation in the glTF are ignored -- this
/// is a geometry loader. Apply a `Material` (and a `Texture` loaded via the
/// device) to the returned mesh as usual.
///
/// Example:
///
/// ```java
/// byte[] glb = ...; // bytes of a .glb model
/// Mesh mesh = GltfLoader.load(device, glb);
/// Material material = new Material(Material.Type.PHONG).setTexture(texture);
/// device.draw(mesh, material, modelMatrix);
/// ```
public final class GltfLoader {
    private static final int GLB_MAGIC = 0x46546C67; // "glTF" little-endian
    private static final int CHUNK_JSON = 0x4E4F534A; // "JSON"
    private static final int CHUNK_BIN = 0x004E4942;  // "BIN\0"

    private static final int FLOAT = 5126;
    private static final int UNSIGNED_INT = 5125;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_BYTE = 5121;

    private static final int FLOATS_PER_VERTEX = 8; // pos(3) + normal(3) + uv(2)

    private GltfLoader() {
    }

    /// Reads all bytes from the stream and loads the model. The stream is closed.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that allocates the mesh buffers
    ///
    /// - `in`: a stream over `.glb` or `.gltf` bytes
    ///
    /// #### Returns
    ///
    /// the loaded mesh
    public static Mesh load(GraphicsDevice device, InputStream in) throws IOException {
        try {
            return load(device, readFully(in));
        } finally {
            Util.cleanup(in);
        }
    }

    /// Loads a model from in-memory `.glb` or `.gltf` bytes.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that allocates the mesh buffers
    ///
    /// - `data`: the raw model bytes (binary `.glb` or JSON `.gltf`)
    ///
    /// #### Returns
    ///
    /// the loaded mesh
    public static Mesh load(GraphicsDevice device, byte[] data) {
        return load(data);
    }

    /// Loads a model from in-memory `.glb` or `.gltf` bytes without a
    /// `GraphicsDevice`, allocating the buffers directly. Behaves exactly like
    /// `load(GraphicsDevice, byte[])`; useful for parsing geometry off the
    /// render thread or handing meshes to non GPU consumers such as the AR
    /// content pipeline.
    ///
    /// #### Parameters
    ///
    /// - `data`: the raw model bytes (binary `.glb` or JSON `.gltf`)
    ///
    /// #### Returns
    ///
    /// the loaded mesh
    public static Mesh load(byte[] data) {
        Object[] parsed = parse(data);
        return build((Map) parsed[0], (byte[]) parsed[1]);
    }

    /// Reads all bytes from the stream and loads the model without a
    /// `GraphicsDevice`. The stream is closed.
    ///
    /// #### Parameters
    ///
    /// - `in`: a stream over `.glb` or `.gltf` bytes
    ///
    /// #### Returns
    ///
    /// the loaded mesh
    public static Mesh load(InputStream in) throws IOException {
        try {
            return load(readFully(in));
        } finally {
            Util.cleanup(in);
        }
    }

    /// Loads a model together with its base-color texture from in-memory `.glb`
    /// or `.gltf` bytes. Use this (rather than `load`) when the model carries its
    /// own texture and you want it applied automatically.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that allocates the mesh buffers and texture
    ///
    /// - `data`: the raw model bytes
    ///
    /// #### Returns
    ///
    /// the loaded mesh plus its base-color texture (null texture if the model
    /// has none)
    public static GltfModel loadModel(GraphicsDevice device, byte[] data) {
        Object[] parsed = parse(data);
        Map root = (Map) parsed[0];
        byte[] binChunk = (byte[]) parsed[1];
        Mesh mesh = build(root, binChunk);
        Texture baseColor = loadBaseColorTexture(device, root, binChunk);
        return new GltfModel(mesh, baseColor);
    }

    /// Reads all bytes from the stream and loads the model with its base-color
    /// texture. The stream is closed.
    public static GltfModel loadModel(GraphicsDevice device, InputStream in) throws IOException {
        try {
            return loadModel(device, readFully(in));
        } finally {
            Util.cleanup(in);
        }
    }

    /// Loads a model together with its base-color image from in-memory `.glb`
    /// or `.gltf` bytes without a `GraphicsDevice`. The image is decoded but not
    /// uploaded to the GPU, so this works off the render thread and on non GPU
    /// consumers such as the AR content pipeline.
    ///
    /// #### Parameters
    ///
    /// - `data`: the raw model bytes
    ///
    /// #### Returns
    ///
    /// the loaded mesh plus its decoded base-color image (null image if the
    /// model has none)
    public static GltfImageModel loadImageModel(byte[] data) {
        Object[] parsed = parse(data);
        Map root = (Map) parsed[0];
        byte[] binChunk = (byte[]) parsed[1];
        Mesh mesh = build(root, binChunk);
        Image baseColor = readBaseColorImage(root, binChunk);
        return new GltfImageModel(mesh, baseColor);
    }

    /// Reads all bytes from the stream and loads the model with its base-color
    /// image without a `GraphicsDevice`. The stream is closed.
    public static GltfImageModel loadImageModel(InputStream in) throws IOException {
        try {
            return loadImageModel(readFully(in));
        } finally {
            Util.cleanup(in);
        }
    }

    private static Object[] parse(byte[] data) {
        if (data == null || data.length < 4) {
            throw new IllegalArgumentException("empty glTF data");
        }
        String json;
        byte[] binChunk;
        if (readUInt32(data, 0) == GLB_MAGIC) {
            // Binary glTF: 12 byte header then length-prefixed chunks.
            String[] jsonHolder = new String[1];
            binChunk = parseGlb(data, jsonHolder);
            json = jsonHolder[0];
        } else {
            json = utf8(data, 0, data.length);
            binChunk = null;
        }
        try {
            Map root = new JSONParser().parseJSON(
                    new InputStreamReader(new ByteArrayInputStream(utf8Bytes(json)), "UTF-8"));
            return new Object[] { root, binChunk };
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse glTF JSON: " + ex.getMessage(), ex);
        }
    }

    private static byte[] parseGlb(byte[] data, String[] jsonHolder) {
        long totalLength = readUInt32(data, 8);
        int pos = 12;
        byte[] bin = null;
        while (pos + 8 <= data.length && pos < totalLength) {
            long chunkLength = readUInt32(data, pos);
            int chunkType = readUInt32(data, pos + 4);
            int chunkStart = pos + 8;
            int len = (int) chunkLength;
            if (chunkStart + len > data.length) {
                len = data.length - chunkStart;
            }
            if (chunkType == CHUNK_JSON) {
                jsonHolder[0] = utf8(data, chunkStart, len);
            } else if (chunkType == CHUNK_BIN) {
                bin = new byte[len];
                System.arraycopy(data, chunkStart, bin, 0, len);
            }
            pos = chunkStart + len;
        }
        if (jsonHolder[0] == null) {
            throw new IllegalArgumentException("glb has no JSON chunk");
        }
        return bin;
    }

    private static Mesh build(Map root, byte[] binChunk) {
        List meshes = (List) root.get("meshes");
        if (meshes == null || meshes.isEmpty()) {
            throw new IllegalArgumentException("glTF has no meshes");
        }
        Map mesh = (Map) meshes.get(0);
        List primitives = (List) mesh.get("primitives");
        if (primitives == null || primitives.isEmpty()) {
            throw new IllegalArgumentException("glTF mesh has no primitives");
        }
        Map primitive = (Map) primitives.get(0);
        // Mode 4 (TRIANGLES) is the default and the only mode this loader builds.
        int mode = primitive.containsKey("mode") ? asInt(primitive.get("mode")) : 4;
        if (mode != 4) {
            throw new IllegalArgumentException("glTF primitive mode " + mode + " is not supported (TRIANGLES only)");
        }
        Map attributes = (Map) primitive.get("attributes");
        if (attributes == null || !attributes.containsKey("POSITION")) {
            throw new IllegalArgumentException("glTF primitive has no POSITION attribute");
        }

        List accessors = (List) root.get("accessors");
        List bufferViews = (List) root.get("bufferViews");
        List buffers = (List) root.get("buffers");

        float[] positions = readFloatAccessor(asInt(attributes.get("POSITION")), 3,
                accessors, bufferViews, buffers, binChunk);
        int vertexCount = positions.length / 3;
        float[] normals = attributes.containsKey("NORMAL")
                ? readFloatAccessor(asInt(attributes.get("NORMAL")), 3, accessors, bufferViews, buffers, binChunk)
                : null;
        float[] texcoords = attributes.containsKey("TEXCOORD_0")
                ? readFloatAccessor(asInt(attributes.get("TEXCOORD_0")), 2, accessors, bufferViews, buffers, binChunk)
                : null;

        int[] indices;
        if (primitive.containsKey("indices")) {
            indices = readIntAccessor(asInt(primitive.get("indices")), accessors, bufferViews, buffers, binChunk);
        } else {
            indices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                indices[i] = i;
            }
        }

        if (normals == null) {
            normals = computeFlatNormals(positions, indices);
        }

        float[] interleaved = new float[vertexCount * FLOATS_PER_VERTEX];
        for (int i = 0; i < vertexCount; i++) {
            int o = i * FLOATS_PER_VERTEX;
            interleaved[o] = positions[i * 3];
            interleaved[o + 1] = positions[i * 3 + 1];
            interleaved[o + 2] = positions[i * 3 + 2];
            interleaved[o + 3] = normals[i * 3];
            interleaved[o + 4] = normals[i * 3 + 1];
            interleaved[o + 5] = normals[i * 3 + 2];
            if (texcoords != null) {
                interleaved[o + 6] = texcoords[i * 2];
                interleaved[o + 7] = texcoords[i * 2 + 1];
            }
        }

        VertexBuffer vb = new VertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, vertexCount);
        vb.setData(interleaved);
        IndexBuffer ib = new IndexBuffer(indices.length);
        ib.setData(indices);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }

    /// Computes one flat normal per triangle and assigns it to that triangle's
    /// three vertices, so a model that ships without normals still lights up.
    private static float[] computeFlatNormals(float[] positions, int[] indices) {
        float[] normals = new float[positions.length];
        for (int t = 0; t + 2 < indices.length; t += 3) {
            int a = indices[t] * 3;
            int b = indices[t + 1] * 3;
            int c = indices[t + 2] * 3;
            float ux = positions[b] - positions[a];
            float uy = positions[b + 1] - positions[a + 1];
            float uz = positions[b + 2] - positions[a + 2];
            float vx = positions[c] - positions[a];
            float vy = positions[c + 1] - positions[a + 1];
            float vz = positions[c + 2] - positions[a + 2];
            float nx = uy * vz - uz * vy;
            float ny = uz * vx - ux * vz;
            float nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0f) {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            normals[a] = nx; normals[a + 1] = ny; normals[a + 2] = nz;
            normals[b] = nx; normals[b + 1] = ny; normals[b + 2] = nz;
            normals[c] = nx; normals[c + 1] = ny; normals[c + 2] = nz;
        }
        return normals;
    }

    private static float[] readFloatAccessor(int accessorIndex, int components, List accessors,
                                             List bufferViews, List buffers, byte[] binChunk) {
        Map accessor = (Map) accessors.get(accessorIndex);
        int count = asInt(accessor.get("count"));
        int componentType = asInt(accessor.get("componentType"));
        int accessorOffset = accessor.containsKey("byteOffset") ? asInt(accessor.get("byteOffset")) : 0;
        boolean normalized = accessor.containsKey("normalized") && Boolean.TRUE.equals(accessor.get("normalized"));
        Map view = (Map) bufferViews.get(asInt(accessor.get("bufferView")));
        byte[] buffer = resolveBuffer(view, buffers, binChunk);
        int viewOffset = view.containsKey("byteOffset") ? asInt(view.get("byteOffset")) : 0;
        int componentSize = componentSize(componentType);
        int stride = view.containsKey("byteStride") ? asInt(view.get("byteStride")) : components * componentSize;

        float[] out = new float[count * components];
        for (int i = 0; i < count; i++) {
            int base = viewOffset + accessorOffset + i * stride;
            for (int c = 0; c < components; c++) {
                int p = base + c * componentSize;
                out[i * components + c] = readComponentAsFloat(buffer, p, componentType, normalized);
            }
        }
        return out;
    }

    private static int[] readIntAccessor(int accessorIndex, List accessors, List bufferViews,
                                         List buffers, byte[] binChunk) {
        Map accessor = (Map) accessors.get(accessorIndex);
        int count = asInt(accessor.get("count"));
        int componentType = asInt(accessor.get("componentType"));
        int accessorOffset = accessor.containsKey("byteOffset") ? asInt(accessor.get("byteOffset")) : 0;
        Map view = (Map) bufferViews.get(asInt(accessor.get("bufferView")));
        byte[] buffer = resolveBuffer(view, buffers, binChunk);
        int viewOffset = view.containsKey("byteOffset") ? asInt(view.get("byteOffset")) : 0;
        int componentSize = componentSize(componentType);
        int stride = view.containsKey("byteStride") ? asInt(view.get("byteStride")) : componentSize;

        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            int p = viewOffset + accessorOffset + i * stride;
            switch (componentType) {
                case UNSIGNED_BYTE:
                    out[i] = buffer[p] & 0xff;
                    break;
                case UNSIGNED_SHORT:
                    out[i] = readUInt16(buffer, p);
                    break;
                case UNSIGNED_INT:
                    out[i] = (int) readUInt32(buffer, p);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported index componentType " + componentType);
            }
        }
        return out;
    }

    private static float readComponentAsFloat(byte[] buffer, int p, int componentType, boolean normalized) {
        switch (componentType) {
            case FLOAT:
                return Float.intBitsToFloat((int) readUInt32(buffer, p));
            case UNSIGNED_BYTE: {
                int v = buffer[p] & 0xff;
                return normalized ? v / 255f : v;
            }
            case UNSIGNED_SHORT: {
                int v = readUInt16(buffer, p);
                return normalized ? v / 65535f : v;
            }
            default:
                throw new IllegalArgumentException("Unsupported attribute componentType " + componentType);
        }
    }

    private static byte[] resolveBuffer(Map view, List buffers, byte[] binChunk) {
        int bufferIndex = asInt(view.get("buffer"));
        Map buffer = (Map) buffers.get(bufferIndex);
        Object uri = buffer.get("uri");
        if (uri == null) {
            if (binChunk == null) {
                throw new IllegalArgumentException("glTF buffer has no uri and no binary chunk");
            }
            return binChunk;
        }
        String u = uri.toString();
        int comma = u.indexOf(',');
        if (u.startsWith("data:") && comma >= 0) {
            return Base64.decode(utf8Bytes(u.substring(comma + 1)));
        }
        throw new IllegalArgumentException("External glTF buffers are not supported: " + u);
    }

    private static int componentSize(int componentType) {
        switch (componentType) {
            case UNSIGNED_BYTE:
                return 1;
            case UNSIGNED_SHORT:
                return 2;
            case FLOAT:
            case UNSIGNED_INT:
                return 4;
            default:
                throw new IllegalArgumentException("Unsupported componentType " + componentType);
        }
    }

    /// Reads the base-color texture of the first primitive's material and uploads
    /// it through the device. Returns null when the model carries no base-color
    /// texture. Only embedded images (a glTF `bufferView` or a `data:` URI) are
    /// supported; external image files are not fetched.
    private static Texture loadBaseColorTexture(GraphicsDevice device, Map root, byte[] binChunk) {
        Image img = readBaseColorImage(root, binChunk);
        if (img == null) {
            return null;
        }
        Texture result = device.createTexture(img);
        result.setFilter(Texture.Filter.LINEAR);
        return result;
    }

    /// Decodes the base-color image of the first primitive's material. Returns
    /// null when the model carries no base-color texture. Only embedded images
    /// (a glTF `bufferView` or a `data:` URI) are supported; external image
    /// files are not fetched.
    private static Image readBaseColorImage(Map root, byte[] binChunk) {
        List materials = (List) root.get("materials");
        if (materials == null || materials.isEmpty()) {
            return null;
        }
        Map primitive = (Map) ((List) ((Map) ((List) root.get("meshes")).get(0)).get("primitives")).get(0);
        int materialIndex = primitive.containsKey("material") ? asInt(primitive.get("material")) : 0;
        if (materialIndex < 0 || materialIndex >= materials.size()) {
            return null;
        }
        Map pbr = (Map) ((Map) materials.get(materialIndex)).get("pbrMetallicRoughness");
        if (pbr == null) {
            return null;
        }
        Map baseColorTexture = (Map) pbr.get("baseColorTexture");
        if (baseColorTexture == null) {
            return null;
        }
        List textures = (List) root.get("textures");
        List images = (List) root.get("images");
        if (textures == null || images == null) {
            return null;
        }
        Map texture = (Map) textures.get(asInt(baseColorTexture.get("index")));
        Map image = (Map) images.get(asInt(texture.get("source")));
        byte[] imageBytes = readImageBytes(image, root, binChunk);
        if (imageBytes == null) {
            return null;
        }
        return Image.createImage(imageBytes, 0, imageBytes.length);
    }

    private static byte[] readImageBytes(Map image, Map root, byte[] binChunk) {
        Object uri = image.get("uri");
        if (uri != null) {
            String u = uri.toString();
            int comma = u.indexOf(',');
            if (u.startsWith("data:") && comma >= 0) {
                return Base64.decode(utf8Bytes(u.substring(comma + 1)));
            }
            return null; // external image files are not fetched
        }
        if (!image.containsKey("bufferView")) {
            return null;
        }
        List bufferViews = (List) root.get("bufferViews");
        List buffers = (List) root.get("buffers");
        Map view = (Map) bufferViews.get(asInt(image.get("bufferView")));
        byte[] buffer = resolveBuffer(view, buffers, binChunk);
        int offset = view.containsKey("byteOffset") ? asInt(view.get("byteOffset")) : 0;
        int length = asInt(view.get("byteLength"));
        byte[] out = new byte[length];
        System.arraycopy(buffer, offset, out, 0, length);
        return out;
    }

    private static int asInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt(o.toString());
    }

    private static int readUInt16(byte[] b, int p) {
        return (b[p] & 0xff) | ((b[p + 1] & 0xff) << 8);
    }

    private static int readUInt32(byte[] b, int p) {
        return (b[p] & 0xff) | ((b[p + 1] & 0xff) << 8)
                | ((b[p + 2] & 0xff) << 16) | ((b[p + 3] & 0xff) << 24);
    }

    private static String utf8(byte[] b, int off, int len) {
        try {
            return new String(b, off, len, "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            // UTF-8 is required to be present on every JVM/CN1 runtime, so this
            // never happens; rethrow rather than fall back to the platform default.
            throw new RuntimeException(ex);
        }
    }

    private static byte[] utf8Bytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[] readFully(InputStream in) throws IOException {
        byte[] buf = new byte[8192];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int r;
        while ((r = in.read(buf)) >= 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    /// A loaded glTF model: its geometry plus the base-color texture extracted
    /// from the model's first material, if any. Returned by `loadModel`.
    public static final class GltfModel {
        private final Mesh mesh;
        private final Texture baseColorTexture;

        GltfModel(Mesh mesh, Texture baseColorTexture) {
            this.mesh = mesh;
            this.baseColorTexture = baseColorTexture;
        }

        /// The model geometry.
        public Mesh getMesh() {
            return mesh;
        }

        /// The base-color texture, or null when the model has none.
        public Texture getBaseColorTexture() {
            return baseColorTexture;
        }
    }

    /// A loaded glTF model in device-free form: its geometry plus the decoded
    /// base-color image extracted from the model's first material, if any.
    /// Returned by `loadImageModel`.
    public static final class GltfImageModel {
        private final Mesh mesh;
        private final Image baseColorImage;

        GltfImageModel(Mesh mesh, Image baseColorImage) {
            this.mesh = mesh;
            this.baseColorImage = baseColorImage;
        }

        /// The model geometry.
        public Mesh getMesh() {
            return mesh;
        }

        /// The decoded base-color image, or null when the model has none.
        public Image getBaseColorImage() {
            return baseColorImage;
        }
    }
}
