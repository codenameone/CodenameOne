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


import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import java.awt.image.BufferedImage;

/// The fold renderer's GPU target: an offscreen OpenGL framebuffer driven
/// through `JavaSEGLDevice`, read back into an image after every frame.
///
/// A frame of the folding device is two textured panels, a few walls and a
/// spine; the software rasterizer spent a quarter of a second on one at
/// retina size, the GPU a few milliseconds (most of it the read back).
///
/// Like `JavaSEJoglSurface` it touches JOGL, so the port loads it
/// reflectively and keeps the software target when it is missing or GL is
/// unusable. Frames are rendered synchronously on the calling thread.
final class JavaSEJoglFoldTarget implements FoldRenderer.Target {
    private final JavaSEGLDevice device = new JavaSEGLDevice();
    private final GLOffscreenAutoDrawable drawable;
    private final AWTGLReadBufferUtil readback;
    private FoldRenderer.Painter pending;
    private BufferedImage frame;
    private RuntimeException failure;
    private int lastW = 1;
    private int lastH = 1;

    JavaSEJoglFoldTarget() {
        // GL2 for the same reason JavaSEJoglSurface uses it: a 2.1
        // compatibility context on macOS accepts the shared shaders.
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(24);
        caps.setAlphaBits(8);
        caps.setOnscreen(false);
        caps.setFBO(true);
        drawable = GLDrawableFactory.getFactory(profile).createOffscreenAutoDrawable(null, caps, null, 1, 1);
        drawable.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
            }

            public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
                device.setGL(d.getGL().getGL2ES2());
            }

            public void display(GLAutoDrawable d) {
                FoldRenderer.Painter p = pending;
                if (p == null) {
                    return;
                }
                device.setGL(d.getGL().getGL2ES2());
                try {
                    device.setViewport(0, 0, lastW, lastH);
                    p.paint(device);
                    frame = readback.readPixelsToBufferedImage(d.getGL(), true);
                } catch (RuntimeException e) {
                    failure = e;
                }
            }

            public void dispose(GLAutoDrawable d) {
                device.setGL(d.getGL().getGL2ES2());
                device.disposePrograms();
            }
        });
        readback = new AWTGLReadBufferUtil(profile, true);
        // Realize the context now, so an unusable GL fails here and the port
        // keeps the software target, never in the middle of a fold.
        drawable.display();
    }

    public synchronized BufferedImage render(int width, int height, FoldRenderer.Painter painter) {
        if (width != lastW || height != lastH) {
            drawable.setSurfaceSize(width, height);
            lastW = width;
            lastH = height;
        }
        pending = painter;
        failure = null;
        frame = null;
        try {
            drawable.display();
        } finally {
            pending = null;
        }
        if (failure != null) {
            throw failure;
        }
        if (frame == null) {
            throw new IllegalStateException("The GL fold frame was not rendered");
        }
        return frame;
    }

    public boolean isAccelerated() {
        return true;
    }
}
