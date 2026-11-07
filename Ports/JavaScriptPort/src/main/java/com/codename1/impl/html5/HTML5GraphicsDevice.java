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
package com.codename1.impl.html5;

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
import com.codename1.html5.js.webgl.WebGLBuffer;
import com.codename1.html5.js.webgl.WebGLFramebuffer;
import com.codename1.html5.js.webgl.WebGLProgram;
import com.codename1.html5.js.webgl.WebGLRenderbuffer;
import com.codename1.html5.js.webgl.WebGLRenderingContext;
import com.codename1.html5.js.webgl.WebGLShader;
import com.codename1.html5.js.webgl.WebGLTexture;
import com.codename1.html5.js.webgl.WebGLUniformLocation;
import com.codename1.ui.Image;

import java.util.HashMap;
import java.util.Map;

/// WebGL backed `GraphicsDevice` for the browser port. The device wraps a
/// `WebGLRenderingContext` obtained from an `HTMLCanvasElement` and uses the
/// shared core `GlslShaderGenerator` to emit GLSL ES 1.00 (WebGL 1 is OpenGL ES
/// 2, so the generated source runs unmodified). Programs are cached per material
/// shader key and vertex format; vertex and index buffers are uploaded lazily
/// when dirty and textures are uploaded from ARGB pixel data.
///
/// IMPORTANT: the translated app runs in a Web Worker, so all WebGL calls go
/// through the `WebGLRenderingContext` JSO interface, which the bridge dispatches
/// against the real context on the browser main thread. Bulk payloads
/// (vertex/index/pixel/uniform data) are passed as plain Java primitive arrays;
/// the bridge serializes those across to the main thread and re-wraps them in the
/// correct typed array before the real GL call (a worker-built typed array would
/// arrive on the main thread as an empty object). GL enum values are spec-fixed
/// and passed as numeric literals to avoid a bridge round-trip per `gl.CONSTANT`
/// read.
class HTML5GraphicsDevice extends GraphicsDevice {
    // WebGL enum values (fixed by the OpenGL ES 2 / WebGL 1 spec).
    private static final int DEPTH_BUFFER_BIT = 0x0100;
    private static final int COLOR_BUFFER_BIT = 0x4000;
    private static final int POINTS = 0x0000;
    private static final int LINES = 0x0001;
    private static final int LINE_STRIP = 0x0003;
    private static final int TRIANGLES = 0x0004;
    private static final int TRIANGLE_STRIP = 0x0005;
    private static final int CULL_FACE = 0x0B44;
    private static final int DEPTH_TEST = 0x0B71;
    private static final int BLEND = 0x0BE2;
    private static final int FRONT = 0x0404;
    private static final int BACK = 0x0405;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int UNSIGNED_SHORT = 0x1403;
    private static final int FLOAT = 0x1406;
    private static final int RGBA = 0x1908;
    private static final int FRAGMENT_SHADER = 0x8B30;
    private static final int VERTEX_SHADER = 0x8B31;
    private static final int COMPILE_STATUS = 0x8B81;
    private static final int LINK_STATUS = 0x8B82;
    private static final int ARRAY_BUFFER = 0x8892;
    private static final int ELEMENT_ARRAY_BUFFER = 0x8893;
    private static final int STATIC_DRAW = 0x88E4;
    private static final int TEXTURE0 = 0x84C0;
    private static final int TEXTURE_MAG_FILTER = 0x2800;
    private static final int TEXTURE_MIN_FILTER = 0x2801;
    private static final int TEXTURE_WRAP_S = 0x2802;
    private static final int TEXTURE_WRAP_T = 0x2803;
    private static final int NEAREST = 0x2600;
    private static final int LINEAR = 0x2601;
    private static final int REPEAT = 0x2901;
    private static final int CLAMP_TO_EDGE = 0x812F;
    private static final int MAX_TEXTURE_SIZE = 0x0D33;
    private static final int MAX_VERTEX_ATTRIBS = 0x8869;
    private static final int FRAMEBUFFER = 0x8D40;
    private static final int RENDERBUFFER = 0x8D41;
    private static final int COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int DEPTH_ATTACHMENT = 0x8D00;
    private static final int DEPTH_COMPONENT16 = 0x81A5;

