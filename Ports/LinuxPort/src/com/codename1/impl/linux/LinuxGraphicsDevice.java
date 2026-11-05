/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.linux;

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

/// OpenGL ES implementation of the Codename One 3D `GraphicsDevice` for the
/// native Linux port. It owns no GPU state itself: it forwards every operation
/// to a native EGL/GLES context (cn1_linux_gl.c) through the `LinuxNative`
/// bridge, passing vertex/index/uniform payloads as plain Java arrays.
///
/// Shaders come from the shared core `GlslShaderGenerator` (GLSL ES 1.00): the
/// material, the mesh vertex format and the environment produce a vertex and a
/// fragment source the native context compiles once and caches as a pipeline.
/// The shadow pass and the post process chain run natively against offscreen
/// targets; this class decides when and with which shaders and parameters.
class LinuxGraphicsDevice extends GraphicsDevice {
    /// Opaque handle to the native D3D11 context. Zero before creation / after
    /// disposal.
    private long contextPeer;

    private final Map<String, Long> pipelines = new HashMap<String, Long>();

    private final GpuCapabilities caps = new GpuCapabilities(
            8192, 16, true, true, true, "Codename One OpenGL ES 3.0 (Linux)", true, true);

    // Separates the vertex stage from the fragment stage in the source handed
    // to the native compiler; must equal CN1_STAGE_SEP in cn1_linux_gl.c.
    private static final String STAGE_SEPARATOR = "//@@CN1_FRAGMENT@@";

    private final float[] mvp = new float[16];
    private final float[] shadowMatrix = new float[16];

    // The packed uniform array; the order must match CN1_UNIFORMS in
    // cn1_linux_gl.c: u_mvp, u_model, u_normalMatrix, u_shadowMatrix (16 each),
    // u_color(4), u_lightDir(3), u_lightColor(3), u_ambient(3), u_eye(3),
    // u_shininess(1), u_fogColor(3), u_fog(3), u_sky(3), u_ground(3),
    // u_exposure(1), u_pbr(2), u_emissive(3), u_zenith(3), u_horizon(3),
    // u_sun(4), u_shadow(3).
    private static final int UNIFORM_FLOATS = 112;
    private final float[] uniforms = new float[UNIFORM_FLOATS];

    private long shadowPipeline;
    private long brightPipeline;
    private long blurPipeline;
    private long compositePipeline;
    private boolean inShadowPass;
    private boolean shadowReady;
    private int shadowSize;
    private PostProcess activePost;
    private final float[] postParams = new float[13];

    LinuxGraphicsDevice(long contextPeer) {
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
        long handle = LinuxNative.gl3dCreateTexture(argb, width, height);
        t.setHandle(Long.valueOf(handle));
        return t;
    }

    public void clear(int argbColor, boolean color, boolean depth) {
        if (contextPeer != 0) {
            LinuxNative.gl3dClear(contextPeer, argbColor, color, depth);
        }
    }

    public void setViewport(int x, int y, int width, int height) {
        if (contextPeer != 0) {
            LinuxNative.gl3dSetViewport(contextPeer, x, y, width, height);
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
        Environment env = getEnvironment();
        boolean shadowed = shadowReady && isShadowMapCurrent() && getLight().isShadows() && material.isLit()
                && material.isReceiveShadows();

        long vboHandle = uploadVertexBuffer(vb);
        if (vboHandle == 0) {
            return;
        }
        long pipeline = getOrCreatePipeline(material, fmt, env, shadowed);
        if (pipeline == 0) {
            return;
        }

        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        packUniforms(material, model, env, shadowed);

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
            LinuxNative.gl3dDrawIndexed(
                    contextPeer, pipeline, vboHandle, strideBytes, iboHandle,
                    ib.getIndexCount(), primitive, uniforms, UNIFORM_FLOATS,
                    texHandle, texFilter, texWrap, shadowed ? 1 : 0);
        } else {
            LinuxNative.gl3dDrawArrays(
                    contextPeer, pipeline, vboHandle, strideBytes,
                    vb.getVertexCount(), primitive, uniforms, UNIFORM_FLOATS,
                    texHandle, texFilter, texWrap, shadowed ? 1 : 0);
        }
    }

