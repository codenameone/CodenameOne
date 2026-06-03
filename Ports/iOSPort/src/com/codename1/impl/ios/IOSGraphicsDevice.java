/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.ios;

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

/// iOS Metal implementation of the Codename One 3D `GraphicsDevice`. The device
/// owns no GL state itself: it forwards every operation to a native Metal 3D
/// context (CN1GL3D.m) through the `IOSNative` bridge. The design leans on
/// ParparVM: vertex, index and uniform payloads are SIMD aligned Java arrays
/// (see `IOSSimd`) whose backing storage lives at a fixed, aligned C address, so
/// the native side can wrap them with `newBufferWithBytesNoCopy` (or copy them
/// cheaply) without an intermediate marshalling step.
///
/// Shader generation happens here in Java (`IOSMetalShaderGenerator`): the
/// material plus the mesh vertex format produce a Metal Shading Language source
/// string which the native context compiles once and caches as a
/// `MTLRenderPipelineState`, keyed by the material shader key, the vertex format
/// and the render state.
class IOSGraphicsDevice extends GraphicsDevice {
    /// Opaque handle to the native Metal 3D context (CN1GL3D pointer cast to a
    /// long). Zero before the context is created or after disposal.
    private long contextPeer;

    // Pipeline state objects keyed by a stable string derived from the material
    // shader key, the vertex stride and the render state. Holds the native
    // MTLRenderPipelineState pointers so we generate and compile each variant
    // exactly once.
    private final Map<String, Long> pipelines = new HashMap<String, Long>();

    private final GpuCapabilities caps = new GpuCapabilities(
            8192, 16, true, true, true, "Codename One Metal (iOS)");

    // Scratch matrices reused every draw to avoid per-frame allocation.
    private final float[] mvp = new float[16];

    // SIMD aligned uniform block handed straight to Metal. Layout must match the
    // CN1Uniforms struct emitted by IOSMetalShaderGenerator and copied on the
    // native side: 4 mat4 (64 floats) + 4 vec4 (16 floats) + shininess + pad.
    // We pad to a multiple of 16 for the aligned allocator.
    private static int DRAW_LOG_COUNT = 0;
    private static final int UNIFORM_FLOATS = 96;
    private final float[] uniforms = allocAligned(UNIFORM_FLOATS);

    IOSGraphicsDevice(long contextPeer) {
        this.contextPeer = contextPeer;
    }

    private static float[] allocAligned(int size) {
        try {
            return new IOSSimd().allocFloat(size < 16 ? 16 : size);
        } catch (Throwable t) {
            return new float[size < 16 ? 16 : size];
        }
    }

    long getContextPeer() {
        return contextPeer;
    }

    public GpuCapabilities getCapabilities() {
        return caps;
    }

    public Texture createTexture(Image image) {
        int w = image.getWidth();
        int h = image.getHeight();
        return createTexture(w, h, image.getRGB());
    }

    public Texture createTexture(int width, int height, int[] argb) {
        Texture t = new Texture(width, height);
        long handle = IOSImplementation.nativeInstance.gl3dCreateTexture(argb, width, height);
        t.setHandle(Long.valueOf(handle));
        return t;
    }

    public void clear(int argbColor, boolean color, boolean depth) {
        if (contextPeer == 0) {
            return;
        }
        IOSImplementation.nativeInstance.gl3dClear(contextPeer, argbColor, color, depth);
    }

    public void setViewport(int x, int y, int width, int height) {
        if (contextPeer != 0) {
            IOSImplementation.nativeInstance.gl3dSetViewport(contextPeer, x, y, width, height);
        }
    }