    private final WebGLRenderingContext gl;
    private final Map<String, ProgramEntry> programs = new HashMap<String, ProgramEntry>();
    private final GpuCapabilities capabilities;

    // an offscreen colour target (and optional depth) for the shadow map and
    // the post processing chain
    private static final class Target {
        WebGLFramebuffer fbo;
        WebGLTexture tex;
        WebGLRenderbuffer depth;
        int width;
        int height;
    }

    private ProgramEntry shadowProgram;
    private ProgramEntry brightProgram;
    private ProgramEntry blurProgram;
    private ProgramEntry compositeProgram;
    private Target shadowTarget;
    private Target sceneTarget;
    private Target bloomA;
    private Target bloomB;
    private WebGLBuffer quadVbo;
    private boolean inShadowPass;
    private boolean shadowReady;
    private final float[] shadowMatrix = new float[16];
    private PostProcess activePost;
    // The canvas is the default framebuffer (null). The viewport is tracked
    // here because reading it back would need a typed array across the bridge.
    private final int[] viewport = new int[4];
    private final int[] savedViewport = new int[4];

    /// Cached compiled program together with the locations the device binds.
    /// Attribute locations are GLint indices (-1 when absent); uniform locations
    /// are opaque handles (null when absent).
    private static final class ProgramEntry {
        WebGLProgram program;
        int aPosition = -1;
        int aNormal = -1;
        int aTexcoord = -1;
        WebGLUniformLocation uMvp;
        WebGLUniformLocation uModel;
        WebGLUniformLocation uNormalMatrix;
        WebGLUniformLocation uColor;
        WebGLUniformLocation uTexture;
        WebGLUniformLocation uLightDir;
        WebGLUniformLocation uLightColor;
        WebGLUniformLocation uAmbient;
        WebGLUniformLocation uEye;
        WebGLUniformLocation uShininess;
        final Map<String, WebGLUniformLocation> uniforms = new HashMap<String, WebGLUniformLocation>();
    }

    HTML5GraphicsDevice(WebGLRenderingContext gl) {
        this.gl = gl;
        int maxTex = gl.getParameter(MAX_TEXTURE_SIZE);
        int maxAttribs = gl.getParameter(MAX_VERTEX_ATTRIBS);
        if (maxTex <= 0) {
            maxTex = 2048;
        }
        if (maxAttribs <= 0) {
            maxAttribs = 8;
        }
        this.capabilities = new GpuCapabilities(maxTex, maxAttribs, false, false, false, "WebGL", true, true);
    }

    public GpuCapabilities getCapabilities() {
        return capabilities;
    }

    public Texture createTexture(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("image is required");
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int[] argb = image.getRGB();
        return createTexture(w, h, argb);
    }

