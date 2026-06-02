/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.android;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.codename1.gpu.RenderView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/// Android `GLSurfaceView` that hosts an OpenGL ES 2.0 `AndroidGraphicsDevice`
/// and drives an application supplied `Renderer`.
///
/// The view is wrapped as a Codename One native peer so it composites with the
/// rest of the UI. The renderer hooks run on the dedicated GL thread the
/// `GLSurfaceView` manages, which is exactly where the `AndroidGraphicsDevice`
/// requires its calls to happen, so the `Renderer` callbacks are forwarded
/// directly from `onSurfaceCreated` / `onSurfaceChanged` / `onDrawFrame`.
class AndroidGLSurface extends GLSurfaceView {
    private final RenderView view;
    private final com.codename1.gpu.Renderer renderer;
    private AndroidGraphicsDevice device;
    private int lastW = -1;
    private int lastH = -1;

    AndroidGLSurface(Context context, RenderView view) {
        super(context);
        this.view = view;
        this.renderer = view.getRenderer();
        setEGLContextClientVersion(2);
        setRenderer(new SurfaceRenderer());
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private final class SurfaceRenderer implements GLSurfaceView.Renderer {
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            device = new AndroidGraphicsDevice();
            lastW = -1;
            lastH = -1;
            try {
                renderer.onInit(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            if (device == null) {
                return;
            }
            lastW = width;
            lastH = height;
            try {
                renderer.onResize(device, width, height);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public void onDrawFrame(GL10 unused) {
            if (device == null) {
                return;
            }
            try {
                renderer.onFrame(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    void disposeSurface() {
        // Run on the GL thread so the dispose callback sees a current context.
        final AndroidGraphicsDevice d = device;
        queueEvent(new Runnable() {
            public void run() {
                try {
                    renderer.onDispose(d);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (d != null) {
                    d.disposePrograms();
                }
            }
        });
    }
}