    public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        boolean log = DRAW_LOG_COUNT < 3;
        if (log) {
            DRAW_LOG_COUNT++;
            System.out.println("CN1SS:GL3D:draw enter ctx=" + contextPeer
                    + " mesh=" + (mesh != null) + " material=" + (material != null)
                    + " model=" + (modelMatrix != null) + " cam=" + (getCamera() != null)
                    + " light=" + (getLight() != null));
        }
        if (contextPeer == 0) {
            return;
        }
        PrimitiveType type = mesh.getPrimitiveType();

        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        if (log) {
            System.out.println("CN1SS:GL3D:draw step1 type=" + type + " vb=" + (vb != null)
                    + " fmt=" + (fmt != null) + " data=" + (vb != null && vb.getData() != null)
                    + " rs=" + (material.getRenderState() != null));
        }

        long vboHandle = uploadVertexBuffer(vb);
        if (log) {
            System.out.println("CN1SS:GL3D:draw step2 vbo=" + vboHandle);
        }
        long pipeline = vboHandle == 0 ? 0 : getOrCreatePipeline(material, fmt);
        if (log) {
            System.out.println("CN1SS:GL3D:draw step3 pipeline=" + pipeline
                    + " indexed=" + mesh.isIndexed() + " stride=" + fmt.getStrideBytes());
        }
        if (vboHandle == 0) {
            return;
        }
        if (pipeline == 0) {
            return;
        }

        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        packUniforms(material, model);
        if (log) {
            System.out.println("CN1SS:GL3D:draw step4 packed uniforms ok");
        }

        long texHandle = 0;
        int texFilter = 0;
        int texWrap = 0;
        Texture tex = material.getTexture();
        if (tex != null && tex.getHandle() instanceof Long) {
            texHandle = ((Long) tex.getHandle()).longValue();
            texFilter = tex.getFilter() == Texture.Filter.LINEAR ? 1 : 0;
            texWrap = tex.getWrap() == Texture.Wrap.REPEAT ? 1 : 0;
        }

