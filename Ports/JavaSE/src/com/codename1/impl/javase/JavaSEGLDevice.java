/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

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
    }

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
            caps = new GpuCapabilities(maxTex, maxAttribs, false, false, false, name);
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

        Program program = getProgram(material, fmt);
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
            if (blend == RenderState.BlendMode.ADDITIVE) {
                gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE);
            } else {
                gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
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

    private Program getProgram(Material material, VertexFormat fmt) {
        String key = material.getShaderKey() + "|" + System.identityHashCode(fmt);
        Program p = programs.get(key);
        if (p != null) {
            return p;
        }
        GlslShaderGenerator gen = new GlslShaderGenerator(material, fmt);
        int handle = linkProgram(gen.getVertexSource(), gen.getFragmentSource());
        p = new Program();
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
        programs.put(key, p);
        return p;
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
    }
}
