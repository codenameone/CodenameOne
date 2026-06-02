/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GlslShaderGenerator;
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
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.ui.Image;

import java.util.HashMap;
import java.util.Map;

/// WebGL backed `GraphicsDevice` for the browser port. The device wraps a WebGL
/// rendering context obtained from an `HTMLCanvasElement` and uses the shared
/// core `GlslShaderGenerator` to emit GLSL ES 1.00 (WebGL 1 is OpenGL ES 2, so
/// the generated source runs unmodified). Programs are cached per material
/// shader key and vertex format; vertex and index buffers are uploaded lazily
/// when dirty and textures are uploaded from ARGB pixel data.
///
/// All WebGL interop goes through narrow `@JSBody` helpers that operate on opaque
/// `JSObject` handles for the context, buffers, programs, uniform/attribute
/// locations and textures. This keeps the backend self contained without
/// introducing a WebGL specific dependency on top of the port's existing JSO
/// interop style.
class HTML5GraphicsDevice extends GraphicsDevice {
    private final JSObject gl;
    private final Map<String, ProgramEntry> programs = new HashMap<String, ProgramEntry>();
    private final GpuCapabilities capabilities;

    /// Cached compiled program together with the locations the device binds.
    private static final class ProgramEntry {
        JSObject program;
        JSObject aPosition;
        JSObject aNormal;
        JSObject aTexcoord;
        JSObject uMvp;
        JSObject uModel;
        JSObject uNormalMatrix;
        JSObject uColor;
        JSObject uTexture;
        JSObject uLightDir;
        JSObject uLightColor;
        JSObject uAmbient;
        JSObject uEye;
        JSObject uShininess;
    }