    public Texture createTexture(int width, int height, int[] argb) {
        Texture texture = new Texture(width, height);
        WebGLTexture handle = gl.createTexture();
        gl.bindTexture(TEXTURE_2D, handle);
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            int c = argb[i];
            int o = i * 4;
            rgba[o] = (byte) ((c >> 16) & 0xff);
            rgba[o + 1] = (byte) ((c >> 8) & 0xff);
            rgba[o + 2] = (byte) (c & 0xff);
            rgba[o + 3] = (byte) ((c >>> 24) & 0xff);
        }
        gl.texImage2D(TEXTURE_2D, 0, RGBA, width, height, 0, RGBA, UNSIGNED_BYTE, rgba);
        boolean linear = texture.getFilter() == Texture.Filter.LINEAR;
        boolean repeat = texture.getWrap() == Texture.Wrap.REPEAT;
        int f = linear ? LINEAR : NEAREST;
        gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, f);
        gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, f);
        int wrap = repeat ? REPEAT : CLAMP_TO_EDGE;
        gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, wrap);
        gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, wrap);
        gl.bindTexture(TEXTURE_2D, null);
        texture.setHandle(handle);
        return texture;
    }

    public void clear(int argbColor, boolean color, boolean depth) {
        float a = ((argbColor >>> 24) & 0xff) / 255f;
        float r = ((argbColor >> 16) & 0xff) / 255f;
        float g = ((argbColor >> 8) & 0xff) / 255f;
        float b = (argbColor & 0xff) / 255f;
        gl.clearColor(r, g, b, a);
        if (depth) {
            gl.depthMask(true);
        }
        int mask = 0;
        if (color) {
            mask |= COLOR_BUFFER_BIT;
        }
        if (depth) {
            mask |= DEPTH_BUFFER_BIT;
        }
        gl.clear(mask);
    }

    public void setViewport(int x, int y, int width, int height) {
        viewport[0] = x;
        viewport[1] = y;
        viewport[2] = width;
        viewport[3] = height;
        gl.viewport(x, y, width, height);
    }

    public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        VertexBuffer vertices = mesh.getVertices();
        VertexFormat format = vertices.getFormat();
        if (inShadowPass) {
            drawShadowCaster(mesh, material, modelMatrix);
            return;
        }
        Environment env = getEnvironment();
        boolean shadowed = shadowReady && getLight().isShadows() && material.isLit()
                && material.isReceiveShadows();
        ProgramEntry entry = getProgram(material, format, env, shadowed);
        gl.useProgram(entry.program);
        applyEnvironment(entry, material, env, shadowed);

        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        Camera camera = getCamera();
        float[] mvp = Matrix4.identity();
        if (camera != null) {
            Matrix4.multiply(camera.getViewProjection(), model, mvp);
        } else {
            Matrix4.copy(model, mvp);
        }
        if (entry.uMvp != null) {
            gl.uniformMatrix4fv(entry.uMvp, false, mvp);
        }
        if (entry.uModel != null) {
            gl.uniformMatrix4fv(entry.uModel, false, model);
        }
        if (entry.uNormalMatrix != null) {
            gl.uniformMatrix4fv(entry.uNormalMatrix, false, Matrix4.normalMatrix(model));
        }
        if (entry.uColor != null) {
            int c = material.getColor();
            gl.uniform4f(entry.uColor,
                    ((c >> 16) & 0xff) / 255f,
                    ((c >> 8) & 0xff) / 255f,
                    (c & 0xff) / 255f,
                    ((c >>> 24) & 0xff) / 255f);
        }
        Light light = getLight();
        if (light != null) {
            if (entry.uLightDir != null) {
                gl.uniform3f(entry.uLightDir,
                        light.getDirectionX(), light.getDirectionY(), light.getDirectionZ());
            }
            if (entry.uLightColor != null) {
                int lc = light.getColor();
                gl.uniform3f(entry.uLightColor,
                        ((lc >> 16) & 0xff) / 255f, ((lc >> 8) & 0xff) / 255f, (lc & 0xff) / 255f);
            }
            if (entry.uAmbient != null) {
                int ac = light.getAmbientColor();
                gl.uniform3f(entry.uAmbient,
                        ((ac >> 16) & 0xff) / 255f, ((ac >> 8) & 0xff) / 255f, (ac & 0xff) / 255f);
            }
        }
        if (entry.uEye != null && camera != null) {
            gl.uniform3f(entry.uEye, camera.getEyeX(), camera.getEyeY(), camera.getEyeZ());
        }
        if (entry.uShininess != null) {
            gl.uniform1f(entry.uShininess, material.getShininess());
        }

        Texture texture = material.getTexture();
        if (texture != null && entry.uTexture != null && texture.getHandle() instanceof WebGLTexture) {
            gl.activeTexture(TEXTURE0);
            gl.bindTexture(TEXTURE_2D, (WebGLTexture) texture.getHandle());
            // Apply the texture's current filter/wrap at bind time so the fluent
            // setFilter/setWrap setters take effect even when called after the
            // texture was created.
            int f = texture.getFilter() == Texture.Filter.LINEAR ? LINEAR : NEAREST;
            gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, f);
            gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, f);
            int wrap = texture.getWrap() == Texture.Wrap.REPEAT ? REPEAT : CLAMP_TO_EDGE;
            gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, wrap);
            gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, wrap);
            gl.uniform1i(entry.uTexture, 0);
        }

        applyRenderState(material.getRenderState());

        WebGLBuffer vbo = uploadVertexBuffer(vertices);
        gl.bindBuffer(ARRAY_BUFFER, vbo);
        bindAttributes(entry, format);

        PrimitiveType pt = mesh.getPrimitiveType();
        int mode = toGlPrimitive(pt);
        if (mesh.isIndexed()) {
            IndexBuffer indices = mesh.getIndices();
            WebGLBuffer ibo = uploadIndexBuffer(indices);
            gl.bindBuffer(ELEMENT_ARRAY_BUFFER, ibo);
            gl.drawElements(mode, indices.getIndexCount(), UNSIGNED_SHORT, 0);
        } else {
            gl.drawArrays(mode, 0, vertices.getVertexCount());
        }
    }

    private void bindAttributes(ProgramEntry entry, VertexFormat format) {
        int strideBytes = format.getStrideBytes();
        int count = format.getAttributeCount();
        for (int i = 0; i < count; i++) {
            VertexAttribute attr = format.getAttribute(i);
            int offsetBytes = format.getAttributeOffset(i) * 4;
            int loc = -1;
            switch (attr.getUsage()) {
                case POSITION:
                    loc = entry.aPosition;
                    break;
                case NORMAL:
                    loc = entry.aNormal;
                    break;
                case TEXCOORD:
                    loc = entry.aTexcoord;
                    break;
                default:
                    loc = -1;
                    break;
            }
            if (loc >= 0) {
                gl.enableVertexAttribArray(loc);
                gl.vertexAttribPointer(loc, attr.getComponents(), FLOAT, false, strideBytes, offsetBytes);
            }
        }
    }

    private WebGLBuffer uploadVertexBuffer(VertexBuffer buffer) {
        WebGLBuffer handle = buffer.getHandle() instanceof WebGLBuffer ? (WebGLBuffer) buffer.getHandle() : null;
        if (handle == null) {
            handle = gl.createBuffer();
            buffer.setHandle(handle);
            buffer.setDirty();
        }
        if (buffer.isDirty()) {
            gl.bindBuffer(ARRAY_BUFFER, handle);
            // The backing array may be SIMD-overallocated; upload exactly the
            // used float range so the GL buffer matches the vertex count.
            float[] data = buffer.getData();
            int count = buffer.getFloatCount();
            if (data.length != count) {
                float[] trimmed = new float[count];
                System.arraycopy(data, 0, trimmed, 0, count);
                data = trimmed;
            }
            gl.bufferData(ARRAY_BUFFER, data, STATIC_DRAW);
            buffer.clearDirty();
        }
        return handle;
    }

    private WebGLBuffer uploadIndexBuffer(IndexBuffer buffer) {
        WebGLBuffer handle = buffer.getHandle() instanceof WebGLBuffer ? (WebGLBuffer) buffer.getHandle() : null;
        if (handle == null) {
            handle = gl.createBuffer();
            buffer.setHandle(handle);
            buffer.setDirty();
        }
        if (buffer.isDirty()) {
            gl.bindBuffer(ELEMENT_ARRAY_BUFFER, handle);
            short[] data = buffer.getData();
            int count = buffer.getIndexCount();
            if (data.length != count) {
                short[] trimmed = new short[count];
                System.arraycopy(data, 0, trimmed, 0, count);
                data = trimmed;
            }
            gl.bufferData(ELEMENT_ARRAY_BUFFER, data, STATIC_DRAW);
            buffer.clearDirty();
        }
        return handle;
    }

    private void applyRenderState(RenderState state) {
        if (state.isDepthTest()) {
            gl.enable(DEPTH_TEST);
        } else {
            gl.disable(DEPTH_TEST);
        }
        gl.depthMask(state.isDepthWrite());

        RenderState.BlendMode blend = state.getBlendMode();
        if (blend == RenderState.BlendMode.NONE) {
            gl.disable(BLEND);
        } else {
            gl.enable(BLEND);
            // Separate factors for the alpha channel, so that an opaque
            // frame stays opaque: see JavaScriptBlendFactors.
            boolean additive = blend == RenderState.BlendMode.ADDITIVE;
            gl.blendFuncSeparate(JavaScriptBlendFactors.sourceColor(additive),
                    JavaScriptBlendFactors.destinationColor(additive),
                    JavaScriptBlendFactors.sourceAlpha(additive),
                    JavaScriptBlendFactors.destinationAlpha(additive));
        }

        RenderState.CullMode cull = state.getCullMode();
        if (cull == RenderState.CullMode.NONE) {
            gl.disable(CULL_FACE);
        } else {
            gl.enable(CULL_FACE);
            gl.cullFace(cull == RenderState.CullMode.FRONT ? FRONT : BACK);
        }
    }

    private int toGlPrimitive(PrimitiveType pt) {
        switch (pt) {
            case POINTS:
                return POINTS;
            case LINES:
                return LINES;
            case LINE_STRIP:
                return LINE_STRIP;
            case TRIANGLE_STRIP:
                return TRIANGLE_STRIP;
            case TRIANGLES:
            default:
                return TRIANGLES;
        }
    }

    private ProgramEntry getProgram(Material material, VertexFormat format, Environment env, boolean shadowed) {
        String key = material.getShaderKey() + "|" + GlslShaderGenerator.variantKey(env, shadowed)
                + "|" + formatKey(format);
        ProgramEntry entry = programs.get(key);
        if (entry != null) {
            return entry;
        }
        GlslShaderGenerator gen = new GlslShaderGenerator(material, format, env, shadowed);
        entry = buildProgram(gen.getVertexSource(), gen.getFragmentSource());
        programs.put(key, entry);
        return entry;
    }

    private ProgramEntry buildProgram(String vertexSrc, String fragmentSrc) {
        WebGLShader vs = compileShader(VERTEX_SHADER, vertexSrc);
        WebGLShader fs = compileShader(FRAGMENT_SHADER, fragmentSrc);
        WebGLProgram program = gl.createProgram();
        gl.attachShader(program, vs);
        gl.attachShader(program, fs);
        gl.linkProgram(program);
        if (!gl.getProgramParameter(program, LINK_STATUS)) {
            String log = gl.getProgramInfoLog(program);
            throw new RuntimeException("WebGL program link failed: " + log);
        }
        ProgramEntry entry = new ProgramEntry();
        entry.program = program;
        entry.aPosition = gl.getAttribLocation(program, GlslShaderGenerator.A_POSITION);
        entry.aNormal = gl.getAttribLocation(program, GlslShaderGenerator.A_NORMAL);
        entry.aTexcoord = gl.getAttribLocation(program, GlslShaderGenerator.A_TEXCOORD);
        entry.uMvp = gl.getUniformLocation(program, "u_mvp");
        entry.uModel = gl.getUniformLocation(program, "u_model");
        entry.uNormalMatrix = gl.getUniformLocation(program, "u_normalMatrix");
        entry.uColor = gl.getUniformLocation(program, "u_color");
        entry.uTexture = gl.getUniformLocation(program, "u_texture");
        entry.uLightDir = gl.getUniformLocation(program, "u_lightDir");
        entry.uLightColor = gl.getUniformLocation(program, "u_lightColor");
        entry.uAmbient = gl.getUniformLocation(program, "u_ambient");
        entry.uEye = gl.getUniformLocation(program, "u_eye");
        entry.uShininess = gl.getUniformLocation(program, "u_shininess");
        return entry;
    }

    private WebGLUniformLocation loc(ProgramEntry p, String name) {
        if (p.uniforms.containsKey(name)) {
            return p.uniforms.get(name);
        }
        WebGLUniformLocation l = gl.getUniformLocation(p.program, name);
        p.uniforms.put(name, l);
        return l;
    }

    private void uniformColor(ProgramEntry p, String name, int argb) {
        WebGLUniformLocation l = loc(p, name);
        if (l != null) {
            gl.uniform3f(l, ((argb >> 16) & 0xff) / 255.0f, ((argb >> 8) & 0xff) / 255.0f,
                    (argb & 0xff) / 255.0f);
        }
    }

    private void uniform1(ProgramEntry p, String name, float v) {
        WebGLUniformLocation l = loc(p, name);
        if (l != null) {
            gl.uniform1f(l, v);
        }
    }

    private void uniform2(ProgramEntry p, String name, float a, float b) {
        WebGLUniformLocation l = loc(p, name);
        if (l != null) {
            gl.uniform2f(l, a, b);
        }
    }

    private void uniform3(ProgramEntry p, String name, float a, float b, float c) {
        WebGLUniformLocation l = loc(p, name);
        if (l != null) {
            gl.uniform3f(l, a, b, c);
        }
    }

    private void sampler(ProgramEntry p, String name, int unit, WebGLTexture tex) {
        WebGLUniformLocation l = loc(p, name);
        if (l != null) {
            gl.activeTexture(TEXTURE0 + unit);
            gl.bindTexture(TEXTURE_2D, tex);
            gl.uniform1i(l, unit);
            gl.activeTexture(TEXTURE0);
        }
    }

    private void applyEnvironment(ProgramEntry p, Material material, Environment env, boolean shadowed) {
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
            WebGLUniformLocation l = loc(p, "u_sun");
            if (l != null) {
                gl.uniform4f(l, ((sun >> 16) & 0xff) / 255f, ((sun >> 8) & 0xff) / 255f, (sun & 0xff) / 255f,
                        env.getCloudCover());
            }
        }
        uniformColor(p, "u_emissive", material.getEmissive());
        if (shadowed) {
            WebGLUniformLocation l = loc(p, "u_shadowMatrix");
            if (l != null) {
                gl.uniformMatrix4fv(l, false, shadowMatrix);
            }
            Light light = getLight();
            uniform3(p, "u_shadow", light.getShadowBias(), light.getShadowStrength(),
                    1f / shadowTarget.width);
            sampler(p, "u_shadowMap", 1, shadowTarget.tex);
        }
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
        n.tex = gl.createTexture();
        gl.bindTexture(TEXTURE_2D, n.tex);
        gl.texImage2D(TEXTURE_2D, 0, RGBA, w, h, 0, RGBA, UNSIGNED_BYTE, null);
        gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, filter);
        gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, filter);
        gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE);
        gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE);
        gl.bindTexture(TEXTURE_2D, null);
        n.fbo = gl.createFramebuffer();
        gl.bindFramebuffer(FRAMEBUFFER, n.fbo);
        gl.framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_2D, n.tex, 0);
        if (depth) {
            n.depth = gl.createRenderbuffer();
            gl.bindRenderbuffer(RENDERBUFFER, n.depth);
            gl.renderbufferStorage(RENDERBUFFER, DEPTH_COMPONENT16, w, h);
            gl.framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT, RENDERBUFFER, n.depth);
            gl.bindRenderbuffer(RENDERBUFFER, null);
        }
        return n;
    }

    private void releaseTarget(Target t) {
        if (t == null) {
            return;
        }
        gl.deleteFramebuffer(t.fbo);
        gl.deleteTexture(t.tex);
        if (t.depth != null) {
            gl.deleteRenderbuffer(t.depth);
        }
    }

    private void saveTarget() {
        System.arraycopy(viewport, 0, savedViewport, 0, 4);
    }

    private void restoreTarget() {
        gl.bindFramebuffer(FRAMEBUFFER, null);
        setViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }

    private boolean shadowUnavailable;
    private boolean postUnavailable;

    private void passInto(Target t) {
        gl.bindFramebuffer(FRAMEBUFFER, t.fbo);
        setViewport(0, 0, t.width, t.height);
    }

    public boolean beginShadowPass(Light light) {
        if (shadowUnavailable) {
            return false;
        }
        if (shadowProgram == null) {
            try {
                shadowProgram = buildProgram(GlslShaderGenerator.shadowVertex(),
                        GlslShaderGenerator.shadowFragment());
            } catch (RuntimeException rejected) {
                // A browser that cannot link the depth shader draws without
                // shadows, as a device without the feature does, and is not
                // asked again every frame.
                shadowUnavailable = true;
                return false;
            }
        }
        saveTarget();
        int size = light.getShadowMapSize();
        // packed depth must not be interpolated between texels
        shadowTarget = target(shadowTarget, size, size, true, NEAREST);
        gl.bindFramebuffer(FRAMEBUFFER, shadowTarget.fbo);
        setViewport(0, 0, size, size);
        gl.clearColor(1f, 1f, 1f, 1f);
        gl.depthMask(true);
        gl.clear(COLOR_BUFFER_BIT | DEPTH_BUFFER_BIT);
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
        restoreTarget();
    }

    private void drawShadowCaster(Mesh mesh, Material material, float[] modelMatrix) {
        if (!material.isCastShadows() || material.getRenderState().getBlendMode() != RenderState.BlendMode.NONE
                || material.getType() == Material.Type.SKYBOX) {
            return;
        }
        VertexBuffer vb = mesh.getVertices();
        VertexFormat fmt = vb.getFormat();
        gl.useProgram(shadowProgram.program);
        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        float[] mvp = Matrix4.identity();
        Matrix4.multiply(shadowMatrix, model, mvp);
        if (shadowProgram.uMvp != null) {
            gl.uniformMatrix4fv(shadowProgram.uMvp, false, mvp);
        }
        gl.enable(DEPTH_TEST);
        gl.depthMask(true);
        gl.disable(BLEND);
        gl.disable(CULL_FACE);
        WebGLBuffer vbo = uploadVertexBuffer(vb);
        gl.bindBuffer(ARRAY_BUFFER, vbo);
        bindAttributes(shadowProgram, fmt);
        int mode = toGlPrimitive(mesh.getPrimitiveType());
        if (mesh.isIndexed()) {
            IndexBuffer ib = mesh.getIndices();
            WebGLBuffer ibo = uploadIndexBuffer(ib);
            gl.bindBuffer(ELEMENT_ARRAY_BUFFER, ibo);
            gl.drawElements(mode, ib.getIndexCount(), UNSIGNED_SHORT, 0);
        } else {
            gl.drawArrays(mode, 0, vb.getVertexCount());
        }
        if (shadowProgram.aPosition >= 0) {
            gl.disableVertexAttribArray(shadowProgram.aPosition);
        }
    }

    public boolean beginPostProcess(PostProcess effects, int width, int height) {
        if (effects == null) {
            return false;
        }
        if (postUnavailable) {
            return false;
        }
        if (compositeProgram == null) {
            String v = GlslShaderGenerator.postVertex();
            try {
                brightProgram = buildProgram(v, GlslShaderGenerator.brightFragment());
                blurProgram = buildProgram(v, GlslShaderGenerator.blurFragment());
                compositeProgram = buildProgram(v, GlslShaderGenerator.compositeFragment());
            } catch (RuntimeException rejected) {
                // Drawn straight to the screen instead, as without the feature.
                brightProgram = null;
                blurProgram = null;
                compositeProgram = null;
                postUnavailable = true;
                return false;
            }
        }
        saveTarget();
        int w = savedViewport[2];
        int h = savedViewport[3];
        if (w <= 0 || h <= 0) {
            return false;
        }
        sceneTarget = target(sceneTarget, w, h, true, LINEAR);
        if (effects.isBloom()) {
            bloomA = target(bloomA, Math.max(1, w / 4), Math.max(1, h / 4), false, LINEAR);
            bloomB = target(bloomB, bloomA.width, bloomA.height, false, LINEAR);
        }
        gl.bindFramebuffer(FRAMEBUFFER, sceneTarget.fbo);
        setViewport(0, 0, w, h);
        activePost = effects;
        return true;
    }

    public void endPostProcess(float timeSeconds) {
        PostProcess pp = activePost;
        if (pp == null) {
            return;
        }
        activePost = null;
        gl.disable(DEPTH_TEST);
        gl.depthMask(false);
        gl.disable(BLEND);
        gl.disable(CULL_FACE);
        WebGLTexture bloomTex = sceneTarget.tex;
        if (pp.isBloom()) {
            passInto(bloomA);
            gl.useProgram(brightProgram.program);
            sampler(brightProgram, "u_scene", 0, sceneTarget.tex);
            uniform1(brightProgram, "u_threshold", pp.getBloomThreshold());
            uniform2(brightProgram, "u_texel", 1f / sceneTarget.width, 1f / sceneTarget.height);
            drawQuad(brightProgram);
            for (int i = 0; i < 2; i++) {
                float spread = 1f + i;
                passInto(bloomB);
                gl.useProgram(blurProgram.program);
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
        ProgramEntry c = compositeProgram;
        gl.useProgram(c.program);
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
        gl.depthMask(true);
    }

    private void drawQuad(ProgramEntry p) {
        if (quadVbo == null) {
            quadVbo = gl.createBuffer();
            gl.bindBuffer(ARRAY_BUFFER, quadVbo);
            gl.bufferData(ARRAY_BUFFER, new float[]{-1f, -1f, 0f, 1f, -1f, 0f, -1f, 1f, 0f, 1f, 1f, 0f},
                    STATIC_DRAW);
        }
        gl.bindBuffer(ARRAY_BUFFER, quadVbo);
        gl.enableVertexAttribArray(p.aPosition);
        gl.vertexAttribPointer(p.aPosition, 3, FLOAT, false, 12, 0);
        gl.drawArrays(TRIANGLE_STRIP, 0, 4);
        gl.disableVertexAttribArray(p.aPosition);
    }

    private static String formatKey(VertexFormat format) {
        StringBuilder sb = new StringBuilder();
        int count = format.getAttributeCount();
        for (int i = 0; i < count; i++) {
            VertexAttribute attr = format.getAttribute(i);
            sb.append(attr.getUsage().name()).append(attr.getComponents());
        }
        return sb.toString();
    }

    private WebGLShader compileShader(int type, String source) {
        WebGLShader shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, COMPILE_STATUS)) {
            String log = gl.getShaderInfoLog(shader);
            throw new RuntimeException("WebGL shader compile failed: " + log + "\nsource:\n" + source);
        }
        return shader;
    }

    public void dispose(VertexBuffer buffer) {
        if (buffer.getHandle() instanceof WebGLBuffer) {
            gl.deleteBuffer((WebGLBuffer) buffer.getHandle());
            buffer.setHandle(null);
        }
    }

    public void dispose(IndexBuffer buffer) {
        if (buffer.getHandle() instanceof WebGLBuffer) {
            gl.deleteBuffer((WebGLBuffer) buffer.getHandle());
            buffer.setHandle(null);
        }
    }

    public void dispose(Texture texture) {
        if (texture.getHandle() instanceof WebGLTexture) {
            gl.deleteTexture((WebGLTexture) texture.getHandle());
            texture.setHandle(null);
        }
    }
}
