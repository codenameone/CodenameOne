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
package com.codename1.html5.js.webgl;

import com.codename1.html5.js.JSObject;

/// JSO interface for the browser `WebGLRenderingContext`.
///
/// Every method here is dispatched through the Codename One JSO bridge against
/// the real context object, which lives on the browser MAIN THREAD (the
/// translated application runs in a Web Worker and has no direct DOM/WebGL
/// access). This is why the 3D backend must talk to WebGL through an interface
/// like this rather than through `@JSBody` (whose script body runs in the worker
/// where the context object is only an opaque bridge proxy without its methods).
///
/// Bulk numeric arguments (vertex/index/pixel/uniform data) are passed as plain
/// Java primitive arrays. The worker->main bridge serializes those as ordinary JS
/// number arrays (a worker-constructed typed array would arrive on main as an
/// empty object), and the bridge re-wraps them in the correct typed array
/// (Float32Array/Uint16Array/Uint8Array) just before the real GL call.
///
/// GL enum values are not exposed here as members (reading `gl.CONSTANT` would be
/// a bridge round-trip each time); callers pass the standard, spec-fixed numeric
/// values directly (see the constants in the 3D device).
public interface WebGLRenderingContext extends JSObject {
    int getParameter(int pname);
    int getError();

    void clearColor(float r, float g, float b, float a);
    void clear(int mask);
    void viewport(int x, int y, int width, int height);
    void enable(int cap);
    void disable(int cap);
    void depthMask(boolean flag);
    void blendFunc(int sfactor, int dfactor);
    void cullFace(int mode);

    WebGLShader createShader(int type);
    void shaderSource(WebGLShader shader, String source);
    void compileShader(WebGLShader shader);
    boolean getShaderParameter(WebGLShader shader, int pname);
    String getShaderInfoLog(WebGLShader shader);

    WebGLProgram createProgram();
    void attachShader(WebGLProgram program, WebGLShader shader);
    void linkProgram(WebGLProgram program);
    boolean getProgramParameter(WebGLProgram program, int pname);
    String getProgramInfoLog(WebGLProgram program);
    void useProgram(WebGLProgram program);

    int getAttribLocation(WebGLProgram program, String name);
    WebGLUniformLocation getUniformLocation(WebGLProgram program, String name);

    WebGLBuffer createBuffer();
    void bindBuffer(int target, WebGLBuffer buffer);
    // The payload arrives on the main thread as a plain JS number array (Java
    // primitive arrays survive the worker->main bridge that way) and the bridge
    // wraps it in the right typed array before calling the real gl.bufferData
    // (Float32Array for ARRAY_BUFFER, Uint16Array for ELEMENT_ARRAY_BUFFER).
    void bufferData(int target, float[] data, int usage);
    void bufferData(int target, short[] data, int usage);
    void deleteBuffer(WebGLBuffer buffer);

    void enableVertexAttribArray(int index);
    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset);

    void uniformMatrix4fv(WebGLUniformLocation location, boolean transpose, float[] value);
    void uniform4f(WebGLUniformLocation location, float x, float y, float z, float w);
    void uniform3f(WebGLUniformLocation location, float x, float y, float z);
    void uniform1f(WebGLUniformLocation location, float x);
    void uniform1i(WebGLUniformLocation location, int x);

    void drawArrays(int mode, int first, int count);
    void drawElements(int mode, int count, int type, int offset);

    WebGLTexture createTexture();
    void bindTexture(int target, WebGLTexture texture);
    void activeTexture(int texture);
    void texImage2D(int target, int level, int internalformat, int width, int height,
                    int border, int format, int type, byte[] pixels);
    void texParameteri(int target, int pname, int param);
    void deleteTexture(WebGLTexture texture);
}