    private long uploadVertexBuffer(VertexBuffer vb) {
        Object handle = vb.getHandle();
        long peer = handle instanceof Long ? ((Long) handle).longValue() : 0;
        if (peer == 0 || vb.isDirty()) {
            float[] data = vb.getData();
            int floats = vb.getFloatCount();
            if (peer == 0) {
                peer = LinuxNative.gl3dCreateFloatBuffer(data, floats);
                vb.setHandle(Long.valueOf(peer));
            } else {
                LinuxNative.gl3dUpdateFloatBuffer(peer, data, floats);
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
                peer = LinuxNative.gl3dCreateShortBuffer(data, count);
                ib.setHandle(Long.valueOf(peer));
            } else {
                LinuxNative.gl3dUpdateShortBuffer(peer, data, count);
            }
            ib.clearDirty();
        }
        return peer;
    }

    private long getOrCreatePipeline(Material material, VertexFormat fmt, Environment env,
                                     boolean shadowed) {
        RenderState rs = material.getRenderState();
        String key = material.getShaderKey()
                + "|" + GlslShaderGenerator.variantKey(env, shadowed)
                + "|s" + fmt.getStrideBytes()
                + "|f" + System.identityHashCode(fmt)
                + "|b" + blendCode(rs.getBlendMode())
                + "|c" + cullCode(rs.getCullMode())
                + "|dt" + (rs.isDepthTest() ? 1 : 0)
                + "|dw" + (rs.isDepthWrite() ? 1 : 0);
        Long existing = pipelines.get(key);
        if (existing != null) {
            return existing.longValue();
        }
        GlslShaderGenerator gen = new GlslShaderGenerator(material, fmt, env, shadowed);
        long pipeline = createPipeline(key, gen.getVertexSource(), gen.getFragmentSource(),
                blendCode(rs.getBlendMode()), cullCode(rs.getCullMode()),
                rs.isDepthTest() ? 1 : 0, rs.isDepthWrite() ? 1 : 0);
        pipelines.put(key, Long.valueOf(pipeline));
        return pipeline;
    }

    private long createPipeline(String key, String vertex, String fragment,
                                int blend, int cull, int depthTest, int depthWrite) {
        return LinuxNative.gl3dGetOrCreatePipeline(contextPeer, key,
                vertex + STAGE_SEPARATOR + "\n" + fragment, blend, cull, depthTest, depthWrite);
    }

    private long namedPipeline(String key, String vertex, String fragment) {
        Long existing = pipelines.get(key);
        if (existing != null) {
            return existing.longValue();
        }
        long p = createPipeline(key, vertex, fragment, 0, 0, 1, 1);
        pipelines.put(key, Long.valueOf(p));
        return p;
    }

    private static void rgb(float[] u, int o, int argb) {
        u[o] = ((argb >> 16) & 0xff) / 255.0f;
        u[o + 1] = ((argb >> 8) & 0xff) / 255.0f;
        u[o + 2] = (argb & 0xff) / 255.0f;
    }

