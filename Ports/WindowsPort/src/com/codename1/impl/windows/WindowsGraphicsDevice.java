/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.windows;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GpuCapabilities;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import com.codename1.ui.Image;

import java.util.HashMap;
import java.util.Map;

/// Direct3D 11 implementation of the Codename One 3D `GraphicsDevice` for the
/// native Windows port. Like the iOS Metal device, it owns no GPU state itself:
/// it forwards every operation to a native D3D11 context (cn1_windows_d3d.cpp)
/// through the `WindowsNative` bridge, passing vertex/index/uniform payloads as
/// plain Java arrays that the native side copies into D3D buffers.
///
/// Shaders are generated here in Java (`HlslShaderGenerator`): the material plus
/// the mesh vertex format produce an HLSL source string the native context
/// compiles once (D3DCompile) and caches as a pipeline keyed by the material
/// shader key, the vertex stride and the render state.
class WindowsGraphicsDevice extends GraphicsDevice {
    /// Opaque handle to the native D3D11 context. Zero before creation / after
    /// disposal.
    private long contextPeer;

    private final Map<String, Long> pipelines = new HashMap<String, Long>();

    private final GpuCapabilities caps = new GpuCapabilities(
            8192, 16, true, true, true, "Codename One Direct3D 11 (Windows)");

    private final float[] mvp = new float[16];

    // Uniform block layout must match the CN1Uniforms cbuffer emitted by
    // HlslShaderGenerator and copied on the native side: 3 mat4 (48) + 5 vec4
    // (20) + shininess (1), padded to a multiple of 4 floats (16 bytes).
    private static final int UNIFORM_FLOATS = 72;
    private final float[] uniforms = new float[UNIFORM_FLOATS];

    WindowsGraphicsDevice(long contextPeer) {
        this.contextPeer = contextPeer;
    }

    long getContextPeer() {
        return contextPeer;
    }

    public GpuCapabilities getCapabilities() {
        return caps;
    }

    public Texture createTexture(Image image) {
        return createTexture(image.getWidth(), image.getHeight(), image.getRGB());
    }

    public Texture createTexture(int width, int height, int[] argb) {
        Texture t = new Texture(width, height);
        long handle = WindowsNative.gl3dCreateTexture(argb, width, height);
        t.setHandle(Long.valueOf(handle));
        return t;
    }

    public void clear(int argbColor, boolean color, boolean depth) {
        if (contextPeer != 0) {
            WindowsNative.gl3dClear(contextPeer, argbColor, color, depth);
        }
    }

    public void setViewport(int x, int y, int width, int height) {
        if (contextPeer != 0) {
            WindowsNative.gl3dSetViewport(contextPeer, x, y, width, height);
        }
    }

    public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        if (contextPeer == 0) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();

        long vboHandle = uploadVertexBuffer(vb);
        if (vboHandle == 0) {
            return;
        }
        long pipeline = getOrCreatePipeline(material, fmt);
        if (pipeline == 0) {
            return;
        }

        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        packUniforms(material, model);

        long texHandle = 0;
        int texFilter = 0;
        int texWrap = 0;
        Texture tex = material.getTexture();
        if (tex != null && tex.getHandle() instanceof Long) {
            texHandle = ((Long) tex.getHandle()).longValue();
            texFilter = tex.getFilter() == Texture.Filter.LINEAR ? 1 : 0;
            texWrap = tex.getWrap() == Texture.Wrap.REPEAT ? 1 : 0;
        }

