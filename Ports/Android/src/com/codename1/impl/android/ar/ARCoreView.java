/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android.ar;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.Display;
import android.view.WindowManager;

import com.google.ar.core.Camera;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The AR view: a GLSurfaceView that renders the ARCore camera image as the
 * background (via the external OES texture ARCore fills) and draws every
 * anchored mesh on top with a minimal lit/textured GLES2 shader. ARCore has
 * no built-in renderer, so this class owns the whole draw path and drives
 * {@link AndroidARImpl#processFrame} once per frame.
 */
class ARCoreView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private final AndroidARImpl impl;
    private int cameraTextureId = -1;
    private boolean displayGeometryDirty = true;
    private int viewportWidth = 1;
    private int viewportHeight = 1;

    // Camera background resources.
    private int backgroundProgram;
    private int backgroundPositionAttr;
    private int backgroundTexCoordAttr;
    private final FloatBuffer quadCoords;
    private final FloatBuffer quadTexCoords;

    // Mesh shader resources.
    private int meshProgram;
    private int meshPositionAttr;
    private int meshNormalAttr;
    private int meshUvAttr;
    private int meshMvpUniform;
    private int meshModelUniform;
    private int meshColorUniform;
    private int meshUseTextureUniform;
    private int meshTextureUniform;
    private int meshLightDirUniform;
    private int meshLightIntensityUniform;

    private volatile float lightIntensity = 1f;

    private static final String BACKGROUND_VERTEX =
            "attribute vec4 a_Position;\n"
            + "attribute vec2 a_TexCoord;\n"
            + "varying vec2 v_TexCoord;\n"
            + "void main() {\n"
            + "  gl_Position = a_Position;\n"
            + "  v_TexCoord = a_TexCoord;\n"
            + "}\n";

    private static final String BACKGROUND_FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_TexCoord;\n"
            + "uniform samplerExternalOES u_Texture;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(u_Texture, v_TexCoord);\n"
            + "}\n";

    private static final String MESH_VERTEX =
            "uniform mat4 u_Mvp;\n"
            + "uniform mat4 u_Model;\n"
            + "attribute vec4 a_Position;\n"
            + "attribute vec3 a_Normal;\n"
            + "attribute vec2 a_Uv;\n"
            + "varying vec3 v_Normal;\n"
            + "varying vec2 v_Uv;\n"
            + "void main() {\n"
            + "  gl_Position = u_Mvp * a_Position;\n"
            + "  v_Normal = normalize((u_Model * vec4(a_Normal, 0.0)).xyz);\n"
            + "  v_Uv = a_Uv;\n"
            + "}\n";

    private static final String MESH_FRAGMENT =
            "precision mediump float;\n"
            + "uniform vec4 u_Color;\n"
            + "uniform float u_UseTexture;\n"
            + "uniform sampler2D u_Texture;\n"
            + "uniform vec3 u_LightDir;\n"
            + "uniform float u_LightIntensity;\n"
            + "varying vec3 v_Normal;\n"
            + "varying vec2 v_Uv;\n"
            + "void main() {\n"
            + "  vec4 base = mix(u_Color, texture2D(u_Texture, v_Uv), u_UseTexture);\n"
            + "  float diffuse = max(dot(normalize(v_Normal), -u_LightDir), 0.0);\n"
            + "  float shade = clamp((0.4 + 0.6 * diffuse) * u_LightIntensity, 0.0, 1.5);\n"
            + "  gl_FragColor = vec4(base.rgb * shade, base.a);\n"
            + "}\n";

    ARCoreView(AndroidARImpl impl) {
        super(impl.getActivity());
        this.impl = impl;
        quadCoords = allocateFloats(new float[]{-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f});
        quadTexCoords = allocateFloats(new float[8]);
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setWillNotDraw(false);
    }

    private static FloatBuffer allocateFloats(float[] values) {
        FloatBuffer b = ByteBuffer.allocateDirect(values.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(values);
        b.rewind();
        return b;
    }

    void pauseView() {
        onPause();
    }

    void resumeView() {
        onResume();
    }

    /** Marks a mesh entry's GL objects for recreation after its removal. */
    void recycleEntry(AndroidARImpl.MeshEntry entry) {
        // GL objects leak until the context dies; acceptable for the
        // placement-scale content this renderer targets.
        entry.vbo = 0;
        entry.ibo = 0;
        entry.textureId = 0;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        cameraTextureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        backgroundProgram = buildProgram(BACKGROUND_VERTEX, BACKGROUND_FRAGMENT);
        backgroundPositionAttr = GLES20.glGetAttribLocation(backgroundProgram, "a_Position");
        backgroundTexCoordAttr = GLES20.glGetAttribLocation(backgroundProgram, "a_TexCoord");

        meshProgram = buildProgram(MESH_VERTEX, MESH_FRAGMENT);
        meshPositionAttr = GLES20.glGetAttribLocation(meshProgram, "a_Position");
        meshNormalAttr = GLES20.glGetAttribLocation(meshProgram, "a_Normal");
        meshUvAttr = GLES20.glGetAttribLocation(meshProgram, "a_Uv");
        meshMvpUniform = GLES20.glGetUniformLocation(meshProgram, "u_Mvp");
        meshModelUniform = GLES20.glGetUniformLocation(meshProgram, "u_Model");
        meshColorUniform = GLES20.glGetUniformLocation(meshProgram, "u_Color");
        meshUseTextureUniform = GLES20.glGetUniformLocation(meshProgram, "u_UseTexture");
        meshTextureUniform = GLES20.glGetUniformLocation(meshProgram, "u_Texture");
        meshLightDirUniform = GLES20.glGetUniformLocation(meshProgram, "u_LightDir");
        meshLightIntensityUniform = GLES20.glGetUniformLocation(meshProgram, "u_LightIntensity");

        Session session = impl.getSession();
        if (session != null) {
            session.setCameraTextureName(cameraTextureId);
            try {
                session.resume();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static int buildProgram(String vertexSrc, String fragmentSrc) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        return program;
    }

    private static int compileShader(int type, String src) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight);
        displayGeometryDirty = true;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Session session = impl.getSession();
        if (session == null || impl.isClosed()) {
            return;
        }
        try {
            if (displayGeometryDirty) {
                displayGeometryDirty = false;
                session.setDisplayGeometry(displayRotation(), viewportWidth, viewportHeight);
            }
            session.setCameraTextureName(cameraTextureId);
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            if (frame.hasDisplayGeometryChanged()) {
                frame.transformCoordinates2d(
                        Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, quadCoords,
                        Coordinates2d.TEXTURE_NORMALIZED, quadTexCoords);
            }
            drawCameraBackground();

            impl.processFrame(frame, camera);

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                drawMeshes(camera);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private int displayRotation() {
        try {
            WindowManager wm = impl.getActivity().getWindowManager();
            Display d = wm.getDefaultDisplay();
            return d.getRotation();
        } catch (Throwable t) {
            return 0;
        }
    }

    private void drawCameraBackground() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        GLES20.glUseProgram(backgroundProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        quadCoords.rewind();
        GLES20.glVertexAttribPointer(backgroundPositionAttr, 2, GLES20.GL_FLOAT,
                false, 0, quadCoords);
        quadTexCoords.rewind();
        GLES20.glVertexAttribPointer(backgroundTexCoordAttr, 2, GLES20.GL_FLOAT,
                false, 0, quadTexCoords);
        GLES20.glEnableVertexAttribArray(backgroundPositionAttr);
        GLES20.glEnableVertexAttribArray(backgroundTexCoordAttr);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(backgroundPositionAttr);
        GLES20.glDisableVertexAttribArray(backgroundTexCoordAttr);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawMeshes(Camera camera) {
        Map<String, float[]> anchorPoses = new HashMap<String, float[]>();
        List<AndroidARImpl.MeshEntry> entries = impl.meshSnapshot(anchorPoses);
        if (entries.isEmpty()) {
            return;
        }
        float[] viewM = new float[16];
        float[] projM = new float[16];
        camera.getViewMatrix(viewM, 0);
        camera.getProjectionMatrix(projM, 0, 0.05f, 100f);
        float[] vp = new float[16];
        Matrix.multiplyMM(vp, 0, projM, 0, viewM, 0);

        GLES20.glUseProgram(meshProgram);
        GLES20.glUniform3f(meshLightDirUniform, -0.35f, -0.85f, -0.35f);
        GLES20.glUniform1f(meshLightIntensityUniform, lightIntensity);

        float[] model = new float[16];
        float[] mvp = new float[16];
        for (AndroidARImpl.MeshEntry entry : entries) {
            float[] anchorPose = anchorPoses.get(entry.anchorId);
            if (anchorPose == null) {
                continue;
            }
            ensureUploaded(entry);
            Matrix.multiplyMM(model, 0, anchorPose, 0, entry.anchorLocal16, 0);
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0);
            GLES20.glUniformMatrix4fv(meshMvpUniform, 1, false, mvp, 0);
            GLES20.glUniformMatrix4fv(meshModelUniform, 1, false, model, 0);
            float a = ((entry.argbColor >> 24) & 0xff) / 255f;
            float r = ((entry.argbColor >> 16) & 0xff) / 255f;
            float g = ((entry.argbColor >> 8) & 0xff) / 255f;
            float b = (entry.argbColor & 0xff) / 255f;
            GLES20.glUniform4f(meshColorUniform, r, g, b, a);
            if (entry.textureId != 0) {
                GLES20.glUniform1f(meshUseTextureUniform, 1f);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, entry.textureId);
                GLES20.glUniform1i(meshTextureUniform, 0);
            } else {
                GLES20.glUniform1f(meshUseTextureUniform, 0f);
            }
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entry.vbo);
            int stride = 8 * 4;
            GLES20.glVertexAttribPointer(meshPositionAttr, 3, GLES20.GL_FLOAT, false, stride, 0);
            GLES20.glVertexAttribPointer(meshNormalAttr, 3, GLES20.GL_FLOAT, false, stride, 12);
            GLES20.glVertexAttribPointer(meshUvAttr, 2, GLES20.GL_FLOAT, false, stride, 24);
            GLES20.glEnableVertexAttribArray(meshPositionAttr);
            GLES20.glEnableVertexAttribArray(meshNormalAttr);
            GLES20.glEnableVertexAttribArray(meshUvAttr);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, entry.ibo);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, entry.indices.length,
                    GLES20.GL_UNSIGNED_INT, 0);
            GLES20.glDisableVertexAttribArray(meshPositionAttr);
            GLES20.glDisableVertexAttribArray(meshNormalAttr);
            GLES20.glDisableVertexAttribArray(meshUvAttr);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    /** Feeds the light estimate through so drawn content matches the scene. */
    void setLightIntensity(float intensity) {
        this.lightIntensity = intensity;
    }

    private void ensureUploaded(AndroidARImpl.MeshEntry entry) {
        if (entry.vbo == 0) {
            int[] ids = new int[2];
            GLES20.glGenBuffers(2, ids, 0);
            entry.vbo = ids[0];
            entry.ibo = ids[1];
            int floatCount = entry.vertexCount * 8;
            FloatBuffer vb = ByteBuffer.allocateDirect(floatCount * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            vb.put(entry.interleaved, 0, floatCount);
            vb.rewind();
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, entry.vbo);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, floatCount * 4, vb,
                    GLES20.GL_STATIC_DRAW);
            IntBuffer ib = ByteBuffer.allocateDirect(entry.indices.length * 4)
                    .order(ByteOrder.nativeOrder()).asIntBuffer();
            ib.put(entry.indices);
            ib.rewind();
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, entry.ibo);
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, entry.indices.length * 4, ib,
                    GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
            if (entry.texture != null && entry.textureId == 0) {
                int[] tex = new int[1];
                GLES20.glGenTextures(1, tex, 0);
                entry.textureId = tex[0];
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, entry.textureId);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, entry.texture, 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            }
        }
    }
}
