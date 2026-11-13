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
package com.codename1.impl.android;

import android.opengl.GLES20;

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
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexBuffer;
import com.codename1.gpu.VertexFormat;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.impl.gpu.GlslShaderGenerator;
import com.codename1.ui.Image;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/// OpenGL ES 2.0 implementation of the Codename One 3D `GraphicsDevice`.
///
/// Every method of this class must be invoked on the `GLSurfaceView` render
/// thread because it issues `GLES20` calls against the current EGL context. The
/// owning `AndroidGLSurface` guarantees this by only constructing the device and
/// forwarding the application `Renderer` callbacks from inside the
/// `GLSurfaceView.Renderer` hooks.
///
/// Shaders are generated once per (material variant, vertex format) pair using
/// the shared `GlslShaderGenerator` and cached as linked programs. Vertex and
/// index buffers are uploaded lazily from their SIMD aligned backing arrays via
/// direct java.nio buffers and re-uploaded only while dirty. Textures are
/// uploaded from packed ARGB pixels converted to GL's RGBA byte order.
class AndroidGraphicsDevice extends GraphicsDevice {
    /// A linked GL program together with the uniform/attribute locations the
    /// draw loop needs. A location of -1 means the program does not declare that
    /// input and the binding is skipped.
    private static final class Program {
        int handle;
        int aPosition;
        int aNormal;
        int aTexcoord;
        int aJoints = -1;
        int aWeights = -1;
        int uJoints = -1;
        int uMvp;
        int uModel;
        int uNormalMatrix;
        int uColor;
        int uTexture;
        int uLightDir;
        int uLightColor;
        int uAmbient;
        int uEye;
        int uShininess;
        final Map<String, Integer> uniforms = new HashMap<String, Integer>();
    }

    /// GPU handle for an uploaded texture.
    private static final class TexHandle {
        final int id;
        final int w;
        final int h;

        TexHandle(int id, int w, int h) {
            this.id = id;
            this.w = w;
            this.h = h;
        }
    }

    private final Map<String, Program> programs = new HashMap<String, Program>();
    // chromatic aberration, FXAA, god rays and the sun, for the composite
    private final float[] postFx = new float[6];
    private final float[] depthFx = new float[12];

    private GpuCapabilities caps;

    private final float[] mvp = new float[16];
    private final float[] model = new float[16];
    private final float[] normalMatrix = new float[16];

    // an offscreen colour target (and optional depth) for the shadow map and
    // the post processing chain
    private static final class Target {
        int fbo;
        int tex;
        int depth;
        boolean depthTex;
        int width;
        int height;
    }

    private Program shadowProgram;
    private Program brightProgram;
    private Program blurProgram;
    private Program compositeProgram;
    private Target shadowTarget;
    private Target sceneTarget;
    private Target bloomA;
    private Target bloomB;
    private int quadVbo;
    private boolean inShadowPass;
    private boolean shadowReady;
    private final float[] shadowMatrix = new float[16];
    // the far cascade's matrix, and the cascade count and square size of the
    // shadow pass in progress (the map is shadowCascades squares wide)
    private final float[] farShadowMatrix = new float[16];
    private int shadowCascades = 1;
    private int shadowSize;
    private PostProcess activePost;
    private final int[] savedFbo = new int[1];
    private final int[] savedViewport = new int[4];

