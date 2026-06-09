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
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.codename1.gpu.RenderView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
///
/// A `SurfaceView`/`GLSurfaceView` renders to its own surface, which the normal
/// view drawing path (and therefore the Codename One screenshot path) cannot
/// read back. To make 3D scenes appear in screenshots, every drawn frame is read
/// back with `glReadPixels` into a `Bitmap`; `AndroidScreenshotTask` composites
/// the most recent frame of each live peer onto the captured screenshot.
class AndroidGLSurface extends GLSurfaceView {
    /// Live GL peers, used by `AndroidScreenshotTask` to composite their last
    /// rendered frame into screenshots.
    static final List<AndroidGLSurface> ACTIVE =
            Collections.synchronizedList(new ArrayList<AndroidGLSurface>());

    private final RenderView view;
    private final com.codename1.gpu.Renderer renderer;
    private AndroidGraphicsDevice device;
    private int lastW = -1;
    private int lastH = -1;
    private volatile Bitmap lastFrame;
    private ByteBuffer readbackBuffer;

    AndroidGLSurface(Context context, RenderView view) {
        super(context);
        this.view = view;
        this.renderer = view.getRenderer();
        setEGLContextClientVersion(2);
        // Composite above the Codename One surface so the GL content is visible
        // in the window (and captured by PixelCopy) rather than punched behind it.
        setZOrderMediaOverlay(true);
        setRenderer(new SurfaceRenderer());
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    /// Returns the most recently rendered frame read back from the GPU, or null
    /// if no frame has been drawn yet. Intended for the screenshot path.
    Bitmap getLastFrame() {
        return lastFrame;
    }

    /// True only when this peer's RenderView belongs to the form currently on
    /// screen. The native View can stay attached/shown for a beat while a form is
    /// torn down, so the screenshot composite uses this instead of isShown() to
    /// avoid bleeding a previous test's 3D frame into a later capture.
    boolean isOnCurrentForm() {
        return view != null
                && view.getComponentForm() == com.codename1.ui.Display.getInstance().getCurrent();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ACTIVE.add(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        ACTIVE.remove(this);
        super.onDetachedFromWindow();
    }

    private void readbackFrame() {
        int w = lastW;
        int h = lastH;
        if (w <= 0 || h <= 0) {
            return;
        }
        int pixels = w * h;
        if (readbackBuffer == null || readbackBuffer.capacity() < pixels * 4) {
            readbackBuffer = ByteBuffer.allocateDirect(pixels * 4).order(ByteOrder.nativeOrder());
        }
        readbackBuffer.position(0);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, readbackBuffer);
        int[] argb = new int[pixels];
        byte[] raw = new byte[pixels * 4];
        readbackBuffer.position(0);
        readbackBuffer.get(raw);
        // glReadPixels returns RGBA bottom-up; convert to ARGB top-down.
        for (int y = 0; y < h; y++) {
            int srcRow = (h - 1 - y) * w * 4;
            int dstRow = y * w;
            for (int x = 0; x < w; x++) {
                int s = srcRow + x * 4;
                int r = raw[s] & 0xff;
                int g = raw[s + 1] & 0xff;
                int b = raw[s + 2] & 0xff;
                int a = raw[s + 3] & 0xff;
                argb[dstRow + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        lastFrame = Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888);
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
            try {
                readbackFrame();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    void disposeSurface() {
        ACTIVE.remove(this);
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