        int primitive = primitiveCode(mesh.getPrimitiveType());
        int strideBytes = fmt.getStrideBytes();

        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            long iboHandle = uploadIndexBuffer(ib);
            if (iboHandle == 0) {
                return;
            }
            WindowsNative.gl3dDrawIndexed(
                    contextPeer, pipeline, vboHandle, strideBytes, iboHandle,
                    ib.getIndexCount(), primitive, uniforms, UNIFORM_FLOATS,
                    texHandle, texFilter, texWrap);
        } else {
            WindowsNative.gl3dDrawArrays(
                    contextPeer, pipeline, vboHandle, strideBytes,
                    vb.getVertexCount(), primitive, uniforms, UNIFORM_FLOATS,
                    texHandle, texFilter, texWrap);
        }
    }

    private long uploadVertexBuffer(VertexBuffer vb) {
        Object handle = vb.getHandle();
        long peer = handle instanceof Long ? ((Long) handle).longValue() : 0;
        if (peer == 0 || vb.isDirty()) {
            float[] data = vb.getData();
            int floats = vb.getFloatCount();
            if (peer == 0) {
                peer = WindowsNative.gl3dCreateFloatBuffer(data, floats);
                vb.setHandle(Long.valueOf(peer));
            } else {
                WindowsNative.gl3dUpdateFloatBuffer(peer, data, floats);
            }
            vb.clearDirty();
        }
        return peer;
    }

    private long uploadIndexBuffer(IndexBuffer ib) {
        Object handle = ib.getHandle();
        long peer = handle instanceof Long ? ((Long) handle).longValue() : 0;
        if (peer == 0 || ib.isDirty()) {
            short[] data = ib.getData();
            int count = ib.getIndexCount();
            if (peer == 0) {
                peer = WindowsNative.gl3dCreateShortBuffer(data, count);
                ib.setHandle(Long.valueOf(peer));
            } else {
                WindowsNative.gl3dUpdateShortBuffer(peer, data, count);
            }
            ib.clearDirty();
        }
        return peer;
    }

    private long getOrCreatePipeline(Material material, VertexFormat fmt) {
        RenderState rs = material.getRenderState();
        String key = material.getShaderKey()
                + "|s" + fmt.getStrideBytes()
                + "|b" + blendCode(rs.getBlendMode())
                + "|c" + cullCode(rs.getCullMode())
                + "|dt" + (rs.isDepthTest() ? 1 : 0)
                + "|dw" + (rs.isDepthWrite() ? 1 : 0);
        Long existing = pipelines.get(key);
        if (existing != null) {
            return existing.longValue();
        }
        HlslShaderGenerator gen = new HlslShaderGenerator(material, fmt);
        long pipeline = WindowsNative.gl3dGetOrCreatePipeline(
                contextPeer, key, gen.getSource(),
                blendCode(rs.getBlendMode()), cullCode(rs.getCullMode()),
                rs.isDepthTest() ? 1 : 0, rs.isDepthWrite() ? 1 : 0);
        pipelines.put(key, Long.valueOf(pipeline));
        return pipeline;
    }

    // Packs the per-draw uniform block. Ordering matches the CN1Uniforms cbuffer
    // in the generated HLSL.
    private void packUniforms(Material material, float[] model) {
        Camera cam = getCamera();
        float[] vp = cam != null ? cam.getViewProjection() : Matrix4.identity();
        Matrix4.multiply(vp, model, mvp);
        float[] nm = Matrix4.normalMatrix(model);

        int o = 0;
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = mvp[i];
        }
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = model[i];
        }
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = nm[i];
        }
        int mc = material.getColor();
        uniforms[o++] = ((mc >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((mc >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (mc & 0xff) / 255.0f;
        uniforms[o++] = ((mc >>> 24) & 0xff) / 255.0f;

        Light light = getLight();
        uniforms[o++] = light.getDirectionX();
        uniforms[o++] = light.getDirectionY();
        uniforms[o++] = light.getDirectionZ();
        uniforms[o++] = 0.0f;
        int lc = light.getColor();
        uniforms[o++] = ((lc >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((lc >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (lc & 0xff) / 255.0f;
        uniforms[o++] = 1.0f;
        int ac = light.getAmbientColor();
        uniforms[o++] = ((ac >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((ac >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (ac & 0xff) / 255.0f;
        uniforms[o++] = 1.0f;
        uniforms[o++] = cam != null ? cam.getEyeX() : 0.0f;
        uniforms[o++] = cam != null ? cam.getEyeY() : 0.0f;
        uniforms[o++] = cam != null ? cam.getEyeZ() : 0.0f;
        uniforms[o++] = 1.0f;
        uniforms[o++] = material.getShininess();
    }

    private static int primitiveCode(PrimitiveType type) {
        switch (type) {
            case POINTS:
                return 0;
            case LINES:
                return 1;
            case LINE_STRIP:
                return 2;
            case TRIANGLE_STRIP:
                return 4;
            case TRIANGLES:
            default:
                return 3;
        }
    }

    private static int blendCode(RenderState.BlendMode mode) {
        switch (mode) {
            case ALPHA:
                return 1;
            case ADDITIVE:
                return 2;
            case NONE:
            default:
                return 0;
        }
    }

    private static int cullCode(RenderState.CullMode mode) {
        switch (mode) {
            case BACK:
                return 1;
            case FRONT:
                return 2;
            case NONE:
            default:
                return 0;
        }
    }

    public void dispose(VertexBuffer buffer) {
        Object handle = buffer.getHandle();
        if (handle instanceof Long) {
            WindowsNative.gl3dDisposeBuffer(((Long) handle).longValue());
        }
        buffer.setHandle(null);
    }

    public void dispose(IndexBuffer buffer) {
        Object handle = buffer.getHandle();
        if (handle instanceof Long) {
            WindowsNative.gl3dDisposeBuffer(((Long) handle).longValue());
        }
        buffer.setHandle(null);
    }

    public void dispose(Texture texture) {
        Object handle = texture.getHandle();
        if (handle instanceof Long) {
            WindowsNative.gl3dDisposeTexture(((Long) handle).longValue());
        }
        texture.setHandle(null);
    }

    /// Releases the native context and all cached pipelines.
    void destroy() {
        if (contextPeer != 0) {
            for (Long p : pipelines.values()) {
                if (p != null && p.longValue() != 0) {
                    WindowsNative.gl3dDisposePipeline(p.longValue());
                }
            }
            pipelines.clear();
            WindowsNative.gl3dDestroyContext(contextPeer);
            contextPeer = 0;
        }
    }
}