    /// Lazily builds and caches the device capabilities by querying GL limits.
    public GpuCapabilities getCapabilities() {
        if (caps == null) {
            int[] v = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, v, 0);
            int maxTex = v[0] > 0 ? v[0] : 2048;
            GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, v, 0);
            int maxAttribs = v[0] > 0 ? v[0] : 8;
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String version = GLES20.glGetString(GLES20.GL_VERSION);
            String name = "Codename One OpenGL ES (Android)";
            if (renderer != null) {
                name = name + " - " + renderer;
            }
            if (version != null) {
                name = name + " / " + version;
            }
            caps = new GpuCapabilities(maxTex, maxAttribs, false, false, false, name, true, true);
        }
        return caps;
    }

    public Texture createTexture(Image image) {
        return createTexture(image.getWidth(), image.getHeight(), image.getRGB());
    }

    public Texture createTexture(int w, int h, int[] argb) {
        Texture t = new Texture(w, h);
        int id = uploadTexture(w, h, argb);
        t.setHandle(new TexHandle(id, w, h));
        return t;
    }

    private int uploadTexture(int w, int h, int[] argb) {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        int id = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);

        // Convert packed ARGB ints into tightly packed RGBA bytes.
        ByteBuffer pixels = ByteBuffer.allocateDirect(w * h * 4);
        pixels.order(ByteOrder.nativeOrder());
        int count = w * h;
        for (int i = 0; i < count; i++) {
            int c = argb[i];
            pixels.put((byte) ((c >> 16) & 0xff));
            pixels.put((byte) ((c >> 8) & 0xff));
            pixels.put((byte) (c & 0xff));
            pixels.put((byte) ((c >>> 24) & 0xff));
        }
        pixels.position(0);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return id;
    }

    public void clear(int argbColor, boolean clearColor, boolean clearDepth) {
        int mask = 0;
        if (clearColor) {
            float a = ((argbColor >>> 24) & 0xff) / 255.0f;
            float r = ((argbColor >> 16) & 0xff) / 255.0f;
            float g = ((argbColor >> 8) & 0xff) / 255.0f;
            float b = (argbColor & 0xff) / 255.0f;
            GLES20.glClearColor(r, g, b, a);
            mask |= GLES20.GL_COLOR_BUFFER_BIT;
        }
        if (clearDepth) {
            GLES20.glClearDepthf(1.0f);
            // Depth writes must be enabled for the depth buffer to be cleared.
            GLES20.glDepthMask(true);
            mask |= GLES20.GL_DEPTH_BUFFER_BIT;
        }
        if (mask != 0) {
            GLES20.glClear(mask);
        }
    }

    public void setViewport(int x, int y, int w, int h) {
        GLES20.glViewport(x, y, w, h);
    }

    public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();

        if (inShadowPass) {
            drawShadowCaster(mesh, material, modelMatrix);
            return;
        }
        Environment env = getEnvironment();
        boolean shadowed = shadowReady && isShadowMapCurrent() && getLight().isShadows() && material.isLit()
                && material.isReceiveShadows();
        Program program = getProgram(material, fmt, env, shadowed);
        drawWith(mesh, material, modelMatrix, program, cameraViewProjection(),
                shadowed, false);
    }

    // One draw of a mesh through a program. vp is the camera's view projection,
    // or the light's for a caster whose material needs its own depth program;
    // a caster draws with depth on, no blending and both faces.
    private void drawWith(Mesh mesh, Material material, float[] modelMatrix, Program program, float[] vp,
            boolean shadowed, boolean caster) {
        if (program.handle == 0) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        Environment env = getEnvironment();
        GLES20.glUseProgram(program.handle);
        applyEnvironment(program, material, env, shadowed);

        // Compose matrices: mvp = viewProjection * model.
        if (modelMatrix != null) {
            Matrix4.copy(modelMatrix, model);
        } else {
            Matrix4.setIdentity(model);
        }
        Camera cam = getCamera();
        Matrix4.multiply(vp, model, mvp);
        float[] nm = Matrix4.normalMatrix(model);
        for (int i = 0; i < 16; i++) {
            normalMatrix[i] = nm[i];
        }

        if (program.uMvp >= 0) {
            GLES20.glUniformMatrix4fv(program.uMvp, 1, false, mvp, 0);
        }
        if (program.uModel >= 0) {
            GLES20.glUniformMatrix4fv(program.uModel, 1, false, model, 0);
        }
        if (program.uNormalMatrix >= 0) {
            GLES20.glUniformMatrix4fv(program.uNormalMatrix, 1, false, normalMatrix, 0);
        }
        if (program.uColor >= 0) {
            int c = material.getColor();
            float a = ((c >>> 24) & 0xff) / 255.0f;
            float r = ((c >> 16) & 0xff) / 255.0f;
            float g = ((c >> 8) & 0xff) / 255.0f;
            float b = (c & 0xff) / 255.0f;
            GLES20.glUniform4f(program.uColor, r, g, b, a);
        }

        Light light = getLight();
        if (program.uLightDir >= 0) {
            GLES20.glUniform3f(program.uLightDir,
                    light.getDirectionX(), light.getDirectionY(), light.getDirectionZ());
        }
        if (program.uLightColor >= 0) {
            int lc = light.getColor();
            GLES20.glUniform3f(program.uLightColor,
                    ((lc >> 16) & 0xff) / 255.0f, ((lc >> 8) & 0xff) / 255.0f, (lc & 0xff) / 255.0f);
        }
        if (program.uAmbient >= 0) {
            int ac = light.getAmbientColor();
            GLES20.glUniform3f(program.uAmbient,
                    ((ac >> 16) & 0xff) / 255.0f, ((ac >> 8) & 0xff) / 255.0f, (ac & 0xff) / 255.0f);
        }
        if (program.uEye >= 0) {
            float ex = cam != null ? cam.getEyeX() : 0;
            float ey = cam != null ? cam.getEyeY() : 0;
            float ez = cam != null ? cam.getEyeZ() : 0;
            GLES20.glUniform3f(program.uEye, ex, ey, ez);
        }
        if (program.uShininess >= 0) {
            GLES20.glUniform1f(program.uShininess, material.getShininess());
        }

        // Bind the texture, if any, to unit 0.
        Texture tex = material.getTexture();
        if (tex != null && program.uTexture >= 0) {
            TexHandle th = (TexHandle) tex.getHandle();
            if (th != null) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, th.id);
                int wrap = tex.getWrap() == Texture.Wrap.REPEAT
                        ? GLES20.GL_REPEAT : GLES20.GL_CLAMP_TO_EDGE;
                int filter = tex.getFilter() == Texture.Filter.NEAREST
                        ? GLES20.GL_NEAREST : GLES20.GL_LINEAR;
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
                GLES20.glUniform1i(program.uTexture, 0);
            }
        }

        if (caster) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        } else {
            applyRenderState(material.getRenderState());
        }

        // Upload (if dirty) and bind the vertex buffer, then wire the attribute
        // pointers from the interleaved format offsets.
        int vbo = uploadVertexBuffer(vb);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        int strideBytes = fmt.getStrideBytes();
        bindAttribute(program.aPosition, fmt, VertexAttribute.Usage.POSITION, strideBytes);
        bindAttribute(program.aNormal, fmt, VertexAttribute.Usage.NORMAL, strideBytes);
        bindAttribute(program.aTexcoord, fmt, VertexAttribute.Usage.TEXCOORD, strideBytes);
        bindAttribute(program.aJoints, fmt, VertexAttribute.Usage.JOINTS, strideBytes);
        bindAttribute(program.aWeights, fmt, VertexAttribute.Usage.WEIGHTS, strideBytes);
        if (program.uJoints >= 0 && material.isSkinned()) {
            GLES20.glUniform4fv(program.uJoints, material.getJointCount() * 3, material.getJointRows(), 0);
        }

        int glMode = toGlPrimitive(mesh.getPrimitiveType());
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            int ibo = uploadIndexBuffer(ib);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GLES20.glDrawElements(glMode, ib.getIndexCount(), GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            GLES20.glDrawArrays(glMode, 0, vb.getVertexCount());
        }

        disableAttribute(program.aPosition);
        disableAttribute(program.aNormal);
        disableAttribute(program.aTexcoord);
        disableAttribute(program.aJoints);
        disableAttribute(program.aWeights);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private void bindAttribute(int location, VertexFormat fmt,
                               VertexAttribute.Usage usage, int strideBytes) {
        if (location < 0) {
            return;
        }
        int offsetFloats = -1;
        int components = 0;
        for (int i = 0; i < fmt.getAttributeCount(); i++) {
            VertexAttribute a = fmt.getAttribute(i);
            if (a.getUsage() == usage) {
                offsetFloats = fmt.getAttributeOffset(i);
                components = a.getComponents();
                break;
            }
        }
        if (offsetFloats < 0) {
            GLES20.glDisableVertexAttribArray(location);
            return;
        }
        GLES20.glEnableVertexAttribArray(location);
        GLES20.glVertexAttribPointer(location, components, GLES20.GL_FLOAT, false,
                strideBytes, offsetFloats * 4);
    }

    private void disableAttribute(int location) {
        if (location >= 0) {
            GLES20.glDisableVertexAttribArray(location);
        }
    }

    private void applyRenderState(RenderState rs) {
        if (rs.isDepthTest()) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        } else {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        GLES20.glDepthMask(rs.isDepthWrite());

        RenderState.BlendMode blend = rs.getBlendMode();
        if (blend == RenderState.BlendMode.NONE) {
            GLES20.glDisable(GLES20.GL_BLEND);
        } else {
            GLES20.glEnable(GLES20.GL_BLEND);
            // The alpha channel accumulates coverage rather than being
            // weighted by itself, so an opaque frame stays opaque
            // (RenderState.BlendMode's contract). The default surface has no
            // alpha channel and showed nothing of this; one that does -- a
            // translucent surface, a frame read back -- would have.
            if (blend == RenderState.BlendMode.ADDITIVE) {
                GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE,
                        GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                GLES20.glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
                        GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

        RenderState.CullMode cull = rs.getCullMode();
        if (cull == RenderState.CullMode.NONE) {
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        } else {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            // The portable winding convention treats counter clockwise as front.
            GLES20.glFrontFace(inRenderTarget ? GLES20.GL_CW : GLES20.GL_CCW);
            GLES20.glCullFace(cull == RenderState.CullMode.FRONT
                    ? GLES20.GL_FRONT : GLES20.GL_BACK);
        }
    }

    private static int toGlPrimitive(PrimitiveType type) {
        switch (type) {
            case POINTS:
                return GLES20.GL_POINTS;
            case LINES:
                return GLES20.GL_LINES;
            case LINE_STRIP:
                return GLES20.GL_LINE_STRIP;
            case TRIANGLE_STRIP:
                return GLES20.GL_TRIANGLE_STRIP;
            case TRIANGLES:
            default:
                return GLES20.GL_TRIANGLES;
        }
    }

    /// Per-buffer GPU state stored on the buffer handle: the GL buffer id and the
    /// reusable direct nio view of the SIMD aligned backing array.
    private static final class VboHandle {
        final int id;
        FloatBuffer view;

        VboHandle(int id) {
            this.id = id;
        }
    }

    private static final class IboHandle {
        final int id;
        ShortBuffer view;

        IboHandle(int id) {
            this.id = id;
        }
    }

    private int uploadVertexBuffer(VertexBuffer vb) {
        VboHandle h = (VboHandle) vb.getHandle();
        if (h == null) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            h = new VboHandle(ids[0]);
            vb.setHandle(h);
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, h.id);
        if (vb.isDirty()) {
            int floatCount = vb.getFloatCount();
            float[] data = vb.getData();
            if (h.view == null || h.view.capacity() < floatCount) {
                ByteBuffer bb = ByteBuffer.allocateDirect(floatCount * 4);
                bb.order(ByteOrder.nativeOrder());
                h.view = bb.asFloatBuffer();
            }
            h.view.position(0);
            h.view.put(data, 0, floatCount);
            h.view.position(0);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, floatCount * 4, h.view, GLES20.GL_STATIC_DRAW);
            vb.clearDirty();
        }
        return h.id;
    }

    private int uploadIndexBuffer(IndexBuffer ib) {
        IboHandle h = (IboHandle) ib.getHandle();
        if (h == null) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            h = new IboHandle(ids[0]);
            ib.setHandle(h);
        }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, h.id);
        if (ib.isDirty()) {
            int indexCount = ib.getIndexCount();
            short[] data = ib.getData();
            if (h.view == null || h.view.capacity() < indexCount) {
                ByteBuffer bb = ByteBuffer.allocateDirect(indexCount * 2);
                bb.order(ByteOrder.nativeOrder());
                h.view = bb.asShortBuffer();
            }
            h.view.position(0);
            h.view.put(data, 0, indexCount);
            h.view.position(0);
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexCount * 2, h.view, GLES20.GL_STATIC_DRAW);
            ib.clearDirty();
        }
        return h.id;
    }

    private Program getProgram(Material material, VertexFormat fmt, Environment env, boolean shadowed) {
        String key = material.getShaderKey() + "|" + GlslShaderGenerator.variantKey(env, shadowed)
                + "|" + System.identityHashCode(fmt);
        Program p = programs.get(key);
        if (p != null) {
            return p;
        }
        GlslShaderGenerator gen = new GlslShaderGenerator(material, fmt, env, shadowed);
        p = buildProgram(gen.getVertexSource(), gen.getFragmentSource());
        programs.put(key, p);
        return p;
    }

    private Program getDepthProgram(Material material, VertexFormat fmt) {
        String key = "depth|" + material.getShaderKey() + "|" + System.identityHashCode(fmt);
        Program p = programs.get(key);
        if (p == null) {
            GlslShaderGenerator gen = GlslShaderGenerator.depthOnly(material, fmt);
            p = buildProgram(gen.getVertexSource(), gen.getFragmentSource());
            programs.put(key, p);
        }
        return p;
    }

    private Program buildProgram(String vertexSrc, String fragmentSrc) {
        int handle = linkProgram(vertexSrc, fragmentSrc);
        Program p = new Program();
        p.handle = handle;
        if (handle != 0) {
            p.aPosition = GLES20.glGetAttribLocation(handle, GlslShaderGenerator.A_POSITION);
            p.aNormal = GLES20.glGetAttribLocation(handle, GlslShaderGenerator.A_NORMAL);
            p.aTexcoord = GLES20.glGetAttribLocation(handle, GlslShaderGenerator.A_TEXCOORD);
            p.aJoints = GLES20.glGetAttribLocation(handle, GlslShaderGenerator.A_JOINTS);
            p.aWeights = GLES20.glGetAttribLocation(handle, GlslShaderGenerator.A_WEIGHTS);
            p.uJoints = GLES20.glGetUniformLocation(handle, GlslShaderGenerator.U_JOINTS);
            p.uMvp = GLES20.glGetUniformLocation(handle, "u_mvp");
            p.uModel = GLES20.glGetUniformLocation(handle, "u_model");
            p.uNormalMatrix = GLES20.glGetUniformLocation(handle, "u_normalMatrix");
            p.uColor = GLES20.glGetUniformLocation(handle, "u_color");
            p.uTexture = GLES20.glGetUniformLocation(handle, "u_texture");
            p.uLightDir = GLES20.glGetUniformLocation(handle, "u_lightDir");
            p.uLightColor = GLES20.glGetUniformLocation(handle, "u_lightColor");
            p.uAmbient = GLES20.glGetUniformLocation(handle, "u_ambient");
            p.uEye = GLES20.glGetUniformLocation(handle, "u_eye");
            p.uShininess = GLES20.glGetUniformLocation(handle, "u_shininess");
        }
        return p;
    }

    private int loc(Program p, String name) {
        Integer cached = p.uniforms.get(name);
        if (cached == null) {
            cached = Integer.valueOf(p.handle == 0 ? -1 : GLES20.glGetUniformLocation(p.handle, name));
            p.uniforms.put(name, cached);
        }
        return cached.intValue();
    }

    private void uniformColor(Program p, String name, int argb) {
        int l = loc(p, name);
        if (l >= 0) {
            GLES20.glUniform3f(l, ((argb >> 16) & 0xff) / 255.0f, ((argb >> 8) & 0xff) / 255.0f,
                    (argb & 0xff) / 255.0f);
        }
    }

    private void uniform1(Program p, String name, float v) {
        int l = loc(p, name);
        if (l >= 0) {
            GLES20.glUniform1f(l, v);
        }
    }

    private void uniform2(Program p, String name, float a, float b) {
        int l = loc(p, name);
        if (l >= 0) {
            GLES20.glUniform2f(l, a, b);
        }
    }

    private void uniform4(Program p, String name, float a, float b, float c, float d) {
        int l = loc(p, name);
        if (l >= 0) {
            GLES20.glUniform4f(l, a, b, c, d);
        }
    }

    private void uniform3(Program p, String name, float a, float b, float c) {
        int l = loc(p, name);
        if (l >= 0) {
            GLES20.glUniform3f(l, a, b, c);
        }
    }

    private void sampler(Program p, String name, int unit, int tex) {
        int l = loc(p, name);
        if (l >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
            GLES20.glUniform1i(l, unit);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        }
    }


    // custom shading uniform names, so a draw does not build strings
    private static final String[] CU_NAMES = new String[ShaderMaterial.MAX_UNIFORMS];
    private static final String[] CT_NAMES = new String[ShaderMaterial.MAX_TEXTURES];

    static {
        for (int i = 0; i < CU_NAMES.length; i++) {
            CU_NAMES[i] = "u_cu[" + i + "]";
        }
        for (int i = 0; i < CT_NAMES.length; i++) {
            CT_NAMES[i] = "u_ct" + i;
        }
    }

    // Custom shading (Material.getShader()): the clock, the uniform array and
    // the material's own textures on units 2 and up -- 0 is the base texture,
    // 1 the shadow map.
    private void applyShader(Program p, Material material) {
        ShaderMaterial sm = material.getEffectiveShader();
        if (sm == null) {
            return;
        }
        uniform1(p, "u_time", getTime());
        float[] v = sm.getUniformValues();
        for (int i = 0; i < sm.getUniformCount(); i++) {
            int l = loc(p, CU_NAMES[i]);
            if (l >= 0) {
                GLES20.glUniform4f(l, v[i * 4], v[i * 4 + 1], v[i * 4 + 2], v[i * 4 + 3]);
            }
        }
        for (int i = 0; i < sm.getTextureCount(); i++) {
            Texture t = sm.getTexture(i);
            int l = loc(p, CT_NAMES[i]);
            if (l >= 0 && t.getHandle() instanceof TexHandle) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 2 + i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ((TexHandle) t.getHandle()).id);
                int wrap = t.getWrap() == Texture.Wrap.REPEAT ? GLES20.GL_REPEAT : GLES20.GL_CLAMP_TO_EDGE;
                int filter = t.getFilter() == Texture.Filter.NEAREST ? GLES20.GL_NEAREST : GLES20.GL_LINEAR;
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
                GLES20.glUniform1i(l, 2 + i);
            }
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    }

    // Binds the environment map image based lighting reads (unit 6) when the
    // program declares it.
    private void applyEnvironmentMap(Program p) {
        Environment env = getEnvironment();
        Texture t = env == null ? null : env.getEnvironmentMap();
        if (t == null) {
            return;
        }
        int l = loc(p, "u_envMap");
        if (l >= 0 && t.getHandle() instanceof TexHandle) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 6);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ((TexHandle) t.getHandle()).id);
            int wrap = t.getWrap() == Texture.Wrap.REPEAT ? GLES20.GL_REPEAT : GLES20.GL_CLAMP_TO_EDGE;
            int filter = t.getFilter() == Texture.Filter.NEAREST ? GLES20.GL_NEAREST : GLES20.GL_LINEAR;
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
            GLES20.glUniform1i(l, 6);
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    }

    private void applyEnvironment(Program p, Material material, Environment env, boolean shadowed) {
        uniformColor(p, "u_fogColor", env.getFogColor());
        uniform3(p, "u_fog", env.getFogDensity(), env.getFogHeightFalloff(), env.getFogBaseHeight());
        uniformColor(p, "u_sky", env.getSkyColor());
        uniformColor(p, "u_ground", env.getGroundColor());
        uniform1(p, "u_exposure", env.getExposure());
        uniform2(p, "u_pbr", material.getMetallic(), material.getRoughness());
        if (env.isSky()) {
            uniformColor(p, "u_zenith", env.getZenithColor());
            uniformColor(p, "u_horizon", env.getHorizonColor());
            int sun = env.getSunColor();
            int l = loc(p, "u_sun");
            if (l >= 0) {
                GLES20.glUniform4f(l, ((sun >> 16) & 0xff) / 255f, ((sun >> 8) & 0xff) / 255f, (sun & 0xff) / 255f,
                        env.getCloudCover());
            }
        }
        uniformColor(p, "u_emissive", material.getEmissive());
        applyShader(p, material);
        applyEnvironmentMap(p);
        if (shadowed) {
            int l = loc(p, "u_shadowMatrix");
            if (l >= 0) {
                GLES20.glUniformMatrix4fv(l, 1, false, shadowMatrix, 0);
            }
            Light light = getLight();
            uniform4(p, "u_shadow", light.getShadowBias(), light.getShadowStrength(),
                    light.getShadowSoftness() / shadowTarget.height, light.getShadowCascadeRatio());
            sampler(p, "u_shadowMap", 1, shadowTarget.tex);
        }
    }

    private Target target(Target t, int w, int h, boolean depth) {
        return target(t, w, h, depth, GLES20.GL_LINEAR);
    }

    private Target target(Target t, int w, int h, boolean depth, int filter) {
        if (t != null && t.width == w && t.height == h) {
            return t;
        }
        if (t != null) {
            releaseTarget(t);
        }
        Target n = new Target();
        n.width = w;
        n.height = h;
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        n.tex = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, n.tex);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glGenFramebuffers(1, ids, 0);
        n.fbo = ids[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, n.fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, n.tex, 0);
        if (depth) {
            // a depth texture, so the composite can read the scene's depth for
            // ambient occlusion, depth of field and outlines; ES 2 devices
            // without OES_depth_texture leave the framebuffer incomplete and
            // fall back to a renderbuffer, and the depth effects turn off
            GLES20.glGenTextures(1, ids, 0);
            n.depth = ids[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, n.depth);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT, w, h, 0,
                    GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_INT, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_TEXTURE_2D, n.depth, 0);
            if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE) {
                n.depthTex = true;
                return n;
            }
            for (int i = 0; i < 8 && GLES20.glGetError() != GLES20.GL_NO_ERROR; i++) {
                // drain the error the unsupported format raised
            }
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_TEXTURE_2D, 0, 0);
            GLES20.glDeleteTextures(1, new int[]{n.depth}, 0);
            GLES20.glGenRenderbuffers(1, ids, 0);
            n.depth = ids[0];
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, n.depth);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, w, h);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_RENDERBUFFER, n.depth);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        }
        return n;
    }

    private void releaseTarget(Target t) {
        if (t == null) {
            return;
        }
        GLES20.glDeleteFramebuffers(1, new int[]{t.fbo}, 0);
        GLES20.glDeleteTextures(1, new int[]{t.tex}, 0);
        if (t.depthTex) {
            GLES20.glDeleteTextures(1, new int[]{t.depth}, 0);
        } else if (t.depth != 0) {
            GLES20.glDeleteRenderbuffers(1, new int[]{t.depth}, 0);
        }
    }

    // The panel renders into its own framebuffer, so "the screen" is whatever
    // was bound when a pass began, never framebuffer 0.
    private void saveTarget() {
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, savedFbo, 0);
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, savedViewport, 0);
    }

    private void restoreTarget() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, savedFbo[0]);
        GLES20.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }

    public boolean beginShadowPass(Light light) {
        if (shadowProgram == null) {
            shadowProgram = buildProgram(GlslShaderGenerator.shadowVertex(),
                    GlslShaderGenerator.shadowFragment());
        }
        if (shadowProgram.handle == 0) {
            return false;
        }
        saveTarget();
        int size = light.getShadowMapSize();
        // packed depth must not be interpolated between texels
        shadowCascades = light.getShadowCascades();
        shadowSize = size;
        shadowTarget = target(shadowTarget, size * shadowCascades, size, true, GLES20.GL_NEAREST);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowTarget.fbo);
        GLES20.glViewport(0, 0, size * shadowCascades, size);
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glClearDepthf(1f);
        GLES20.glDepthMask(true);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        light.computeCascadeMatrix(0, shadowMatrix);
        light.computeCascadeMatrix(1, farShadowMatrix);
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
        restoreTarget();
    }

    // Draws a caster into every cascade: each has its own square of the map
    // and its own matrix.
    private void drawShadowCaster(Mesh mesh, Material material, float[] modelMatrix) {
        if (shadowCascades < 2) {
            drawShadowCascade(mesh, material, modelMatrix, shadowMatrix);
            return;
        }
        GLES20.glViewport(0, 0, shadowSize, shadowSize);
        drawShadowCascade(mesh, material, modelMatrix, shadowMatrix);
        GLES20.glViewport(shadowSize, 0, shadowSize, shadowSize);
        drawShadowCascade(mesh, material, modelMatrix, farShadowMatrix);
        GLES20.glViewport(0, 0, shadowSize * 2, shadowSize);
    }

    private void drawShadowCascade(Mesh mesh, Material material, float[] modelMatrix, float[] lightMatrix) {
        if (!material.isCastShadows() || material.getRenderState().getBlendMode() != RenderState.BlendMode.NONE
                || material.getType() == Material.Type.SKYBOX) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        if (GlslShaderGenerator.needsOwnDepthProgram(material)) {
            drawWith(mesh, material, modelMatrix, getDepthProgram(material, fmt), lightMatrix, false, true);
            return;
        }
        GLES20.glUseProgram(shadowProgram.handle);
        if (modelMatrix != null) {
            Matrix4.copy(modelMatrix, model);
        } else {
            Matrix4.setIdentity(model);
        }
        Matrix4.multiply(lightMatrix, model, mvp);
        GLES20.glUniformMatrix4fv(shadowProgram.uMvp, 1, false, mvp, 0);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        int vbo = uploadVertexBuffer(vb);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        bindAttribute(shadowProgram.aPosition, fmt, VertexAttribute.Usage.POSITION, fmt.getStrideBytes());
        int glMode = toGlPrimitive(mesh.getPrimitiveType());
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            int ibo = uploadIndexBuffer(ib);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GLES20.glDrawElements(glMode, ib.getIndexCount(), GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            GLES20.glDrawArrays(glMode, 0, vb.getVertexCount());
        }
        disableAttribute(shadowProgram.aPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    protected boolean beginPostEffects(PostProcess effects, int width, int height) {
        if (effects == null) {
            return false;
        }
        if (compositeProgram == null) {
            String v = GlslShaderGenerator.postVertex();
            brightProgram = buildProgram(v, GlslShaderGenerator.brightFragment());
            blurProgram = buildProgram(v, GlslShaderGenerator.blurFragment());
            compositeProgram = buildProgram(v, GlslShaderGenerator.compositeFragment());
        }
        if (compositeProgram.handle == 0 || brightProgram.handle == 0 || blurProgram.handle == 0) {
            return false;
        }
        saveTarget();
        int w = savedViewport[2];
        int h = savedViewport[3];
        if (w <= 0 || h <= 0) {
            return false;
        }
        sceneTarget = target(sceneTarget, w, h, true);
        if (effects.isBloom()) {
            bloomA = target(bloomA, Math.max(1, w / 4), Math.max(1, h / 4), false);
            bloomB = target(bloomB, bloomA.width, bloomA.height, false);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, sceneTarget.fbo);
        GLES20.glViewport(0, 0, w, h);
        activePost = effects;
        return true;
    }

    protected void endPostEffects(float timeSeconds) {
        PostProcess pp = activePost;
        if (pp == null) {
            return;
        }
        activePost = null;
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        int bloomTex = sceneTarget.tex;
        if (pp.isBloom()) {
            passInto(bloomA);
            GLES20.glUseProgram(brightProgram.handle);
            sampler(brightProgram, "u_scene", 0, sceneTarget.tex);
            uniform1(brightProgram, "u_threshold", pp.getBloomThreshold());
            uniform2(brightProgram, "u_texel", 1f / sceneTarget.width, 1f / sceneTarget.height);
            drawQuad(brightProgram);
            for (int i = 0; i < 2; i++) {
                float spread = 1f + i;
                passInto(bloomB);
                GLES20.glUseProgram(blurProgram.handle);
                sampler(blurProgram, "u_scene", 0, bloomA.tex);
                uniform2(blurProgram, "u_dir", spread / bloomA.width, 0f);
                drawQuad(blurProgram);
                passInto(bloomA);
                sampler(blurProgram, "u_scene", 0, bloomB.tex);
                uniform2(blurProgram, "u_dir", 0f, spread / bloomA.height);
                drawQuad(blurProgram);
            }
            bloomTex = bloomA.tex;
        }
        restoreTarget();
        Program c = compositeProgram;
        GLES20.glUseProgram(c.handle);
        sampler(c, "u_scene", 0, sceneTarget.tex);
        sampler(c, "u_bloom", 1, bloomTex);
        uniform1(c, "u_bloomIntensity", pp.isBloom() ? pp.getBloomIntensity() : 0f);
        uniform2(c, "u_grade", pp.getContrast(), pp.getSaturation());
        uniformColor(c, "u_lift", pp.getShadowTint());
        uniformColor(c, "u_gain", pp.getHighlightTint());
        uniform1(c, "u_vignette", pp.getVignette());
        uniform1(c, "u_grain", pp.getGrain());
        uniform1(c, "u_time", timeSeconds);
        uniform2(c, "u_resolution", sceneTarget.width, sceneTarget.height);
        packPostEffects(pp, postFx, 0);
        uniform3(c, "u_fx", postFx[0], postFx[1], postFx[2]);
        uniform2(c, "u_sun", postFx[3], postFx[4]);
        packDepthEffects(pp, depthFx, 0);
        if (!sceneTarget.depthTex) {
            depthFx[0] = 0f;
            depthFx[3] = 0f;
            depthFx[4] = 0f;
        }
        sampler(c, "u_depth", 2, sceneTarget.depthTex ? sceneTarget.depth : 0);
        Texture lut = pp.getColorLut();
        if (lut != null && lut.getHandle() instanceof TexHandle) {
            sampler(c, "u_lut", 3, ((TexHandle) lut.getHandle()).id);
        } else {
            depthFx[7] = 0f;
            sampler(c, "u_lut", 3, 0);
        }
        uniform4(c, "u_dfx", depthFx[0], depthFx[1], depthFx[2], depthFx[3]);
        uniform4(c, "u_dfx2", depthFx[4], depthFx[5], depthFx[6], depthFx[7]);
        uniform3(c, "u_ocol", depthFx[8], depthFx[9], depthFx[10]);
        drawQuad(c);
        GLES20.glDepthMask(true);
    }

    private void passInto(Target t) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, t.fbo);
        GLES20.glViewport(0, 0, t.width, t.height);
    }

    private void drawQuad(Program p) {
        if (quadVbo == 0) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            quadVbo = ids[0];
            FloatBuffer fb = ByteBuffer.allocateDirect(12 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            fb.put(new float[]{-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f});
            fb.position(0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 12 * 4, fb, GLES20.GL_STATIC_DRAW);
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo);
        GLES20.glEnableVertexAttribArray(p.aPosition);
        GLES20.glVertexAttribPointer(p.aPosition, 3, GLES20.GL_FLOAT, false, 12, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(p.aPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private int linkProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc);
        if (vs == 0 || fs == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        // The individual shaders can be released once the program is linked.
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        if (status[0] == 0) {
            android.util.Log.e("CN1Gpu", "program link failed: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            android.util.Log.e("CN1Gpu", "shader compile failed: " + GLES20.glGetShaderInfoLog(shader)
                    + "\n" + source);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public void dispose(VertexBuffer buffer) {
        VboHandle h = (VboHandle) buffer.getHandle();
        if (h != null) {
            GLES20.glDeleteBuffers(1, new int[]{h.id}, 0);
            buffer.setHandle(null);
        }
    }

    public void dispose(IndexBuffer buffer) {
        IboHandle h = (IboHandle) buffer.getHandle();
        if (h != null) {
            GLES20.glDeleteBuffers(1, new int[]{h.id}, 0);
            buffer.setHandle(null);
        }
    }


    // Render targets: each is a colour texture with a depth buffer of its own.
    // GL stores a target's rows bottom up while an uploaded image starts at
    // the top, so draws into a target flip clip space Y (and with it the
    // winding) to leave the texture upright, as Metal and Direct3D have it.
    private final Map<Texture, Target> renderTargets = new HashMap<Texture, Target>();
    private boolean inRenderTarget;
    private final int[] rtSavedFbo = new int[1];
    private final int[] rtSavedViewport = new int[4];
    private final float[] flippedVp = new float[16];

    @Override
    public boolean isGpuSkinning() {
        return true;
    }

    @Override
    public Texture createRenderTarget(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        // creating the framebuffer binds it; drawing stays where it was
        int[] bound = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, bound, 0);
        Target t = target(null, width, height, true);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bound[0]);
        Texture tex = new Texture(width, height);
        tex.setHandle(new TexHandle(t.tex, width, height));
        renderTargets.put(tex, t);
        return tex;
    }

    public boolean beginRenderTarget(Texture target, int clearArgb) {
        Target t = target == null ? null : renderTargets.get(target);
        if (t == null || inRenderTarget || inShadowPass || activePost != null) {
            return false;
        }
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, rtSavedFbo, 0);
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, rtSavedViewport, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, t.fbo);
        GLES20.glViewport(0, 0, t.width, t.height);
        inRenderTarget = true;
        clear(clearArgb, true, true);
        return true;
    }

    public void endRenderTarget() {
        if (!inRenderTarget) {
            return;
        }
        inRenderTarget = false;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, rtSavedFbo[0]);
        GLES20.glViewport(rtSavedViewport[0], rtSavedViewport[1], rtSavedViewport[2], rtSavedViewport[3]);
    }

    // the camera's view projection, flipped in Y inside a render target
    private float[] cameraViewProjection() {
        Camera cam = getCamera();
        float[] vp = cam != null ? cam.getViewProjection() : Matrix4.identity();
        if (!inRenderTarget) {
            return vp;
        }
        for (int i = 0; i < 16; i++) {
            flippedVp[i] = (i & 3) == 1 ? -vp[i] : vp[i];
        }
        return flippedVp;
    }

    public void dispose(Texture texture) {
        Target rt = renderTargets.remove(texture);
        if (rt != null) {
            releaseTarget(rt);
            texture.setHandle(null);
            return;
        }
        TexHandle h = (TexHandle) texture.getHandle();
        if (h != null) {
            GLES20.glDeleteTextures(1, new int[]{h.id}, 0);
            texture.setHandle(null);
        }
    }

    /// Releases all cached GL programs. Called when the surface is destroyed and
    /// the EGL context is lost so that nothing dangles into a new context.
    void disposePrograms() {
        for (Program p : programs.values()) {
            if (p.handle != 0) {
                GLES20.glDeleteProgram(p.handle);
            }
        }
        programs.clear();
        caps = null;
        shadowProgram = null;
        brightProgram = null;
        blurProgram = null;
        compositeProgram = null;
        // the context that owned these is gone with them
        shadowTarget = null;
        sceneTarget = null;
        bloomA = null;
        bloomB = null;
        quadVbo = 0;
        shadowReady = false;
    }
}
