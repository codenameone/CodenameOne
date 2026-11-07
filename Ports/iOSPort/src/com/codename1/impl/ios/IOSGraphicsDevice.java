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
package com.codename1.impl.ios;

import com.codename1.gpu.Camera;
import com.codename1.gpu.Environment;
import com.codename1.gpu.GpuCapabilities;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.PostProcess;
import com.codename1.gpu.PrimitiveType;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import com.codename1.impl.gpu.GlslShaderGenerator;
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
            8192, 16, true, true, true, "Codename One Metal (iOS)", true, true);

    // Scratch matrices reused every draw to avoid per-frame allocation.
    private final float[] mvp = new float[16];

    // SIMD aligned uniform block handed straight to Metal. Layout must match the
    // CN1Uniforms struct emitted by IOSMetalShaderGenerator and copied on the
    // native side: 3 mat4 (48 floats) + 5 vec4 (20 floats) + shininess + pad to 72, then 9 vec4 of
    // environment, PBR and sky values (see packEnvironment), then the shadow
    // matrix (16 floats) and a vec4 of bias, strength and texel size.
    // We pad to a multiple of 16 for the aligned allocator.
    private static final int UNIFORM_FLOATS = 128;

    // Shadow map state: the light's view-projection (also read by receivers
    // through the uniform block), and whether a map has been rendered yet.
    private final float[] shadowMatrix = new float[16];
    private int shadowSize = 1;
    private boolean inShadowPass;
    private boolean shadowReady;
    private PostProcess activePost;
    private String postSource;
    private final float[] postParams = new float[16];
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
        if (contextPeer == 0) {
            return;
        }
        if (inShadowPass) {
            drawShadowCaster(mesh, material, modelMatrix);
            return;
        }
        PrimitiveType type = mesh.getPrimitiveType();

        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();

        long vboHandle = uploadVertexBuffer(vb);
        if (vboHandle == 0) {
            return;
        }
        boolean shadowed = shadowReady && isShadowMapCurrent() && getLight().isShadows() && material.isLit()
                && material.isReceiveShadows();
        long pipeline = getOrCreatePipeline(material, fmt, getEnvironment(), shadowed);
        if (pipeline == 0) {
            return;
        }

        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        packUniforms(material, model);
        if (shadowed) {
            packShadow(getLight());
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

    private long getOrCreatePipeline(Material material, VertexFormat fmt, Environment env,
            boolean shadowed) {
        RenderState rs = material.getRenderState();
        String key = material.getShaderKey() + "|" + GlslShaderGenerator.variantKey(env, shadowed)
                + "|s" + fmt.getStrideBytes()
                + "|b" + blendCode(rs.getBlendMode())
                + "|c" + cullCode(rs.getCullMode())
                + "|dt" + (rs.isDepthTest() ? 1 : 0)
                + "|dw" + (rs.isDepthWrite() ? 1 : 0);
        Long existing = pipelines.get(key);
        if (existing != null) {
            return existing.longValue();
        }
        IOSMetalShaderGenerator gen = new IOSMetalShaderGenerator(material, fmt, env, shadowed);
        String src = gen.getSource();
        long pipeline = IOSImplementation.nativeInstance.gl3dGetOrCreatePipeline(
                contextPeer, key, src,
                blendCode(rs.getBlendMode()), cullCode(rs.getCullMode()),
                rs.isDepthTest() ? 1 : 0, rs.isDepthWrite() ? 1 : 0);
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
        o = 72;
        packEnvironment(material, o);
        // a draw that does not receive shadows must not see the previous one's
        for (int i = 108; i < UNIFORM_FLOATS; i++) {
            uniforms[i] = 0.0f;
        }
    }

    private void packShadow(Light light) {
        for (int i = 0; i < 16; i++) {
            uniforms[108 + i] = shadowMatrix[i];
        }
        uniforms[124] = light.getShadowBias();
        uniforms[125] = light.getShadowStrength();
        uniforms[126] = 1.0f / Math.max(1, shadowSize);
        uniforms[127] = 0.0f;
    }

    public boolean beginShadowPass(Light light) {
        if (contextPeer == 0 || light == null || inShadowPass) {
            return false;
        }
        int size = light.getShadowMapSize();
        if (!IOSImplementation.nativeInstance.gl3dBeginShadowPass(contextPeer, size)) {
            return false;
        }
        shadowSize = size;
        light.computeShadowMatrix(shadowMatrix);
        inShadowPass = true;
        return true;
    }

    public void endShadowPass() {
        if (!inShadowPass) {
            return;
        }
        inShadowPass = false;
        shadowReady = true;
        shadowMapRendered();
        IOSImplementation.nativeInstance.gl3dEndShadowPass(contextPeer);
    }

    // Depth only draw from the light. The pipeline key's "shadow|" prefix is
    // what tells the native side to build it without a colour attachment.
    private void drawShadowCaster(Mesh mesh, Material material, float[] modelMatrix) {
        if (!material.isCastShadows() || material.getRenderState().getBlendMode() != RenderState.BlendMode.NONE
                || material.getType() == Material.Type.SKYBOX) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        long vboHandle = uploadVertexBuffer(vb);
        if (vboHandle == 0) {
            return;
        }
        String key = "shadow|s" + fmt.getStrideBytes() + "|b0|c0|dt1|dw1";
        Long existing = pipelines.get(key);
        long pipeline;
        if (existing != null) {
            pipeline = existing.longValue();
        } else {
            pipeline = IOSImplementation.nativeInstance.gl3dGetOrCreatePipeline(
                    contextPeer, key, IOSMetalShaderGenerator.shadowSource(fmt), 0, 0, 1, 1);
            pipelines.put(key, Long.valueOf(pipeline));
        }
        if (pipeline == 0) {
            return;
        }
        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        Matrix4.multiply(shadowMatrix, model, mvp);
        for (int i = 0; i < 16; i++) {
            uniforms[i] = mvp[i];
        }
        int primitive = primitiveCode(mesh.getPrimitiveType());
        int strideBytes = fmt.getStrideBytes();
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            long iboHandle = uploadIndexBuffer(ib);
            if (iboHandle == 0) {
                return;
            }
            IOSImplementation.nativeInstance.gl3dDrawIndexed(
                    contextPeer, pipeline, vboHandle, strideBytes, iboHandle,
                    ib.getIndexCount(), primitive, uniforms, UNIFORM_FLOATS, 0, 0, 0);
        } else {
            IOSImplementation.nativeInstance.gl3dDrawArrays(
                    contextPeer, pipeline, vboHandle, strideBytes,
                    vb.getVertexCount(), primitive, uniforms, UNIFORM_FLOATS, 0, 0, 0);
        }
    }

    public boolean beginPostProcess(PostProcess effects, int width, int height) {
        if (contextPeer == 0 || effects == null || activePost != null) {
            return false;
        }
        if (postSource == null) {
            postSource = IOSMetalShaderGenerator.postSource();
        }
        if (!IOSImplementation.nativeInstance.gl3dBeginPostProcess(contextPeer, postSource,
                effects.isBloom())) {
            return false;
        }
        activePost = effects;
        return true;
    }

    public void endPostProcess(float timeSeconds) {
        PostProcess pp = activePost;
        if (pp == null) {
            return;
        }
        activePost = null;
        float[] p = postParams;
        p[0] = pp.getBloomThreshold();
        p[1] = pp.getBloomIntensity();
        p[2] = pp.getContrast();
        p[3] = pp.getSaturation();
        int lift = pp.getShadowTint();
        p[4] = ((lift >> 16) & 0xff) / 255.0f;
        p[5] = ((lift >> 8) & 0xff) / 255.0f;
        p[6] = (lift & 0xff) / 255.0f;
        p[7] = 0.0f;
        int gain = pp.getHighlightTint();
        p[8] = ((gain >> 16) & 0xff) / 255.0f;
        p[9] = ((gain >> 8) & 0xff) / 255.0f;
        p[10] = (gain & 0xff) / 255.0f;
        p[11] = 0.0f;
        p[12] = pp.getVignette();
        p[13] = pp.getGrain();
        p[14] = timeSeconds;
        p[15] = pp.isBloom() ? 1.0f : 0.0f;
        IOSImplementation.nativeInstance.gl3dEndPostProcess(contextPeer, p);
    }

    private void rgb4(int o, int c, float w) {
        uniforms[o] = ((c >> 16) & 0xff) / 255.0f;
        uniforms[o + 1] = ((c >> 8) & 0xff) / 255.0f;
        uniforms[o + 2] = (c & 0xff) / 255.0f;
        uniforms[o + 3] = w;
    }

    // Environment block, in the order of the trailing CN1Uniforms float4s.
    private void packEnvironment(Material material, int o) {
        Environment env = getEnvironment();
        if (env == null) {
            env = new Environment();
        }
        rgb4(o, env.getFogColor(), env.getExposure());
        uniforms[o + 4] = env.getFogDensity();
        uniforms[o + 5] = env.getFogHeightFalloff();
        uniforms[o + 6] = env.getFogBaseHeight();
        uniforms[o + 7] = 0.0f;
        rgb4(o + 8, env.getSkyColor(), 1.0f);
        rgb4(o + 12, env.getGroundColor(), 1.0f);
        uniforms[o + 16] = material.getMetallic();
        uniforms[o + 17] = material.getRoughness();
        uniforms[o + 18] = 0.0f;
        uniforms[o + 19] = 0.0f;
        rgb4(o + 20, material.getEmissive(), 1.0f);
        rgb4(o + 24, env.getZenithColor(), 1.0f);
        rgb4(o + 28, env.getHorizonColor(), 1.0f);
        rgb4(o + 32, env.getSunColor(), env.getCloudCover());
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