        int primitive = primitiveCode(type);
        int strideBytes = fmt.getStrideBytes();

        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            long iboHandle = uploadIndexBuffer(ib);
            if (iboHandle == 0) {
                return;
            }
            IOSImplementation.nativeInstance.gl3dDrawIndexed(
                    contextPeer, pipeline, vboHandle, strideBytes, iboHandle,
                    ib.getIndexCount(), primitive, uniforms, UNIFORM_FLOATS,
                    texHandle, texFilter, texWrap);
        } else {
            IOSImplementation.nativeInstance.gl3dDrawArrays(
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
                peer = IOSImplementation.nativeInstance.gl3dCreateFloatBuffer(data, floats);
                vb.setHandle(Long.valueOf(peer));
            } else {
                IOSImplementation.nativeInstance.gl3dUpdateFloatBuffer(peer, data, floats);
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
                peer = IOSImplementation.nativeInstance.gl3dCreateShortBuffer(data, count);
                ib.setHandle(Long.valueOf(peer));
            } else {
                IOSImplementation.nativeInstance.gl3dUpdateShortBuffer(peer, data, count);
            }
            ib.clearDirty();
        }
        return peer;
    }

    private long getOrCreatePipeline(Material material, VertexFormat fmt) {
        boolean log = DRAW_LOG_COUNT <= 3;
        RenderState rs = material.getRenderState();
        if (log) {
            System.out.println("CN1SS:GL3D:pipe a rs=" + (rs != null) + " ni="
                    + (IOSImplementation.nativeInstance != null) + " pipes=" + (pipelines != null));
        }
        String key = material.getShaderKey()
                + "|s" + fmt.getStrideBytes()
                + "|b" + blendCode(rs.getBlendMode())
                + "|c" + cullCode(rs.getCullMode())
                + "|dt" + (rs.isDepthTest() ? 1 : 0)
                + "|dw" + (rs.isDepthWrite() ? 1 : 0);
        if (log) {
            System.out.println("CN1SS:GL3D:pipe b key=" + key);
        }
        Long existing = pipelines.get(key);
        if (existing != null) {
            return existing.longValue();
        }
        IOSMetalShaderGenerator gen = new IOSMetalShaderGenerator(material, fmt);
        String src = gen.getSource();
        if (log) {
            System.out.println("CN1SS:GL3D:pipe c srcLen=" + (src == null ? -1 : src.length()));
        }
        long pipeline = IOSImplementation.nativeInstance.gl3dGetOrCreatePipeline(
                contextPeer, key, src,
                blendCode(rs.getBlendMode()), cullCode(rs.getCullMode()),
                rs.isDepthTest() ? 1 : 0, rs.isDepthWrite() ? 1 : 0);
        if (log) {
            System.out.println("CN1SS:GL3D:pipe d pipeline=" + pipeline);
        }
        pipelines.put(key, Long.valueOf(pipeline));
        return pipeline;
    }

    // Packs the per-draw uniform block into the SIMD aligned float array. The
    // ordering matches the CN1Uniforms struct in the generated MSL.
    private void packUniforms(Material material, float[] model) {
        Camera cam = getCamera();
        float[] vp = cam != null ? cam.getViewProjection() : Matrix4.identity();
        Matrix4.multiply(vp, model, mvp);
        float[] nm = Matrix4.normalMatrix(model);

        int o = 0;
        // mvp (16)
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = mvp[i];
        }
        // model (16)
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = model[i];
        }
        // normalMatrix (16)
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = nm[i];
        }
        // color vec4 (rgba)
        int mc = material.getColor();
        uniforms[o++] = ((mc >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((mc >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (mc & 0xff) / 255.0f;
        uniforms[o++] = ((mc >>> 24) & 0xff) / 255.0f;

        Light light = getLight();
        // lightDir vec4
        uniforms[o++] = light.getDirectionX();
        uniforms[o++] = light.getDirectionY();
        uniforms[o++] = light.getDirectionZ();
        uniforms[o++] = 0.0f;
        // lightColor vec4
        int lc = light.getColor();
        uniforms[o++] = ((lc >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((lc >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (lc & 0xff) / 255.0f;
        uniforms[o++] = 1.0f;
        // ambient vec4
        int ac = light.getAmbientColor();
        uniforms[o++] = ((ac >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((ac >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (ac & 0xff) / 255.0f;
        uniforms[o++] = 1.0f;
        // eye vec4
        uniforms[o++] = cam != null ? cam.getEyeX() : 0.0f;
        uniforms[o++] = cam != null ? cam.getEyeY() : 0.0f;
        uniforms[o++] = cam != null ? cam.getEyeZ() : 0.0f;
        uniforms[o++] = 1.0f;
        // shininess (1) + pad to keep the layout stable
        uniforms[o++] = material.getShininess();
        // remaining floats are padding for alignment; leave as-is
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
            IOSImplementation.nativeInstance.gl3dDisposeBuffer(((Long) handle).longValue());
        }
        buffer.setHandle(null);
    }

    public void dispose(IndexBuffer buffer) {
        Object handle = buffer.getHandle();
        if (handle instanceof Long) {
            IOSImplementation.nativeInstance.gl3dDisposeBuffer(((Long) handle).longValue());
        }
        buffer.setHandle(null);
    }

    public void dispose(Texture texture) {
        Object handle = texture.getHandle();
        if (handle instanceof Long) {
            IOSImplementation.nativeInstance.gl3dDisposeTexture(((Long) handle).longValue());
        }
        texture.setHandle(null);
    }

    /// Releases the native context and all cached pipelines. Called when the
    /// hosting peer is torn down.
    void destroy() {
        if (contextPeer != 0) {
            for (Long p : pipelines.values()) {
                if (p != null && p.longValue() != 0) {
                    IOSImplementation.nativeInstance.gl3dDisposePipeline(p.longValue());
                }
            }
            pipelines.clear();
            IOSImplementation.nativeInstance.gl3dDestroyContext(contextPeer);
            contextPeer = 0;
        }
    }
}
