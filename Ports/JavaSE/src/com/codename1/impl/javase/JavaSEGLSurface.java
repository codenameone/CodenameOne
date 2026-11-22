/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/// AWT surface that hosts the JavaSE software 3D renderer. The component is
/// wrapped as a Codename One native peer; each time it is painted it drives one
/// frame of the application `Renderer` and blits the resulting image. In
/// continuous mode a Swing timer requests repaints to form an animation loop.
///
/// This is the fallback backend used only when the JOGL hardware renderer
/// (`JavaSEJoglSurface`) cannot be initialized; the software rasterizer keeps the
/// simulator working (for simple scenes) when no OpenGL is available.
class JavaSEGLSurface extends JComponent implements JavaSEGpuSurface {
    private final RenderView view;
    private final Renderer renderer;
    private final JavaSESoftwareDevice device = new JavaSESoftwareDevice();
    private boolean initialized;
    private int lastW = -1;
    private int lastH = -1;
    private Timer timer;

    JavaSEGLSurface(RenderView view) {
        this.view = view;
        this.renderer = view.getRenderer();
        setOpaque(false);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setContinuous(boolean continuous) {
        if (continuous) {
            if (timer == null) {
                timer = new Timer(16, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (HIDDEN) {
                            renderOffscreen();
                        } else {
                            view.repaint();
                        }
                    }
                });
            }
            timer.start();
        } else if (timer != null) {
            timer.stop();
        }
    }

    public void requestRender() {
        if (HIDDEN) {
            renderOffscreen();
        } else {
            view.repaint();
        }
    }

    /// A hidden simulator (`cn1.simulator.hidden`) never shows its window, and
    /// Swing does not paint a component that is not showing, so a repaint
    /// would never reach [#paintComponent] and the view would sit on its first
    /// frame. There the timer renders each frame itself, into the device's
    /// image, and painting -- which a screen capture still does -- only draws
    /// the last one, so a capture does not advance the view by a frame.
    static final boolean HIDDEN = Boolean.getBoolean("cn1.simulator.hidden");
    private boolean rendered;
    private boolean disposed;

    /// Run after each offscreen frame by the port, which copies the frame
    /// into the peer's buffer -- what a capture of the form draws.
    private Runnable afterOffscreenFrame;

    public void setAfterOffscreenFrame(Runnable afterFrame) {
        afterOffscreenFrame = afterFrame;
    }

    private void renderOffscreen() {
        if (disposed) {
            // A last tick or render request queued before the dispose.
            return;
        }
        renderFrame(getWidth(), getHeight());
        if (afterOffscreenFrame != null) {
            afterOffscreenFrame.run();
        }
    }

    public void disposeSurface() {
        if (HIDDEN && !java.awt.EventQueue.isDispatchThread()) {
            // Hidden, frames are rendered on the AWT thread by the timer, not
            // by painting; disposing from another thread could free the
            // renderer in the middle of one. Serialize with them instead.
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    disposeSurface();
                }
            });
            return;
        }
        disposed = true;
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        try {
            renderer.onDispose(device);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (!HIDDEN || !rendered) {
            renderFrame(w, h);
        }
        BufferedImage img = device.getImage();
        if (img != null) {
            g.drawImage(img, 0, 0, null);
        }
    }

    private void renderFrame(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        rendered = true;
        device.resize(w, h);
        if (!initialized) {
            try {
                renderer.onInit(device);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            initialized = true;
            lastW = -1;
        }
        if (w != lastW || h != lastH) {
            lastW = w;
            lastH = h;
            try {
                renderer.onResize(device, w, h);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        try {
            renderer.onFrame(device);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public BufferedImage lastFrame() {
        return device.getImage();
    }
}