    HTML5GraphicsDevice(JSObject gl) {
        this.gl = gl;
        int maxTex = glGetParameterInt(gl, glMaxTextureSize(gl));
        int maxAttribs = glGetParameterInt(gl, glMaxVertexAttribs(gl));
        if (maxTex <= 0) {
            maxTex = 2048;
        }
        if (maxAttribs <= 0) {
            maxAttribs = 8;
        }
        this.capabilities = new GpuCapabilities(maxTex, maxAttribs, false, false, false, "WebGL");
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
        JSObject handle = glCreateTexture(gl);
        glBindTexture2D(gl, handle);
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            int c = argb[i];
            int o = i * 4;
            rgba[o] = (byte) ((c >> 16) & 0xff);
            rgba[o + 1] = (byte) ((c >> 8) & 0xff);
            rgba[o + 2] = (byte) (c & 0xff);
            rgba[o + 3] = (byte) ((c >>> 24) & 0xff);
        }
        glTexImage2DRGBA(gl, width, height, rgba);
        boolean linear = texture.getFilter() == Texture.Filter.LINEAR;
        boolean repeat = texture.getWrap() == Texture.Wrap.REPEAT;
        glTexParameters(gl, linear, repeat);
        glBindTexture2D(gl, null);
        texture.setHandle(handle);
        return texture;
    }

    public void clear(int argbColor, boolean color, boolean depth) {
        float a = ((argbColor >>> 24) & 0xff) / 255f;
        float r = ((argbColor >> 16) & 0xff) / 255f;
        float g = ((argbColor >> 8) & 0xff) / 255f;
        float b = (argbColor & 0xff) / 255f;
        glClearColor(gl, r, g, b, a);
        if (depth) {
            glDepthMask(gl, true);
        }
        glClear(gl, color, depth);
    }

    public void setViewport(int x, int y, int width, int height) {
        glViewport(gl, x, y, width, height);
    }

    public void draw(Mesh mesh, Material material, float[] modelMatrix) {
        VertexBuffer vertices = mesh.getVertices();
        VertexFormat format = vertices.getFormat();
        ProgramEntry entry = getProgram(material, format);
        glUseProgram(gl, entry.program);

        float[] model = modelMatrix != null ? modelMatrix : Matrix4.identity();
        Camera camera = getCamera();
        float[] mvp = Matrix4.identity();
        if (camera != null) {
            Matrix4.multiply(camera.getViewProjection(), model, mvp);
        } else {
            Matrix4.copy(model, mvp);
        }
        if (entry.uMvp != null) {
            glUniformMatrix4(gl, entry.uMvp, mvp);
        }
        if (entry.uModel != null) {
            glUniformMatrix4(gl, entry.uModel, model);
        }
        if (entry.uNormalMatrix != null) {
            glUniformMatrix4(gl, entry.uNormalMatrix, Matrix4.normalMatrix(model));
        }
        if (entry.uColor != null) {
            int c = material.getColor();
            glUniform4f(gl, entry.uColor,
                    ((c >> 16) & 0xff) / 255f,
                    ((c >> 8) & 0xff) / 255f,
                    (c & 0xff) / 255f,
                    ((c >>> 24) & 0xff) / 255f);
        }
        Light light = getLight();
        if (light != null) {
            if (entry.uLightDir != null) {
                glUniform3f(gl, entry.uLightDir,
                        light.getDirectionX(), light.getDirectionY(), light.getDirectionZ());
            }
            if (entry.uLightColor != null) {
                int lc = light.getColor();
                glUniform3f(gl, entry.uLightColor,
                        ((lc >> 16) & 0xff) / 255f, ((lc >> 8) & 0xff) / 255f, (lc & 0xff) / 255f);
            }
            if (entry.uAmbient != null) {
                int ac = light.getAmbientColor();
                glUniform3f(gl, entry.uAmbient,
                        ((ac >> 16) & 0xff) / 255f, ((ac >> 8) & 0xff) / 255f, (ac & 0xff) / 255f);
            }
        }
        if (entry.uEye != null && camera != null) {
            glUniform3f(gl, entry.uEye, camera.getEyeX(), camera.getEyeY(), camera.getEyeZ());
        }
        if (entry.uShininess != null) {
            glUniform1f(gl, entry.uShininess, material.getShininess());
        }

        Texture texture = material.getTexture();
        if (texture != null && entry.uTexture != null && texture.getHandle() instanceof JSObject) {
            glActiveTexture0(gl);
            glBindTexture2D(gl, (JSObject) texture.getHandle());
            glUniform1i(gl, entry.uTexture, 0);
        }

        applyRenderState(material.getRenderState());

        JSObject vbo = uploadVertexBuffer(vertices);
        glBindArrayBuffer(gl, vbo);
        bindAttributes(entry, format);

        PrimitiveType pt = mesh.getPrimitiveType();
        int mode = toGlPrimitive(pt);
        if (mesh.isIndexed()) {
            IndexBuffer indices = mesh.getIndices();
            JSObject ibo = uploadIndexBuffer(indices);
            glBindElementArrayBuffer(gl, ibo);
            glDrawElements(gl, mode, indices.getIndexCount());
        } else {
            glDrawArrays(gl, mode, vertices.getVertexCount());
        }
    }

    private void bindAttributes(ProgramEntry entry, VertexFormat format) {
        int strideBytes = format.getStrideBytes();
        int count = format.getAttributeCount();
        for (int i = 0; i < count; i++) {
            VertexAttribute attr = format.getAttribute(i);
            int offsetBytes = format.getAttributeOffset(i) * 4;
            JSObject loc = null;
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
                    loc = null;
                    break;
            }
            if (loc != null) {
                glEnableVertexAttrib(gl, loc);
                glVertexAttribPointer(gl, loc, attr.getComponents(), strideBytes, offsetBytes);
            }
        }
    }

    private JSObject uploadVertexBuffer(VertexBuffer buffer) {
        JSObject handle = buffer.getHandle() instanceof JSObject ? (JSObject) buffer.getHandle() : null;
        if (handle == null) {
            handle = glCreateBuffer(gl);
            buffer.setHandle(handle);
            buffer.setDirty();
        }
        if (buffer.isDirty()) {
            glBindArrayBuffer(gl, handle);
            glBufferDataFloat(gl, buffer.getData(), buffer.getFloatCount());
            buffer.clearDirty();
        }
        return handle;
    }

    private JSObject uploadIndexBuffer(IndexBuffer buffer) {
        JSObject handle = buffer.getHandle() instanceof JSObject ? (JSObject) buffer.getHandle() : null;
        if (handle == null) {
            handle = glCreateBuffer(gl);
            buffer.setHandle(handle);
            buffer.setDirty();
        }
        if (buffer.isDirty()) {
            glBindElementArrayBuffer(gl, handle);
            glBufferDataShort(gl, buffer.getData(), buffer.getIndexCount());
            buffer.clearDirty();
        }
        return handle;
    }

    private void applyRenderState(RenderState state) {
        if (state.isDepthTest()) {
            glEnableDepthTest(gl, true);
        } else {
            glEnableDepthTest(gl, false);
        }
        glDepthMask(gl, state.isDepthWrite());

        RenderState.BlendMode blend = state.getBlendMode();
        if (blend == RenderState.BlendMode.NONE) {
            glEnableBlend(gl, false);
        } else {
            glEnableBlend(gl, true);
            if (blend == RenderState.BlendMode.ADDITIVE) {
                glBlendFuncAdditive(gl);
            } else {
                glBlendFuncAlpha(gl);
            }
        }

        RenderState.CullMode cull = state.getCullMode();
        if (cull == RenderState.CullMode.NONE) {
            glEnableCull(gl, false);
        } else {
            glEnableCull(gl, true);
            glCullFace(gl, cull == RenderState.CullMode.FRONT);
        }
    }

    private int toGlPrimitive(PrimitiveType pt) {
        switch (pt) {
            case POINTS:
                return glConstPoints(gl);
            case LINES:
                return glConstLines(gl);
            case LINE_STRIP:
                return glConstLineStrip(gl);
            case TRIANGLE_STRIP:
                return glConstTriangleStrip(gl);
            case TRIANGLES:
            default:
                return glConstTriangles(gl);
        }
    }

    private ProgramEntry getProgram(Material material, VertexFormat format) {
        String key = material.getShaderKey() + "|" + formatKey(format);
        ProgramEntry entry = programs.get(key);
        if (entry != null) {
            return entry;
        }
        GlslShaderGenerator gen = new GlslShaderGenerator(material, format);
        JSObject vs = compileShader(gl, glConstVertexShader(gl), gen.getVertexSource());
        JSObject fs = compileShader(gl, glConstFragmentShader(gl), gen.getFragmentSource());
        JSObject program = glCreateProgram(gl);
        glAttachShader(gl, program, vs);
        glAttachShader(gl, program, fs);
        glLinkProgram(gl, program);
        if (!glProgramLinked(gl, program)) {
            String log = glProgramInfoLog(gl, program);
            throw new RuntimeException("WebGL program link failed: " + log);
        }
        entry = new ProgramEntry();
        entry.program = program;
        entry.aPosition = nonNeg(glGetAttribLocation(gl, program, GlslShaderGenerator.A_POSITION));
        entry.aNormal = nonNeg(glGetAttribLocation(gl, program, GlslShaderGenerator.A_NORMAL));
        entry.aTexcoord = nonNeg(glGetAttribLocation(gl, program, GlslShaderGenerator.A_TEXCOORD));
        entry.uMvp = glGetUniformLocation(gl, program, "u_mvp");
        entry.uModel = glGetUniformLocation(gl, program, "u_model");
        entry.uNormalMatrix = glGetUniformLocation(gl, program, "u_normalMatrix");
        entry.uColor = glGetUniformLocation(gl, program, "u_color");
        entry.uTexture = glGetUniformLocation(gl, program, "u_texture");
        entry.uLightDir = glGetUniformLocation(gl, program, "u_lightDir");
        entry.uLightColor = glGetUniformLocation(gl, program, "u_lightColor");
        entry.uAmbient = glGetUniformLocation(gl, program, "u_ambient");
        entry.uEye = glGetUniformLocation(gl, program, "u_eye");
        entry.uShininess = glGetUniformLocation(gl, program, "u_shininess");
        programs.put(key, entry);
        return entry;
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

    private static JSObject nonNeg(JSObject loc) {
        return loc;
    }

    private static JSObject compileShader(JSObject gl, int type, String source) {
        JSObject shader = glCreateShader(gl, type);
        glShaderSource(gl, shader, source);
        glCompileShader(gl, shader);
        if (!glShaderCompiled(gl, shader)) {
            String log = glShaderInfoLog(gl, shader);
            throw new RuntimeException("WebGL shader compile failed: " + log + "\nsource:\n" + source);
        }
        return shader;
    }

    public void dispose(VertexBuffer buffer) {
        if (buffer.getHandle() instanceof JSObject) {
            glDeleteBuffer(gl, (JSObject) buffer.getHandle());
            buffer.setHandle(null);
        }
    }

    public void dispose(IndexBuffer buffer) {
        if (buffer.getHandle() instanceof JSObject) {
            glDeleteBuffer(gl, (JSObject) buffer.getHandle());
            buffer.setHandle(null);
        }
    }

    public void dispose(Texture texture) {
        if (texture.getHandle() instanceof JSObject) {
            glDeleteTexture(gl, (JSObject) texture.getHandle());
            texture.setHandle(null);
        }
    }

    // ---------------------------------------------------------------------
    // WebGL interop. All methods operate on the opaque WebGLRenderingContext
    // and the objects it produces; the constants are read from the context so
    // no numeric duplication is required.
    // ---------------------------------------------------------------------

    @JSBody(params = {"gl", "name"}, script = "return gl.getParameter(name);")
    private static native int glGetParameterInt(JSObject gl, int name);

    @JSBody(params = {"gl"}, script = "return gl.MAX_TEXTURE_SIZE;")
    private static native int glMaxTextureSize(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.MAX_VERTEX_ATTRIBS;")
    private static native int glMaxVertexAttribs(JSObject gl);

    @JSBody(params = {"gl", "r", "g", "b", "a"}, script = "gl.clearColor(r, g, b, a);")
    private static native void glClearColor(JSObject gl, float r, float g, float b, float a);

    @JSBody(params = {"gl", "color", "depth"},
            script = "var m = 0; if (color) { m |= gl.COLOR_BUFFER_BIT; } if (depth) { m |= gl.DEPTH_BUFFER_BIT; } gl.clear(m);")
    private static native void glClear(JSObject gl, boolean color, boolean depth);

    @JSBody(params = {"gl", "x", "y", "w", "h"}, script = "gl.viewport(x, y, w, h);")
    private static native void glViewport(JSObject gl, int x, int y, int w, int h);

    @JSBody(params = {"gl", "enable"},
            script = "if (enable) { gl.enable(gl.DEPTH_TEST); } else { gl.disable(gl.DEPTH_TEST); }")
    private static native void glEnableDepthTest(JSObject gl, boolean enable);

    @JSBody(params = {"gl", "write"}, script = "gl.depthMask(write);")
    private static native void glDepthMask(JSObject gl, boolean write);

    @JSBody(params = {"gl", "enable"},
            script = "if (enable) { gl.enable(gl.BLEND); } else { gl.disable(gl.BLEND); }")
    private static native void glEnableBlend(JSObject gl, boolean enable);

    @JSBody(params = {"gl"}, script = "gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);")
    private static native void glBlendFuncAlpha(JSObject gl);

    @JSBody(params = {"gl"}, script = "gl.blendFunc(gl.SRC_ALPHA, gl.ONE);")
    private static native void glBlendFuncAdditive(JSObject gl);

    @JSBody(params = {"gl", "enable"},
            script = "if (enable) { gl.enable(gl.CULL_FACE); } else { gl.disable(gl.CULL_FACE); }")
    private static native void glEnableCull(JSObject gl, boolean enable);

    @JSBody(params = {"gl", "front"}, script = "gl.cullFace(front ? gl.FRONT : gl.BACK);")
    private static native void glCullFace(JSObject gl, boolean front);

    @JSBody(params = {"gl", "type"}, script = "return gl.createShader(type);")
    private static native JSObject glCreateShader(JSObject gl, int type);

    @JSBody(params = {"gl", "shader", "source"}, script = "gl.shaderSource(shader, source);")
    private static native void glShaderSource(JSObject gl, JSObject shader, String source);

    @JSBody(params = {"gl", "shader"}, script = "gl.compileShader(shader);")
    private static native void glCompileShader(JSObject gl, JSObject shader);

    @JSBody(params = {"gl", "shader"}, script = "return !!gl.getShaderParameter(shader, gl.COMPILE_STATUS);")
    private static native boolean glShaderCompiled(JSObject gl, JSObject shader);

    @JSBody(params = {"gl", "shader"}, script = "return gl.getShaderInfoLog(shader);")
    private static native String glShaderInfoLog(JSObject gl, JSObject shader);

    @JSBody(params = {"gl"}, script = "return gl.createProgram();")
    private static native JSObject glCreateProgram(JSObject gl);

    @JSBody(params = {"gl", "program", "shader"}, script = "gl.attachShader(program, shader);")
    private static native void glAttachShader(JSObject gl, JSObject program, JSObject shader);

    @JSBody(params = {"gl", "program"}, script = "gl.linkProgram(program);")
    private static native void glLinkProgram(JSObject gl, JSObject program);

    @JSBody(params = {"gl", "program"}, script = "return !!gl.getProgramParameter(program, gl.LINK_STATUS);")
    private static native boolean glProgramLinked(JSObject gl, JSObject program);

    @JSBody(params = {"gl", "program"}, script = "return gl.getProgramInfoLog(program);")
    private static native String glProgramInfoLog(JSObject gl, JSObject program);

    @JSBody(params = {"gl", "program"}, script = "gl.useProgram(program);")
    private static native void glUseProgram(JSObject gl, JSObject program);

    @JSBody(params = {"gl", "program", "name"},
            script = "var l = gl.getAttribLocation(program, name); return l < 0 ? null : {loc: l};")
    private static native JSObject glGetAttribLocation(JSObject gl, JSObject program, String name);

    @JSBody(params = {"gl", "program", "name"}, script = "return gl.getUniformLocation(program, name);")
    private static native JSObject glGetUniformLocation(JSObject gl, JSObject program, String name);

    @JSBody(params = {"gl"}, script = "return gl.createBuffer();")
    private static native JSObject glCreateBuffer(JSObject gl);

    @JSBody(params = {"gl", "buffer"}, script = "gl.bindBuffer(gl.ARRAY_BUFFER, buffer);")
    private static native void glBindArrayBuffer(JSObject gl, JSObject buffer);

    @JSBody(params = {"gl", "buffer"}, script = "gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, buffer);")
    private static native void glBindElementArrayBuffer(JSObject gl, JSObject buffer);

    @JSBody(params = {"gl", "data", "count"},
            script = "var a = new Float32Array(count); for (var i = 0; i < count; i++) { a[i] = data[i]; }"
                    + " gl.bufferData(gl.ARRAY_BUFFER, a, gl.STATIC_DRAW);")
    private static native void glBufferDataFloat(JSObject gl, float[] data, int count);

    @JSBody(params = {"gl", "data", "count"},
            script = "var a = new Uint16Array(count); for (var i = 0; i < count; i++) { a[i] = data[i] & 0xffff; }"
                    + " gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, a, gl.STATIC_DRAW);")
    private static native void glBufferDataShort(JSObject gl, short[] data, int count);

    @JSBody(params = {"gl", "loc"}, script = "gl.enableVertexAttribArray(loc.loc);")
    private static native void glEnableVertexAttrib(JSObject gl, JSObject loc);

    @JSBody(params = {"gl", "loc", "size", "stride", "offset"},
            script = "gl.vertexAttribPointer(loc.loc, size, gl.FLOAT, false, stride, offset);")
    private static native void glVertexAttribPointer(JSObject gl, JSObject loc, int size, int stride, int offset);

    @JSBody(params = {"gl", "loc", "data"},
            script = "var a = new Float32Array(16); for (var i = 0; i < 16; i++) { a[i] = data[i]; }"
                    + " gl.uniformMatrix4fv(loc, false, a);")
    private static native void glUniformMatrix4(JSObject gl, JSObject loc, float[] data);

    @JSBody(params = {"gl", "loc", "x", "y", "z", "w"}, script = "gl.uniform4f(loc, x, y, z, w);")
    private static native void glUniform4f(JSObject gl, JSObject loc, float x, float y, float z, float w);

    @JSBody(params = {"gl", "loc", "x", "y", "z"}, script = "gl.uniform3f(loc, x, y, z);")
    private static native void glUniform3f(JSObject gl, JSObject loc, float x, float y, float z);

    @JSBody(params = {"gl", "loc", "x"}, script = "gl.uniform1f(loc, x);")
    private static native void glUniform1f(JSObject gl, JSObject loc, float x);

    @JSBody(params = {"gl", "loc", "x"}, script = "gl.uniform1i(loc, x);")
    private static native void glUniform1i(JSObject gl, JSObject loc, int x);

    @JSBody(params = {"gl", "mode", "count"}, script = "gl.drawArrays(mode, 0, count);")
    private static native void glDrawArrays(JSObject gl, int mode, int count);

    @JSBody(params = {"gl", "mode", "count"}, script = "gl.drawElements(mode, count, gl.UNSIGNED_SHORT, 0);")
    private static native void glDrawElements(JSObject gl, int mode, int count);

    @JSBody(params = {"gl"}, script = "return gl.createTexture();")
    private static native JSObject glCreateTexture(JSObject gl);

    @JSBody(params = {"gl", "texture"}, script = "gl.bindTexture(gl.TEXTURE_2D, texture);")
    private static native void glBindTexture2D(JSObject gl, JSObject texture);

    @JSBody(params = {"gl"}, script = "gl.activeTexture(gl.TEXTURE0);")
    private static native void glActiveTexture0(JSObject gl);

    @JSBody(params = {"gl", "width", "height", "pixels"},
            script = "var n = width * height * 4; var a = new Uint8Array(n); for (var i = 0; i < n; i++) { a[i] = pixels[i] & 0xff; }"
                    + " gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, a);")
    private static native void glTexImage2DRGBA(JSObject gl, int width, int height, byte[] pixels);

    @JSBody(params = {"gl", "linear", "repeat"},
            script = "var f = linear ? gl.LINEAR : gl.NEAREST;"
                    + " gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, f);"
                    + " gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, f);"
                    + " var w = repeat ? gl.REPEAT : gl.CLAMP_TO_EDGE;"
                    + " gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, w);"
                    + " gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, w);")
    private static native void glTexParameters(JSObject gl, boolean linear, boolean repeat);

    @JSBody(params = {"gl", "buffer"}, script = "gl.deleteBuffer(buffer);")
    private static native void glDeleteBuffer(JSObject gl, JSObject buffer);

    @JSBody(params = {"gl", "texture"}, script = "gl.deleteTexture(texture);")
    private static native void glDeleteTexture(JSObject gl, JSObject texture);

    @JSBody(params = {"gl"}, script = "return gl.VERTEX_SHADER;")
    private static native int glConstVertexShader(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.FRAGMENT_SHADER;")
    private static native int glConstFragmentShader(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.POINTS;")
    private static native int glConstPoints(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.LINES;")
    private static native int glConstLines(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.LINE_STRIP;")
    private static native int glConstLineStrip(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.TRIANGLES;")
    private static native int glConstTriangles(JSObject gl);

    @JSBody(params = {"gl"}, script = "return gl.TRIANGLE_STRIP;")
    private static native int glConstTriangleStrip(JSObject gl);
}
