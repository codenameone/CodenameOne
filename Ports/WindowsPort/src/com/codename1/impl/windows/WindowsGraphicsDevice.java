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
package com.codename1.impl.windows;

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
            8192, 16, true, true, true, "Codename One Direct3D 11 (Windows)", true, true);

    private final float[] mvp = new float[16];

    // Uniform block layout must match the CN1Uniforms cbuffer emitted by
    // HlslShaderGenerator and copied on the native side: 3 mat4 (48) + 6 vec4
    // (24: color, light dir, light colour, ambient, eye, params) + 7 vec4 (28:
    // fog colour, fog, sky, ground, emissive, zenith, horizon) + sun (4).
    // + shadow matrix (16) + shadow params (4, bias / strength / texel size).
    private static final int UNIFORM_FLOATS = 124;
    private final float[] uniforms = new float[UNIFORM_FLOATS];

    // Shadow state, as in JavaSEGLDevice: the light matrix of the last shadow
    // pass, whether a pass is running, and whether its map may be sampled.
    private final float[] shadowMatrix = new float[16];
    private final float[] model = new float[16];
    private boolean inShadowPass;
    private boolean shadowReady;
    private boolean shadowSampling;
    private int shadowSize = 1024;
    private PostProcess activePost;
    private long postBright;
    private long postBlur;
    private long postComposite;
    private final float[] postParams = new float[16];

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
        if (inShadowPass) {
            drawShadowCaster(mesh, material, modelMatrix);
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        boolean shadowed = shadowReady && isShadowMapCurrent() && getLight().isShadows() && material.isLit()
                && material.isReceiveShadows();
        if (shadowed != shadowSampling) {
            shadowSampling = shadowed;
            WindowsNative.gl3dSetShadowSampling(contextPeer, shadowed);
        }

        long vboHandle = uploadVertexBuffer(vb);
        if (vboHandle == 0) {
            return;
        }
        long pipeline = getOrCreatePipeline(material, fmt, shadowed);
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

    private long getOrCreatePipeline(Material material, VertexFormat fmt, boolean shadowed) {
        RenderState rs = material.getRenderState();
        Environment env = getEnvironment();
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
        HlslShaderGenerator gen = new HlslShaderGenerator(material, fmt, env, shadowed);
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
        Environment env = getEnvironment();
        uniforms[o++] = material.getShininess();
        uniforms[o++] = material.getMetallic();
        uniforms[o++] = material.getRoughness();
        uniforms[o++] = env.getExposure();
        o = packColor(o, env.getFogColor());
        uniforms[o++] = env.getFogDensity();
        uniforms[o++] = env.getFogHeightFalloff();
        uniforms[o++] = env.getFogBaseHeight();
        uniforms[o++] = 0.0f;
        o = packColor(o, env.getSkyColor());
        o = packColor(o, env.getGroundColor());
        o = packColor(o, material.getEmissive());
        o = packColor(o, env.getZenithColor());
        o = packColor(o, env.getHorizonColor());
        int sun = env.getSunColor();
        uniforms[o++] = ((sun >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((sun >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (sun & 0xff) / 255.0f;
        uniforms[o++] = env.getCloudCover();
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = shadowMatrix[i];
        }
        Light sl = getLight();
        uniforms[o++] = sl.getShadowBias();
        uniforms[o++] = sl.getShadowStrength();
        uniforms[o++] = 1.0f / Math.max(1, shadowSize);
        uniforms[o++] = 0.0f;
    }

    public boolean beginShadowPass(Light light) {
        if (contextPeer == 0) {
            return false;
        }
        int size = light.getShadowMapSize();
        if (!WindowsNative.gl3dBeginShadowPass(contextPeer, size)) {
            return false;
        }
        shadowSize = size;
        shadowSampling = false;
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
        WindowsNative.gl3dEndShadowPass(contextPeer);
    }

    // Renders one caster's depth into the shadow map: only positions are read,
    // the first uniform matrix is shadowMatrix * model.
    private void drawShadowCaster(Mesh mesh, Material material, float[] modelMatrix) {
        if (!material.isCastShadows() || material.getRenderState().getBlendMode() != RenderState.BlendMode.NONE
                || material.getType() == Material.Type.SKYBOX) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        long vboHandle = uploadVertexBuffer(vb);
        long pipeline = getOrCreateAuxPipeline("cn1-shadow", HlslShaderGenerator.shadowSource(), 0, 1);
        if (vboHandle == 0 || pipeline == 0) {
            return;
        }
        if (modelMatrix != null) {
            Matrix4.copy(modelMatrix, model);
        } else {
            Matrix4.setIdentity(model);
        }
        Matrix4.multiply(shadowMatrix, model, mvp);
        for (int i = 0; i < 16; i++) {
            uniforms[i] = mvp[i];
        }
        int primitive = primitiveCode(mesh.getPrimitiveType());
        int strideBytes = vb.getFormat().getStrideBytes();
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            long iboHandle = uploadIndexBuffer(ib);
            if (iboHandle != 0) {
                WindowsNative.gl3dDrawIndexed(contextPeer, pipeline, vboHandle, strideBytes, iboHandle,
                        ib.getIndexCount(), primitive, uniforms, UNIFORM_FLOATS, 0, 0, 0);
            }
        } else {
            WindowsNative.gl3dDrawArrays(contextPeer, pipeline, vboHandle, strideBytes,
                    vb.getVertexCount(), primitive, uniforms, UNIFORM_FLOATS, 0, 0, 0);
        }
    }

    // A pipeline for the shadow and post passes: no blending, no culling, depth
    // test and write as given.
    private long getOrCreateAuxPipeline(String key, String source, int depthTest, int depthWrite) {
        Long existing = pipelines.get(key);
        if (existing != null) {
            return existing.longValue();
        }
        long pipeline = WindowsNative.gl3dGetOrCreatePipeline(contextPeer, key, source,
                0, 0, depthTest, depthWrite);
        pipelines.put(key, Long.valueOf(pipeline));
        return pipeline;
    }

    public boolean beginPostProcess(PostProcess effects, int width, int height) {
        if (contextPeer == 0 || effects == null) {
            return false;
        }
        postBright = getOrCreateAuxPipeline("cn1-post-bright", HlslShaderGenerator.postBrightSource(), 0, 0);
        postBlur = getOrCreateAuxPipeline("cn1-post-blur", HlslShaderGenerator.postBlurSource(), 0, 0);
        postComposite = getOrCreateAuxPipeline("cn1-post-composite",
                HlslShaderGenerator.postCompositeSource(), 0, 0);
        if (postBright == 0 || postBlur == 0 || postComposite == 0
                || !WindowsNative.gl3dBeginPost(contextPeer, effects.isBloom())) {
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
        p[1] = pp.isBloom() ? pp.getBloomIntensity() : 0f;
        p[2] = pp.getContrast();
        p[3] = pp.getSaturation();
        int lift = pp.getShadowTint();
        p[4] = ((lift >> 16) & 0xff) / 255.0f;
        p[5] = ((lift >> 8) & 0xff) / 255.0f;
        p[6] = (lift & 0xff) / 255.0f;
        p[7] = pp.getVignette();
        int gain = pp.getHighlightTint();
        p[8] = ((gain >> 16) & 0xff) / 255.0f;
        p[9] = ((gain >> 8) & 0xff) / 255.0f;
        p[10] = (gain & 0xff) / 255.0f;
        p[11] = pp.getGrain();
        p[12] = timeSeconds;
        p[13] = 0f;
        p[14] = 0f;
        p[15] = pp.isBloom() ? 1f : 0f;
        WindowsNative.gl3dEndPost(contextPeer, postBright, postBlur, postComposite, p);
        shadowSampling = false;
    }

    // Writes an RGB colour as a float4 with a zero fourth component.
    private int packColor(int o, int argb) {
        uniforms[o++] = ((argb >> 16) & 0xff) / 255.0f;
        uniforms[o++] = ((argb >> 8) & 0xff) / 255.0f;
        uniforms[o++] = (argb & 0xff) / 255.0f;
        uniforms[o++] = 0.0f;
        return o;
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
