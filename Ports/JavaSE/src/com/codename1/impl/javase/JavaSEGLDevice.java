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
package com.codename1.impl.javase;

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
import com.codename1.impl.gpu.GlslShaderGenerator;
import com.codename1.ui.Image;

import com.jogamp.opengl.GL2ES2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

/// Desktop OpenGL implementation of the Codename One 3D `GraphicsDevice`, used by
/// the JavaSE simulator through JOGL. It mirrors the Android OpenGL ES backend:
/// the same shared `GlslShaderGenerator` produces the shaders, and geometry is
/// uploaded through direct java.nio buffers. The only difference is that GL calls
/// go through a JOGL `GL2ES2` instance (the GLES2 compatible profile) rather than
/// the static Android `GLES20` class.
///
/// Every method must run on the JOGL drawable's GL thread; the owning
/// `JavaSEJoglSurface` sets the current `GL2ES2` (via `setGL`) and forwards the
/// application `Renderer` callbacks from inside the `GLEventListener` hooks.
class JavaSEGLDevice extends GraphicsDevice {
    private GL2ES2 gl;

    private final Map<String, Program> programs = new HashMap<String, Program>();
    private GpuCapabilities caps;

    private final float[] mvp = new float[16];
    private final float[] model = new float[16];
    private final float[] normalMatrix = new float[16];

    /// Sets the GL context for the current callback. Called by the surface at the
    /// start of every `GLEventListener` hook.
    void setGL(GL2ES2 gl) {
        this.gl = gl;
    }

    private static final class Program {
        int handle;
        int aPosition;
        int aNormal;
        int aTexcoord;
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

    // an offscreen colour target (and optional depth) for the shadow map and
    // the post processing chain
    private static final class Target {
        int fbo;
        int tex;
        int depth;
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
    private PostProcess activePost;
    private final int[] savedFbo = new int[1];
    private final int[] savedViewport = new int[4];

    private static final class TexHandle {
        final int id;

        TexHandle(int id) {
            this.id = id;
        }
    }