    // Packs the per-draw uniforms in the order of CN1_UNIFORMS in cn1_linux_gl.c.
    private void packUniforms(Material material, float[] model, Environment env, boolean shadowed) {
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
        for (int i = 0; i < 16; i++) {
            uniforms[o++] = shadowed ? shadowMatrix[i] : 0.0f;
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
        rgb(uniforms, o, light.getColor());
        o += 3;
        rgb(uniforms, o, light.getAmbientColor());
        o += 3;
        uniforms[o++] = cam != null ? cam.getEyeX() : 0.0f;
        uniforms[o++] = cam != null ? cam.getEyeY() : 0.0f;
        uniforms[o++] = cam != null ? cam.getEyeZ() : 0.0f;
        uniforms[o++] = material.getShininess();

        rgb(uniforms, o, env.getFogColor());
        o += 3;
        uniforms[o++] = env.getFogDensity();
        uniforms[o++] = env.getFogHeightFalloff();
        uniforms[o++] = env.getFogBaseHeight();
        rgb(uniforms, o, env.getSkyColor());
        o += 3;
        rgb(uniforms, o, env.getGroundColor());
        o += 3;
        uniforms[o++] = env.getExposure();
        uniforms[o++] = material.getMetallic();
        uniforms[o++] = material.getRoughness();
        rgb(uniforms, o, material.getEmissive());
        o += 3;
        if (env.isSky()) {
            rgb(uniforms, o, env.getZenithColor());
            o += 3;
            rgb(uniforms, o, env.getHorizonColor());
            o += 3;
            rgb(uniforms, o, env.getSunColor());
            o += 3;
            uniforms[o++] = env.getCloudCover();
        } else {
            o += 10;
        }
        if (shadowed) {
            uniforms[o++] = light.getShadowBias();
            uniforms[o++] = light.getShadowStrength();
            uniforms[o++] = shadowSize > 0 ? 1.0f / shadowSize : 0.0f;
        } else {
            o += 3;
        }
    }

    public boolean beginShadowPass(Light light) {
        if (contextPeer == 0) {
            return false;
        }
        if (shadowPipeline == 0) {
            shadowPipeline = namedPipeline("cn1|shadow", GlslShaderGenerator.shadowVertex(),
                    GlslShaderGenerator.shadowFragment());
        }
        int size = light.getShadowMapSize();
        if (shadowPipeline == 0 || !LinuxNative.gl3dBeginShadowPass(contextPeer, size)) {
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
        LinuxNative.gl3dEndShadowPass(contextPeer);
    }

    // Depth only draw of one caster into the shadow map: the shadow program reads
    // u_mvp (light space) and a_position only.
    private void drawShadowCaster(Mesh mesh, Material material, float[] modelMatrix) {
        if (!material.isCastShadows()
                || material.getRenderState().getBlendMode() != RenderState.BlendMode.NONE
                || material.getType() == Material.Type.SKYBOX) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        long vboHandle = uploadVertexBuffer(vb);
        if (vboHandle == 0 || shadowPipeline == 0) {
            return;
        }
        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        Matrix4.multiply(shadowMatrix, model, mvp);
        for (int i = 0; i < 16; i++) {
            uniforms[i] = mvp[i];
        }
        int primitive = primitiveCode(mesh.getPrimitiveType());
        int strideBytes = vb.getFormat().getStrideBytes();
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            long iboHandle = uploadIndexBuffer(ib);
            if (iboHandle == 0) {
                return;
            }
            LinuxNative.gl3dDrawIndexed(contextPeer, shadowPipeline, vboHandle, strideBytes,
                    iboHandle, ib.getIndexCount(), primitive, uniforms, UNIFORM_FLOATS, 0, 0, 0, 0);
        } else {
            LinuxNative.gl3dDrawArrays(contextPeer, shadowPipeline, vboHandle, strideBytes,
                    vb.getVertexCount(), primitive, uniforms, UNIFORM_FLOATS, 0, 0, 0, 0);
        }
    }

    public boolean beginPostProcess(PostProcess effects, int width, int height) {
        if (contextPeer == 0 || effects == null) {
            return false;
        }
        if (compositePipeline == 0) {
            String v = GlslShaderGenerator.postVertex();
            brightPipeline = namedPipeline("cn1|bright", v, GlslShaderGenerator.brightFragment());
            blurPipeline = namedPipeline("cn1|blur", v, GlslShaderGenerator.blurFragment());
            compositePipeline = namedPipeline("cn1|composite", v,
                    GlslShaderGenerator.compositeFragment());
        }
        if (compositePipeline == 0 || brightPipeline == 0 || blurPipeline == 0) {
            return false;
        }
        if (!LinuxNative.gl3dBeginPost(contextPeer, effects.isBloom())) {
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
        rgb(p, 4, pp.getShadowTint());
        rgb(p, 7, pp.getHighlightTint());
        p[10] = pp.getVignette();
        p[11] = pp.getGrain();
        p[12] = timeSeconds;
        LinuxNative.gl3dEndPost(contextPeer, brightPipeline, blurPipeline, compositePipeline,
                pp.isBloom(), p);
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
            LinuxNative.gl3dDisposeBuffer(((Long) handle).longValue());
        }
        buffer.setHandle(null);
    }

    public void dispose(IndexBuffer buffer) {
        Object handle = buffer.getHandle();
        if (handle instanceof Long) {
            LinuxNative.gl3dDisposeBuffer(((Long) handle).longValue());
        }
        buffer.setHandle(null);
    }

    public void dispose(Texture texture) {
        Object handle = texture.getHandle();
        if (handle instanceof Long) {
            LinuxNative.gl3dDisposeTexture(((Long) handle).longValue());
        }
        texture.setHandle(null);
    }

    /// Releases the native context and all cached pipelines.
    void destroy() {
        if (contextPeer != 0) {
            for (Long p : pipelines.values()) {
                if (p != null && p.longValue() != 0) {
                    LinuxNative.gl3dDisposePipeline(p.longValue());
                }
            }
            pipelines.clear();
            // the native context owned the shadow and post targets and the
            // programs; none of them outlives it
            shadowPipeline = 0;
            brightPipeline = 0;
            blurPipeline = 0;
            compositePipeline = 0;
            inShadowPass = false;
            shadowReady = false;
            activePost = null;
            LinuxNative.gl3dDestroyContext(contextPeer);
            contextPeer = 0;
        }
    }
}