    public GpuCapabilities getCapabilities() {
        if (caps == null) {
            int[] v = new int[1];
            gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_SIZE, v, 0);
            int maxTex = v[0] > 0 ? v[0] : 2048;
            gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_ATTRIBS, v, 0);
            int maxAttribs = v[0] > 0 ? v[0] : 8;
            String renderer = gl.glGetString(GL2ES2.GL_RENDERER);
            String version = gl.glGetString(GL2ES2.GL_VERSION);
            String name = "Codename One OpenGL (JavaSE/JOGL)";
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
        int[] ids = new int[1];
        gl.glGenTextures(1, ids, 0);
        int id = ids[0];
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, id);

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
        gl.glTexImage2D(GL2ES2.GL_TEXTURE_2D, 0, GL2ES2.GL_RGBA, w, h, 0,
                GL2ES2.GL_RGBA, GL2ES2.GL_UNSIGNED_BYTE, pixels);
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, 0);
        t.setHandle(new TexHandle(id));
        return t;
    }

    public void clear(int argbColor, boolean clearColor, boolean clearDepth) {
        int mask = 0;
        if (clearColor) {
            float a = ((argbColor >>> 24) & 0xff) / 255.0f;
            float r = ((argbColor >> 16) & 0xff) / 255.0f;
            float g = ((argbColor >> 8) & 0xff) / 255.0f;
            float b = (argbColor & 0xff) / 255.0f;
            gl.glClearColor(r, g, b, a);
            mask |= GL2ES2.GL_COLOR_BUFFER_BIT;
        }
        if (clearDepth) {
            gl.glClearDepthf(1.0f);
            gl.glDepthMask(true);
            mask |= GL2ES2.GL_DEPTH_BUFFER_BIT;
        }
        if (mask != 0) {
            gl.glClear(mask);
        }
    }

    public void setViewport(int x, int y, int w, int h) {
        gl.glViewport(x, y, w, h);
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
        if (program.handle == 0) {
            return;
        }
        gl.glUseProgram(program.handle);

        if (modelMatrix != null) {
            Matrix4.copy(modelMatrix, model);
        } else {
            Matrix4.setIdentity(model);
        }
        Camera cam = getCamera();
        float[] vp = cam != null ? cam.getViewProjection() : Matrix4.identity();
        Matrix4.multiply(vp, model, mvp);
        float[] nm = Matrix4.normalMatrix(model);
        for (int i = 0; i < 16; i++) {
            normalMatrix[i] = nm[i];
        }

        if (program.uMvp >= 0) {
            gl.glUniformMatrix4fv(program.uMvp, 1, false, mvp, 0);
        }
        if (program.uModel >= 0) {
            gl.glUniformMatrix4fv(program.uModel, 1, false, model, 0);
        }
        if (program.uNormalMatrix >= 0) {
            gl.glUniformMatrix4fv(program.uNormalMatrix, 1, false, normalMatrix, 0);
        }
        if (program.uColor >= 0) {
            int c = material.getColor();
            gl.glUniform4f(program.uColor,
                    ((c >> 16) & 0xff) / 255.0f, ((c >> 8) & 0xff) / 255.0f,
                    (c & 0xff) / 255.0f, ((c >>> 24) & 0xff) / 255.0f);
        }

        Light light = getLight();
        if (program.uLightDir >= 0) {
            gl.glUniform3f(program.uLightDir,
                    light.getDirectionX(), light.getDirectionY(), light.getDirectionZ());
        }
        if (program.uLightColor >= 0) {
            int lc = light.getColor();
            gl.glUniform3f(program.uLightColor,
                    ((lc >> 16) & 0xff) / 255.0f, ((lc >> 8) & 0xff) / 255.0f, (lc & 0xff) / 255.0f);
        }
        if (program.uAmbient >= 0) {
            int ac = light.getAmbientColor();
            gl.glUniform3f(program.uAmbient,
                    ((ac >> 16) & 0xff) / 255.0f, ((ac >> 8) & 0xff) / 255.0f, (ac & 0xff) / 255.0f);
        }
        if (program.uEye >= 0) {
            float ex = cam != null ? cam.getEyeX() : 0;
            float ey = cam != null ? cam.getEyeY() : 0;
            float ez = cam != null ? cam.getEyeZ() : 0;
            gl.glUniform3f(program.uEye, ex, ey, ez);
        }
        if (program.uShininess >= 0) {
            gl.glUniform1f(program.uShininess, material.getShininess());
        }
        applyEnvironment(program, material, env, shadowed);

        Texture tex = material.getTexture();
        if (tex != null && program.uTexture >= 0) {
            TexHandle th = (TexHandle) tex.getHandle();
            if (th != null) {
                gl.glActiveTexture(GL2ES2.GL_TEXTURE0);
                gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, th.id);
                int wrap = tex.getWrap() == Texture.Wrap.REPEAT
                        ? GL2ES2.GL_REPEAT : GL2ES2.GL_CLAMP_TO_EDGE;
                int filter = tex.getFilter() == Texture.Filter.NEAREST
                        ? GL2ES2.GL_NEAREST : GL2ES2.GL_LINEAR;
                gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_WRAP_S, wrap);
                gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_WRAP_T, wrap);
                gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_MIN_FILTER, filter);
                gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_MAG_FILTER, filter);
                gl.glUniform1i(program.uTexture, 0);
            }
        }

        applyRenderState(material.getRenderState());

        int vbo = uploadVertexBuffer(vb);
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vbo);
        int strideBytes = fmt.getStrideBytes();
        bindAttribute(program.aPosition, fmt, VertexAttribute.Usage.POSITION, strideBytes);
        bindAttribute(program.aNormal, fmt, VertexAttribute.Usage.NORMAL, strideBytes);
        bindAttribute(program.aTexcoord, fmt, VertexAttribute.Usage.TEXCOORD, strideBytes);

        int glMode = toGlPrimitive(mesh.getPrimitiveType());
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            int ibo = uploadIndexBuffer(ib);
            gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, ibo);
            gl.glDrawElements(glMode, ib.getIndexCount(), GL2ES2.GL_UNSIGNED_SHORT, 0L);
            gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            gl.glDrawArrays(glMode, 0, vb.getVertexCount());
        }

        disableAttribute(program.aPosition);
        disableAttribute(program.aNormal);
        disableAttribute(program.aTexcoord);
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0);
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
            gl.glDisableVertexAttribArray(location);
            return;
        }
        gl.glEnableVertexAttribArray(location);
        gl.glVertexAttribPointer(location, components, GL2ES2.GL_FLOAT, false,
                strideBytes, (long) (offsetFloats * 4));
    }

    private void disableAttribute(int location) {
        if (location >= 0) {
            gl.glDisableVertexAttribArray(location);
        }
    }

    private void applyRenderState(RenderState rs) {
        if (rs.isDepthTest()) {
            gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        } else {
            gl.glDisable(GL2ES2.GL_DEPTH_TEST);
        }
        gl.glDepthMask(rs.isDepthWrite());

        RenderState.BlendMode blend = rs.getBlendMode();
        if (blend == RenderState.BlendMode.NONE) {
            gl.glDisable(GL2ES2.GL_BLEND);
        } else {
            gl.glEnable(GL2ES2.GL_BLEND);
            // The alpha channel accumulates coverage rather than being
            // weighted by itself, so an opaque frame stays opaque
            // (RenderState.BlendMode's contract). The frame is read back
            // with its alpha and painted over the form: with one glBlendFunc
            // a 40% quad left 0.76 there and the form showed through it.
            if (blend == RenderState.BlendMode.ADDITIVE) {
                gl.glBlendFuncSeparate(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE,
                        GL2ES2.GL_ONE, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                gl.glBlendFuncSeparate(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA,
                        GL2ES2.GL_ONE, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

        RenderState.CullMode cull = rs.getCullMode();
        if (cull == RenderState.CullMode.NONE) {
            gl.glDisable(GL2ES2.GL_CULL_FACE);
        } else {
            gl.glEnable(GL2ES2.GL_CULL_FACE);
            gl.glFrontFace(GL2ES2.GL_CCW);
            gl.glCullFace(cull == RenderState.CullMode.FRONT
                    ? GL2ES2.GL_FRONT : GL2ES2.GL_BACK);
        }
    }

    private static int toGlPrimitive(PrimitiveType type) {
        switch (type) {
            case POINTS:
                return GL2ES2.GL_POINTS;
            case LINES:
                return GL2ES2.GL_LINES;
            case LINE_STRIP:
                return GL2ES2.GL_LINE_STRIP;
            case TRIANGLE_STRIP:
                return GL2ES2.GL_TRIANGLE_STRIP;
            case TRIANGLES:
            default:
                return GL2ES2.GL_TRIANGLES;
        }
    }

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
            gl.glGenBuffers(1, ids, 0);
            h = new VboHandle(ids[0]);
            vb.setHandle(h);
        }
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, h.id);
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
            gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, (long) floatCount * 4, h.view, GL2ES2.GL_STATIC_DRAW);
            vb.clearDirty();
        }
        return h.id;
    }

    private int uploadIndexBuffer(IndexBuffer ib) {
        IboHandle h = (IboHandle) ib.getHandle();
        if (h == null) {
            int[] ids = new int[1];
            gl.glGenBuffers(1, ids, 0);
            h = new IboHandle(ids[0]);
            ib.setHandle(h);
        }
        gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, h.id);
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
            gl.glBufferData(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, (long) indexCount * 2, h.view, GL2ES2.GL_STATIC_DRAW);
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

    private Program buildProgram(String vertexSrc, String fragmentSrc) {
        int handle = linkProgram(vertexSrc, fragmentSrc);
        Program p = new Program();
        p.handle = handle;
        if (handle != 0) {
            p.aPosition = gl.glGetAttribLocation(handle, GlslShaderGenerator.A_POSITION);
            p.aNormal = gl.glGetAttribLocation(handle, GlslShaderGenerator.A_NORMAL);
            p.aTexcoord = gl.glGetAttribLocation(handle, GlslShaderGenerator.A_TEXCOORD);
            p.uMvp = gl.glGetUniformLocation(handle, "u_mvp");
            p.uModel = gl.glGetUniformLocation(handle, "u_model");
            p.uNormalMatrix = gl.glGetUniformLocation(handle, "u_normalMatrix");
            p.uColor = gl.glGetUniformLocation(handle, "u_color");
            p.uTexture = gl.glGetUniformLocation(handle, "u_texture");
            p.uLightDir = gl.glGetUniformLocation(handle, "u_lightDir");
            p.uLightColor = gl.glGetUniformLocation(handle, "u_lightColor");
            p.uAmbient = gl.glGetUniformLocation(handle, "u_ambient");
            p.uEye = gl.glGetUniformLocation(handle, "u_eye");
            p.uShininess = gl.glGetUniformLocation(handle, "u_shininess");
        }
        return p;
    }

    private int loc(Program p, String name) {
        Integer cached = p.uniforms.get(name);
        if (cached == null) {
            cached = Integer.valueOf(p.handle == 0 ? -1 : gl.glGetUniformLocation(p.handle, name));
            p.uniforms.put(name, cached);
        }
        return cached.intValue();
    }

    private void uniformColor(Program p, String name, int argb) {
        int l = loc(p, name);
        if (l >= 0) {
            gl.glUniform3f(l, ((argb >> 16) & 0xff) / 255.0f, ((argb >> 8) & 0xff) / 255.0f,
                    (argb & 0xff) / 255.0f);
        }
    }

    private void uniform1(Program p, String name, float v) {
        int l = loc(p, name);
        if (l >= 0) {
            gl.glUniform1f(l, v);
        }
    }

    private void uniform2(Program p, String name, float a, float b) {
        int l = loc(p, name);
        if (l >= 0) {
            gl.glUniform2f(l, a, b);
        }
    }

    private void uniform3(Program p, String name, float a, float b, float c) {
        int l = loc(p, name);
        if (l >= 0) {
            gl.glUniform3f(l, a, b, c);
        }
    }

    private void sampler(Program p, String name, int unit, int tex) {
        int l = loc(p, name);
        if (l >= 0) {
            gl.glActiveTexture(GL2ES2.GL_TEXTURE0 + unit);
            gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, tex);
            gl.glUniform1i(l, unit);
            gl.glActiveTexture(GL2ES2.GL_TEXTURE0);
        }
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
                gl.glUniform4f(l, ((sun >> 16) & 0xff) / 255f, ((sun >> 8) & 0xff) / 255f, (sun & 0xff) / 255f,
                        env.getCloudCover());
            }
        }
        uniformColor(p, "u_emissive", material.getEmissive());
        if (shadowed) {
            int l = loc(p, "u_shadowMatrix");
            if (l >= 0) {
                gl.glUniformMatrix4fv(l, 1, false, shadowMatrix, 0);
            }
            Light light = getLight();
            uniform3(p, "u_shadow", light.getShadowBias(), light.getShadowStrength(),
                    1f / shadowTarget.width);
            sampler(p, "u_shadowMap", 1, shadowTarget.tex);
        }
    }

    private Target target(Target t, int w, int h, boolean depth) {
        return target(t, w, h, depth, GL2ES2.GL_LINEAR);
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
        gl.glGenTextures(1, ids, 0);
        n.tex = ids[0];
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, n.tex);
        gl.glTexImage2D(GL2ES2.GL_TEXTURE_2D, 0, GL2ES2.GL_RGBA, w, h, 0, GL2ES2.GL_RGBA,
                GL2ES2.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_MIN_FILTER, filter);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_MAG_FILTER, filter);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2ES2.GL_TEXTURE_2D, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_EDGE);
        gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, 0);
        gl.glGenFramebuffers(1, ids, 0);
        n.fbo = ids[0];
        gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, n.fbo);
        gl.glFramebufferTexture2D(GL2ES2.GL_FRAMEBUFFER, GL2ES2.GL_COLOR_ATTACHMENT0,
                GL2ES2.GL_TEXTURE_2D, n.tex, 0);
        if (depth) {
            gl.glGenRenderbuffers(1, ids, 0);
            n.depth = ids[0];
            gl.glBindRenderbuffer(GL2ES2.GL_RENDERBUFFER, n.depth);
            gl.glRenderbufferStorage(GL2ES2.GL_RENDERBUFFER, GL2ES2.GL_DEPTH_COMPONENT16, w, h);
            gl.glFramebufferRenderbuffer(GL2ES2.GL_FRAMEBUFFER, GL2ES2.GL_DEPTH_ATTACHMENT,
                    GL2ES2.GL_RENDERBUFFER, n.depth);
            gl.glBindRenderbuffer(GL2ES2.GL_RENDERBUFFER, 0);
        }
        return n;
    }

    private void releaseTarget(Target t) {
        if (t == null || gl == null) {
            return;
        }
        gl.glDeleteFramebuffers(1, new int[]{t.fbo}, 0);
        gl.glDeleteTextures(1, new int[]{t.tex}, 0);
        if (t.depth != 0) {
            gl.glDeleteRenderbuffers(1, new int[]{t.depth}, 0);
        }
    }

    // The panel renders into its own framebuffer, so "the screen" is whatever
    // was bound when a pass began, never framebuffer 0.
    private void saveTarget() {
        gl.glGetIntegerv(GL2ES2.GL_FRAMEBUFFER_BINDING, savedFbo, 0);
        gl.glGetIntegerv(GL2ES2.GL_VIEWPORT, savedViewport, 0);
    }

    private void restoreTarget() {
        gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, savedFbo[0]);
        gl.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }

    public boolean beginShadowPass(Light light) {
        if (gl == null) {
            return false;
        }
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
        shadowTarget = target(shadowTarget, size, size, true, GL2ES2.GL_NEAREST);
        gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, shadowTarget.fbo);
        gl.glViewport(0, 0, size, size);
        gl.glClearColor(1f, 1f, 1f, 1f);
        gl.glClearDepthf(1f);
        gl.glDepthMask(true);
        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_DEPTH_BUFFER_BIT);
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
        restoreTarget();
    }

    private void drawShadowCaster(Mesh mesh, Material material, float[] modelMatrix) {
        if (!material.isCastShadows() || material.getRenderState().getBlendMode() != RenderState.BlendMode.NONE
                || material.getType() == Material.Type.SKYBOX) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        gl.glUseProgram(shadowProgram.handle);
        if (modelMatrix != null) {
            Matrix4.copy(modelMatrix, model);
        } else {
            Matrix4.setIdentity(model);
        }
        Matrix4.multiply(shadowMatrix, model, mvp);
        gl.glUniformMatrix4fv(shadowProgram.uMvp, 1, false, mvp, 0);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDisable(GL2ES2.GL_BLEND);
        gl.glDisable(GL2ES2.GL_CULL_FACE);
        int vbo = uploadVertexBuffer(vb);
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vbo);
        bindAttribute(shadowProgram.aPosition, fmt, VertexAttribute.Usage.POSITION, fmt.getStrideBytes());
        int glMode = toGlPrimitive(mesh.getPrimitiveType());
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            int ibo = uploadIndexBuffer(ib);
            gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, ibo);
            gl.glDrawElements(glMode, ib.getIndexCount(), GL2ES2.GL_UNSIGNED_SHORT, 0L);
            gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            gl.glDrawArrays(glMode, 0, vb.getVertexCount());
        }
        disableAttribute(shadowProgram.aPosition);
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0);
    }

    public boolean beginPostProcess(PostProcess effects, int width, int height) {
        if (gl == null || effects == null) {
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
        gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, sceneTarget.fbo);
        gl.glViewport(0, 0, w, h);
        activePost = effects;
        return true;
    }

    public void endPostProcess(float timeSeconds) {
        PostProcess pp = activePost;
        if (pp == null) {
            return;
        }
        activePost = null;
        gl.glDisable(GL2ES2.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDisable(GL2ES2.GL_BLEND);
        gl.glDisable(GL2ES2.GL_CULL_FACE);
        int bloomTex = sceneTarget.tex;
        if (pp.isBloom()) {
            passInto(bloomA);
            gl.glUseProgram(brightProgram.handle);
            sampler(brightProgram, "u_scene", 0, sceneTarget.tex);
            uniform1(brightProgram, "u_threshold", pp.getBloomThreshold());
            uniform2(brightProgram, "u_texel", 1f / sceneTarget.width, 1f / sceneTarget.height);
            drawQuad(brightProgram);
            for (int i = 0; i < 2; i++) {
                float spread = 1f + i;
                passInto(bloomB);
                gl.glUseProgram(blurProgram.handle);
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
        gl.glUseProgram(c.handle);
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
        drawQuad(c);
        gl.glDepthMask(true);
    }

    private void passInto(Target t) {
        gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, t.fbo);
        gl.glViewport(0, 0, t.width, t.height);
    }

    private void drawQuad(Program p) {
        if (quadVbo == 0) {
            int[] ids = new int[1];
            gl.glGenBuffers(1, ids, 0);
            quadVbo = ids[0];
            FloatBuffer fb = ByteBuffer.allocateDirect(12 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            fb.put(new float[]{-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f});
            fb.position(0);
            gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, quadVbo);
            gl.glBufferData(GL2ES2.GL_ARRAY_BUFFER, 12L * 4, fb, GL2ES2.GL_STATIC_DRAW);
        }
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, quadVbo);
        gl.glEnableVertexAttribArray(p.aPosition);
        gl.glVertexAttribPointer(p.aPosition, 3, GL2ES2.GL_FLOAT, false, 12, 0L);
        gl.glDrawArrays(GL2ES2.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisableVertexAttribArray(p.aPosition);
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0);
    }

    private int linkProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GL2ES2.GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GL2ES2.GL_FRAGMENT_SHADER, fragmentSrc);
        if (vs == 0 || fs == 0) {
            return 0;
        }
        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vs);
        gl.glAttachShader(program, fs);
        gl.glLinkProgram(program);
        int[] status = new int[1];
        gl.glGetProgramiv(program, GL2ES2.GL_LINK_STATUS, status, 0);
        gl.glDeleteShader(vs);
        gl.glDeleteShader(fs);
        if (status[0] == 0) {
            System.out.println("CN1Gpu: program link failed: " + getProgramInfoLog(program));
            gl.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    /// Adapts the shared GLSL ES 1.00 source (written for WebGL / GLES, where it
    /// has no `#version` and uses `precision` qualifiers) to the desktop GLSL the
    /// JOGL context provides. Desktop GLSL 1.20 understands `attribute`,
    /// `varying`, `gl_FragColor` and `texture2D`, but requires a `#version`
    /// directive and rejects `precision` qualifiers, so prepend `#version 120`
    /// and drop the precision lines.
    private static String toDesktopGlsl(String src) {
        StringBuilder sb = new StringBuilder("#version 120\n");
        int start = 0;
        int len = src.length();
        while (start < len) {
            int nl = src.indexOf('\n', start);
            int end = nl < 0 ? len : nl;
            String line = src.substring(start, end);
            if (!line.trim().startsWith("precision ")) {
                sb.append(line).append('\n');
            }
            if (nl < 0) {
                break;
            }
            start = nl + 1;
        }
        return sb.toString();
    }

    private int compileShader(int type, String source) {
        source = toDesktopGlsl(source);
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[]{source}, new int[]{source.length()}, 0);
        gl.glCompileShader(shader);
        int[] status = new int[1];
        gl.glGetShaderiv(shader, GL2ES2.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            System.out.println("CN1Gpu: shader compile failed: " + getShaderInfoLog(shader) + "\n" + source);
            gl.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private String getShaderInfoLog(int shader) {
        int[] len = new int[1];
        byte[] log = new byte[2048];
        gl.glGetShaderInfoLog(shader, log.length, len, 0, log, 0);
        return new String(log, 0, Math.max(0, len[0]));
    }

    private String getProgramInfoLog(int program) {
        int[] len = new int[1];
        byte[] log = new byte[2048];
        gl.glGetProgramInfoLog(program, log.length, len, 0, log, 0);
        return new String(log, 0, Math.max(0, len[0]));
    }

    public void dispose(VertexBuffer buffer) {
        VboHandle h = (VboHandle) buffer.getHandle();
        if (h != null && gl != null) {
            gl.glDeleteBuffers(1, new int[]{h.id}, 0);
            buffer.setHandle(null);
        }
    }

    public void dispose(IndexBuffer buffer) {
        IboHandle h = (IboHandle) buffer.getHandle();
        if (h != null && gl != null) {
            gl.glDeleteBuffers(1, new int[]{h.id}, 0);
            buffer.setHandle(null);
        }
    }

    public void dispose(Texture texture) {
        TexHandle h = (TexHandle) texture.getHandle();
        if (h != null && gl != null) {
            gl.glDeleteTextures(1, new int[]{h.id}, 0);
            texture.setHandle(null);
        }
    }

    /// Releases all cached GL programs; called when the drawable is disposed and
    /// the GL context goes away.
    void disposePrograms() {
        if (gl != null) {
            for (Program p : programs.values()) {
                if (p.handle != 0) {
                    gl.glDeleteProgram(p.handle);
                }
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
